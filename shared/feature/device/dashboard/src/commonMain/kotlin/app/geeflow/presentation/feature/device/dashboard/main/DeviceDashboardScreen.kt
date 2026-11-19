package app.geeflow.presentation.feature.device.dashboard.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.navigation.destination.DeviceDashboard
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.domain.device.recipe.LiveRecipeCompiler
import app.geeflow.data.device.BookooScaleConnection
import app.geeflow.domain.device.LiveBrewTestCoordinator
import app.geeflow.domain.device.LiveTestMode
import app.geeflow.domain.device.recipe.LiveRecipeDocument
import app.geeflow.domain.device.recipe.LiveRecipeJson
import app.geeflow.presentation.feature.device.dashboard.livecontrol.rememberLiveBrewServiceStarter
import app.geeflow.presentation.feature.device.dashboard.main.ShotRecipeSettings
import app.geeflow.platform.ForegroundEffect
import app.geeflow.presentation.feature.device.dashboard.components.BrewCharts
import app.geeflow.presentation.feature.device.dashboard.components.BrewProfileSummary
import app.geeflow.presentation.feature.device.dashboard.components.BrewStatistics
import app.geeflow.presentation.feature.device.dashboard.components.DashboardBrewButton
import app.geeflow.presentation.feature.device.dashboard.components.ExtractionReadout
import app.geeflow.presentation.feature.device.dashboard.components.FlushOverlay
import app.geeflow.presentation.feature.device.dashboard.components.PhoneBrewControls
import app.geeflow.presentation.feature.device.dashboard.components.TopBar
import app.geeflow.presentation.feature.device.dashboard.components.dashboardStatusText
import app.geeflow.presentation.feature.device.dashboard.maintenance.MaintenanceReminderHost
import app.geeflow.presentation.feature.device.dashboard.model.chartModel
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileList
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModel
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModelEvent.SelectProfile
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModelEvent.ShowHistoryBrew
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModelEvent.ShowSnackbar
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState
import app.geeflow.ui.EventsDispatcher
import app.geeflow.ui.modifier.geeFlowInsetsPadding
import app.geeflow.ui.text.ClearText
import app.geeflow.ui.text.clearText
import app.geeflow.ui.theme.ClearScreenTheme
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import app.geeflow.ui.theme.disabled
import geeflow.shared.core.ui.generated.resources.Res
import geeflow.shared.core.ui.generated.resources.error_generic
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject

@Composable
internal fun DeviceDashboardScreen(
    viewModel: DeviceDashboardViewModel,
    profileListViewModel: ProfileListViewModel,
    navigator: Navigator,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val snackbarState = remember { SnackbarHostState() }
    val profileListViewState by profileListViewModel.viewState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState { 2 }
    val liveService = rememberLiveBrewServiceStarter()
    val localRecipes by liveService.recipes.recipes.collectAsStateWithLifecycle()
    val scale = koinInject<BookooScaleConnection>()
    val scaleState by scale.state.collectAsStateWithLifecycle()
    var previousScaleStatus by remember { mutableStateOf<String?>(null) }
    val scaleStatus = when {
        scaleState.connecting -> "connecting"
        scaleState.connected && scaleState.receiving -> "receiving"
        scaleState.connected -> "waiting"
        else -> "disconnected"
    }
    LaunchedEffect(scaleStatus) {
        val previous = previousScaleStatus
        previousScaleStatus = scaleStatus
        val message = when {
            previous == null -> null
            scaleStatus == "receiving" -> "Scale connected · receiving weight"
            scaleStatus == "waiting" -> "Scale connected · waiting for weight readings"
            scaleStatus == "disconnected" && previous != "disconnected" -> "Scale disconnected"
            else -> null
        }
        if (message != null) {
            snackbarState.currentSnackbarData?.dismiss()
            snackbarState.showSnackbar(message)
        }
    }
    val liveBrewCoordinator = koinInject<LiveBrewTestCoordinator>()
    val liveRun by liveBrewCoordinator.state.collectAsStateWithLifecycle()
    var selectedLiveRecipe by remember { mutableStateOf<LiveRecipeDocument?>(null) }

    val provider = koinInject<DeviceControllerProvider>()
    val deviceId = viewState.device.id
    val ownsLiveShot = liveRun.deviceId == deviceId && liveRun.mode in setOf(
        LiveTestMode.LiveControl,
        LiveTestMode.CommandEncodingProbe,
    )
    val liveActive = ownsLiveShot && liveRun.active
    val liveStopPending = ownsLiveShot && liveRun.stopPending
    val runRecipe = remember(liveRun.recipeSnapshot) {
        liveRun.recipeSnapshot?.let { runCatching { LiveRecipeJson.decode(it) }.getOrNull() }
    }
    val selectedRecipe = if (liveActive || liveStopPending) runRecipe ?: selectedLiveRecipe else selectedLiveRecipe
    val sameRecipe = selectedRecipe != null && liveRun.deviceId == deviceId && runRecipe?.id == selectedRecipe.id
    val sample = liveRun.lastSample.takeIf { sameRecipe }
    var reportedExtractionRunId by remember { mutableStateOf<Long?>(null) }
    androidx.compose.runtime.LaunchedEffect(liveRun.runId, liveRun.samples, sameRecipe) {
        val runId = liveRun.runId ?: return@LaunchedEffect
        if (!sameRecipe || reportedExtractionRunId == runId) return@LaunchedEffect
        val extractionPhaseId = runRecipe?.phases?.firstOrNull { phase ->
            phase.name.trim().equals("Extraction", true) || phase.name.trim().equals("Extraction start", true)
        }?.id ?: return@LaunchedEffect
        liveRun.samples.firstOrNull { it.phaseId == extractionPhaseId }?.let { extractionSample ->
            reportedExtractionRunId = runId
            viewModel.handleEvent(DeviceDashboardEvent.ExtractionPhaseStarted(extractionSample.elapsedMillis))
        }
    }
    val liveIssues = remember(provider, deviceId, selectedRecipe) {
        selectedRecipe?.let { LiveRecipeCompiler.issues(it, provider.getController(deviceId).profilingCapabilities) }.orEmpty()
    }

    ForegroundEffect(viewModel) {
        viewModel.handleEvent(DeviceDashboardEvent.Resumed)
    }

    RecipeDashboardContent(
        state = viewState,
        onEvent = viewModel::handleEvent,
        profiles = profileListViewState,
        onProfileListEvent = profileListViewModel::handleEvent,
        snackbarState = snackbarState,
        pagerState = pagerState,
        selectedLiveRecipe = selectedRecipe,
        localRecipes = localRecipes,
        scaleConnected = scaleState.connected,
        scaleReceiving = scaleState.receiving,
        connectingScale = scaleState.connecting,
        cupFlowGramsPerSecond = sample?.let { it.reportedCupFlowGramsPerSecond ?: it.derivedCupFlowGramsPerSecond },
        scaleWeightGrams = sample?.weightGrams,
        liveSamples = liveRun.samples.takeIf { sameRecipe }.orEmpty(),
        liveIssues = liveIssues,
        liveActive = liveActive,
        liveStopPending = liveStopPending,
        onStopLiveRecipe = { coroutineScope.launch { liveBrewCoordinator.stop(deviceId) } },
            onLiveRecipeSelected = { recipe ->
            selectedLiveRecipe = recipe
                viewModel.handleEvent(
                    DeviceDashboardEvent.LiveRecipeSelected(
                        name = recipe.name,
                        description = recipe.mappings.joinToString("\n"),
                        extractionPhaseId = recipe.phases.firstOrNull { phase ->
                            phase.name.trim().equals("Extraction", true) || phase.name.trim().equals("Extraction start", true)
                        }?.id,
                    ),
                )
            coroutineScope.launch { pagerState.animateScrollToPage(1) }
        },
        onLiveRecipeFavoriteChanged = liveService.recipes::save,
        onScaleToggle = {
            if (scaleState.connected) {
                coroutineScope.launch { scale.disconnect() }
            } else {
                viewModel.withBluetoothPermissions(requireBluetoothPermission = true) {
                    try {
                        scale.connectBookoo(coroutineScope)
                    } catch (error: Exception) {
                        snackbarState.showSnackbar(error.message ?: "Could not connect BOOKOO scale")
                    }
                }
            }
        },
        preferredDoseGrams = viewState.preferredShotDoseGrams,
        onBackToLibrary = {
            coroutineScope.launch {
                pagerState.animateScrollToPage(0)
                if (viewState.brew.historyId == null) selectedLiveRecipe = null
            }
        },
        onStartLiveRecipe = { settings ->
            val recipe = selectedLiveRecipe
            val scaleId = scaleState.device?.id
            if (recipe == null || liveBrewCoordinator.owns(deviceId) || liveIssues.isNotEmpty()) return@RecipeDashboardContent
            if (!scaleState.connected || scaleId == null) {
                coroutineScope.launch { snackbarState.showSnackbar("Connect a BOOKOO scale before starting this recipe") }
            } else {
                val shotRecipe = recipe.copy(
                    settings = recipe.settings.copy(
                        doseGrams = settings.doseGrams,
                        ratio = settings.ratio,
                        targetWeightGrams = settings.targetGrams,
                        useRatio = false,
                    ),
                )
                liveService.startRecipe(viewState.device.id, scaleId, shotRecipe)?.let { message ->
                    liveService.requestNotificationPermission()
                    coroutineScope.launch { snackbarState.showSnackbar(message) }
                }
            }
        },
    )

    EventsDispatcher(profileListViewModel.events) {
        when (it) {
            is SelectProfile -> {
                if (it.navigateToBrewPage) selectedLiveRecipe = null
                if (selectedLiveRecipe == null) viewModel.handleEvent(DeviceDashboardEvent.ProfileSelected(it.id))
                if (it.navigateToBrewPage) coroutineScope.launch { pagerState.animateScrollToPage(1) }
            }
            is ShowHistoryBrew -> {
                viewModel.handleEvent(
                    DeviceDashboardEvent.HistoryBrewSelected(
                        name = it.name,
                        description = it.description,
                        durationSeconds = it.durationSeconds,
                        data = it.data,
                        scaleSamples = it.scaleSamples,
                        targetData = it.targetData,
                        targetWeightGrams = it.targetWeightGrams,
                        phaseProgram = it.phaseProgram,
                        phaseTransitions = it.phaseTransitions,
                        id = it.id,
                        startedAt = it.startedAt,
                        endedAt = it.endedAt,
                        doseGrams = it.doseGrams,
                        grindSetting = it.grindSetting,
                        extractionStartMillis = it.extractionStartMillis,
                        extractionStartEstimated = it.extractionStartEstimated,
                        finalWeightGrams = it.finalWeightGrams,
                        recipeRatio = it.recipeRatio,
                        recipeTargetGrams = it.recipeTargetGrams,
                        recordedPhaseIds = it.recordedPhaseIds,
                    ),
                )
                coroutineScope.launch { pagerState.animateScrollToPage(1) }
            }

            is ShowSnackbar -> coroutineScope.launch {
                snackbarState.currentSnackbarData?.dismiss()
                snackbarState.showSnackbar(it.message)
            }
        }
    }

    NavigatorEffect(navigator, viewModel.navEvent)
    NavigatorEffect(navigator, profileListViewModel.navEvent)

    EventsDispatcher(viewModel.events) {
        when (it) {
            is DeviceDashboardViewModelEvent.SwitchToDetails -> coroutineScope.launch {
                pagerState.animateScrollToPage(1)
            }

            is DeviceDashboardViewModelEvent.SelectProfile ->
                profileListViewModel.handleEvent(ProfileListEvent.ProfileSelected(it.id))

            is DeviceDashboardViewModelEvent.ShowSnackbar -> coroutineScope.launch {
                snackbarState.currentSnackbarData?.dismiss()
                snackbarState.showSnackbar(it.message)
            }
        }
    }

    MaintenanceReminderHost(
        deviceId = viewState.device.id,
        blocked = viewState.dialog != null || navigator.getCurrentDestination() != DeviceDashboard(viewState.device.id),
        onOpen = { viewModel.handleEvent(DeviceDashboardEvent.MaintenanceReminderOpened(it)) },
        onError = { coroutineScope.launch { snackbarState.showSnackbar(getString(Res.string.error_generic)) } },
    )

    viewState.dialog?.let {
        DeviceDashboardDialog(
            model = it,
            error = viewState.device.error,
            onEvent = viewModel::handleEvent,
        )
    }
}

@Composable
private fun DeviceDashboardContent(
    viewState: DeviceDashboardViewState,
    onEvent: (DeviceDashboardEvent) -> Unit = {},
    profileListViewState: ProfileListViewState,
    snackbarState: SnackbarHostState = SnackbarHostState(),
    onProfileListEvent: (ProfileListEvent) -> Unit,
    pagerState: androidx.compose.foundation.pager.PagerState,
) {
    ClearScreenTheme {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxWidth >= DashboardExpandedWidth.dp) {
                ExpandedDashboard(
                    viewState = viewState,
                    profileListViewState = profileListViewState,
                    snackbarState = snackbarState,
                    onEvent = onEvent,
                    onProfileListEvent = onProfileListEvent,
                )
            } else {
                CompactDashboard(
                    viewState = viewState,
                    snackbarState = snackbarState,
                    onEvent = onEvent,
                    profileListViewState = profileListViewState,
                    onProfileListEvent = onProfileListEvent,
                    pagerState = pagerState,
                    phone = maxWidth < 600.dp,
                )
            }
            FlushOverlay(viewState, onEvent)
        }
    }
}

@Composable
private fun ExpandedDashboard(
    viewState: DeviceDashboardViewState,
    profileListViewState: ProfileListViewState,
    snackbarState: SnackbarHostState,
    onEvent: (DeviceDashboardEvent) -> Unit = {},
    onProfileListEvent: (ProfileListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var detailsVisible by remember { mutableStateOf(false) }
    Box(modifier.background(MaterialTheme.colorScheme.background).geeFlowInsetsPadding()) {
        Column(Modifier.fillMaxSize()) {
            DashboardHeader(viewState, onEvent)
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    Modifier.weight(MainColumnWeight).fillMaxHeight().padding(start = 12.dp, end = 6.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExtractionReadout(
                        viewState,
                        profileListViewState.profiles,
                        { onEvent(DeviceDashboardEvent.ToggleChartVisibility(it)) },
                        Modifier.fillMaxWidth()
                    )
                    BrewCharts(
                        model = viewState.chartModel(profileListViewState),
                        visibleCharts = viewState.visibleCharts,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                    DashboardControls(viewState, onEvent, { detailsVisible = true })
                }
                ProfileList(
                    profileListViewState,
                    onProfileListEvent,
                    Modifier.weight(SideColumnWeight).fillMaxHeight().padding(start = 6.dp, end = 12.dp, bottom = 8.dp)
                )
            }
        }
        SnackbarHost(snackbarState, Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp))
    }
    if (detailsVisible) DashboardDetails(viewState, onEvent) { detailsVisible = false }
}

@Composable
private fun DashboardHeader(state: DeviceDashboardViewState, onEvent: (DeviceDashboardEvent) -> Unit) {
    TopBar(
        device = state.device,
        photoFileName = state.user.photoFileName,
        profileName = state.brew.name,
        statusText = dashboardStatusText(state),
        onEvent = onEvent,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

@Composable
private fun DashboardControls(state: DeviceDashboardViewState, onEvent: (DeviceDashboardEvent) -> Unit, onDetails: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DashboardBrewButton(state, onEvent, Modifier.weight(1f))
        TextButton(onClick = onDetails, modifier = Modifier.width(76.dp).fillMaxHeight()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Info, contentDescription = null)
                Text(clearText(ClearText.Details), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun DashboardDetails(
    state: DeviceDashboardViewState,
    onEvent: (DeviceDashboardEvent) -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(clearText(ClearText.ShotDetails)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val labels = listOf(
                        if (state.device.isBrewing) clearText(ClearText.CupFlow) else clearText(ClearText.AvgCupFlow),
                        clearText(ClearText.CupOutput),
                        clearText(ClearText.WaterVolume),
                    )
                    labels.forEach { label ->
                        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                    }
                }
                BrewStatistics(
                    state.brew,
                    state.device.isBrewing,
                    state.visibleCharts,
                    { onEvent(DeviceDashboardEvent.ToggleChartVisibility(it)) },
                    Modifier.fillMaxWidth(),
                    showPrimaryMetrics = false,
                )
                Text(
                    clearText(ClearText.ToggleChartHint),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(clearText(ClearText.Close)) } },
    )
}

@Composable
private fun CompactDashboard(
    viewState: DeviceDashboardViewState,
    profileListViewState: ProfileListViewState,
    snackbarState: SnackbarHostState,
    onEvent: (DeviceDashboardEvent) -> Unit = {},
    onProfileListEvent: (ProfileListEvent) -> Unit,
    pagerState: androidx.compose.foundation.pager.PagerState,
    phone: Boolean,
) {
    val scope = rememberCoroutineScope()
    var detailsVisible by remember { mutableStateOf(false) }
    Box(Modifier.background(MaterialTheme.colorScheme.background).fillMaxSize().geeFlowInsetsPadding()) {
        Column(Modifier.fillMaxSize()) {
            DashboardHeader(viewState, onEvent)
            TabRow(pagerState.currentPage, Modifier.fillMaxWidth().padding(horizontal = 12.dp)) { index ->
                scope.launch { pagerState.animateScrollToPage(index) }
            }
            HorizontalPager(pagerState, key = { it }, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                when (page) {
                    CompactDashboardPage.Details.ordinal -> Column(
                        Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ExtractionReadout(
                            viewState,
                            profileListViewState.profiles,
                            { onEvent(DeviceDashboardEvent.ToggleChartVisibility(it)) },
                            Modifier.fillMaxWidth(),
                            compact = phone,
                        )
                        BrewProfileSummary(viewState.brew.name)
                        BrewCharts(
                            model = viewState.chartModel(profileListViewState),
                            visibleCharts = viewState.visibleCharts,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                    }
                    CompactDashboardPage.Profiles.ordinal -> ProfileList(
                        profileListViewState,
                        onProfileListEvent,
                        Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp),
                        phone = phone,
                    )
                }
            }
            Box(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp, top = 4.dp)) {
                if (phone) {
                    PhoneBrewControls(viewState, onEvent, { detailsVisible = true })
                } else {
                    DashboardControls(viewState, onEvent, { detailsVisible = true })
                }
            }
        }
        SnackbarHost(snackbarState, Modifier.align(Alignment.BottomCenter))
    }
    if (detailsVisible) DashboardDetails(viewState, onEvent) { detailsVisible = false }
}

@Composable
private fun TabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    onSelected: (Int) -> Unit = {},
) = Row(modifier = modifier) {
    CompactDashboardPage.entries.forEachIndexed { index, page ->
        val color by animateColorAsState(
            targetValue = if (index == selectedTabIndex) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.primary.disabled()
            },
        )
        Text(
            text = when (page) {
                CompactDashboardPage.Details -> clearText(ClearText.Details)
                CompactDashboardPage.Profiles -> clearText(ClearText.Profiles)
            },
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .clickable { onSelected(index) }
                .padding(vertical = 10.dp)
                .fillMaxWidth()
                .weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = color,
        )
    }
}

private enum class CompactDashboardPage {
    Details,
    Profiles
}

private const val MainColumnWeight = 0.70f
private const val SideColumnWeight = 0.30f
private const val DashboardExpandedWidth = 1_100

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
internal fun DeviceDashboardPreviewContent() {
    val state = remember { mutableStateOf(getMockDeviceDashboardViewState()) }
    val pagerState = rememberPagerState { CompactDashboardPage.entries.size }
    DeviceDashboardContent(
        viewState = state.value,
        profileListViewState = getMockProfileListViewState(),
        onProfileListEvent = {},
        pagerState = pagerState,
    )
}
