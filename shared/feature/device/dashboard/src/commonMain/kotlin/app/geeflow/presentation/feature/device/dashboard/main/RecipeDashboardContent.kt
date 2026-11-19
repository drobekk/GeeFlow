package app.geeflow.presentation.feature.device.dashboard.main

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.ConditionOperator
import app.geeflow.data.brew.model.MeasurementScope
import app.geeflow.data.brew.model.TargetSourceKind
import app.geeflow.data.brew.model.ThresholdComparison
import app.geeflow.data.brew.model.BrewScaleSample
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.domain.device.recipe.LiveRecipeDocument
import app.geeflow.domain.device.LiveTestSample
import app.geeflow.domain.device.recipe.RecipeMetric
import app.geeflow.domain.device.recipe.RecipeControl
import app.geeflow.domain.device.recipe.RecipeValueSource
import app.geeflow.presentation.feature.device.dashboard.components.BrewButtonState
import app.geeflow.presentation.feature.device.dashboard.components.DashboardBrewButton
import app.geeflow.presentation.feature.device.dashboard.components.ExtractionReadout
import app.geeflow.presentation.feature.device.dashboard.components.BrewShotSummaryFooter
import app.geeflow.presentation.feature.device.dashboard.components.BrewCharts
import app.geeflow.presentation.feature.device.dashboard.components.FlushOverlay
import app.geeflow.presentation.feature.device.dashboard.components.TopBar
import app.geeflow.presentation.feature.device.dashboard.components.BrewMachineSidebar
import app.geeflow.presentation.feature.device.dashboard.components.DashboardBrewControlsRail
import app.geeflow.presentation.feature.device.dashboard.model.BrewChartModel
import app.geeflow.presentation.feature.device.dashboard.model.ChartData
import app.geeflow.presentation.feature.device.dashboard.model.ChartPhaseBoundary
import app.geeflow.presentation.feature.device.dashboard.model.hasElapsedChartMeasurements
import app.geeflow.presentation.feature.device.dashboard.model.chartModel
import app.geeflow.data.brew.model.plannedDurationMillis
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileList
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState
import app.geeflow.presentation.feature.device.dashboard.profiles.RecipeCurvePreview
import app.geeflow.ui.modifier.geeFlowInsetsPadding
import app.geeflow.ui.theme.ClearScreenTheme
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_phase_and
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_phase_or
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_phase_min
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_phase_stable
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_phase_dynamic
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_phase_max
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_phase_until
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_phase_stage
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_phase_total
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_phase_pressure_pump
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_phase_pressure_group
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_phase_pressure_boiler
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_phase_water_flow
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_phase_cup_flow
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_phase_weight
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_phase_volume
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_phase_time
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_source_details
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_parameters
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_history_grind_saved
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_history_save_grind
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_details_description
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_dose
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_grind
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_machine_weight_target
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_use_profile
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_ratio
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_recipe_library
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_stop_brew
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_review_recipe
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_recipe_unavailable
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_machine_not_ready
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_reference_yield
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_pause
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_weight_stop_unavailable
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_decrease
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_increase
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_machine_error
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_brewing_state
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_charts
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_pressure_chart
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_water_flow_chart
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_cup_flow_chart
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_volume_relative_chart
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_weight_relative_chart
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RecipeDashboardReviewPreviewContent(initialPage: Int = BrewPage) {
    val state = remember { getMockDeviceDashboardViewState().copy(brewButtonState = BrewButtonState.Brewing) }
    val profiles = remember { getMockProfileListViewState() }
    val pagerState = rememberPagerState(initialPage = initialPage) { 2 }
    RecipeDashboardContent(
        state = state,
        profiles = profiles,
        onEvent = {},
        onProfileListEvent = {},
        pagerState = pagerState,
        snackbarState = remember { SnackbarHostState() },
        selectedLiveRecipe = null,
        localRecipes = emptyList(),
        scaleConnected = false,
        scaleReceiving = false,
        connectingScale = false,
        cupFlowGramsPerSecond = null,
        scaleWeightGrams = null,
        liveSamples = emptyList(),
        liveIssues = emptyList(),
        liveActive = false,
        liveStopPending = false,
        onStopLiveRecipe = {},
        onLiveRecipeSelected = {},
        onLiveRecipeFavoriteChanged = {},
        onScaleToggle = {},
        onBackToLibrary = {},
        onStartLiveRecipe = {},
    )
}

@Composable
internal fun RecipeDashboardContent(
    state: DeviceDashboardViewState,
    profiles: ProfileListViewState,
    onEvent: (DeviceDashboardEvent) -> Unit,
    onProfileListEvent: (ProfileListEvent) -> Unit,
    pagerState: PagerState,
    snackbarState: SnackbarHostState,
    selectedLiveRecipe: LiveRecipeDocument?,
    localRecipes: List<LiveRecipeDocument>,
    scaleConnected: Boolean,
    scaleReceiving: Boolean,
    connectingScale: Boolean,
    cupFlowGramsPerSecond: Float?,
    scaleWeightGrams: Float?,
    liveSamples: List<LiveTestSample>,
    liveIssues: List<String>,
    liveActive: Boolean,
    liveStopPending: Boolean,
    onStopLiveRecipe: () -> Unit,
    onLiveRecipeSelected: (LiveRecipeDocument) -> Unit,
    onLiveRecipeFavoriteChanged: (LiveRecipeDocument) -> Unit,
    onScaleToggle: () -> Unit,
    onBackToLibrary: () -> Unit,
    onStartLiveRecipe: (ShotRecipeSettings) -> Unit,
    preferredDoseGrams: Float = ShotRecipeSettings.DEFAULT_DOSE_GRAMS,
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val chartStateHolder = rememberSaveableStateHolder()
    val selectedProfile = profiles.profiles.firstOrNull { it.selected }.takeUnless { selectedLiveRecipe != null }
    val shotSettings = remember(selectedProfile?.id, selectedLiveRecipe?.id, preferredDoseGrams) {
        val target = when (val finish = selectedProfile?.finishCondition) {
            is Condition.Weight -> finish.target
            else -> selectedLiveRecipe?.settings?.yieldGrams ?: ShotRecipeSettings.DEFAULT_TARGET_GRAMS
        }
        ShotRecipeSettings.forRecipe(preferredDoseGrams, target)
    }
    var settings by remember(selectedProfile?.id, selectedLiveRecipe?.id) {
        mutableStateOf(shotSettings)
    }
    val active = liveActive || liveStopPending || state.device.isBrewing || state.device.dispensingHotWater
    val showingBrew = pagerState.currentPage == BrewPage
    androidx.compose.runtime.LaunchedEffect(showingBrew) {
        if (showingBrew) focusManager.clearFocus(force = true)
    }
    if (showingBrew) {
        NavigationBackHandler(rememberNavigationEventState(NavigationEventInfo.None)) {
            if (!active) onBackToLibrary()
        }
    }
    androidx.compose.runtime.LaunchedEffect(preferredDoseGrams) {
        if (settings.doseGrams != preferredDoseGrams) settings = settings.withDose(preferredDoseGrams)
    }
    androidx.compose.runtime.LaunchedEffect(selectedProfile?.id, selectedLiveRecipe?.id) {
        settings = shotSettings
    }
    val chart = selectedLiveRecipe?.takeUnless { state.brew.historyId != null }?.let {
        liveRecipeChartModel(it, liveSamples, liveActive, state.brew.shotDurationSeconds)
    } ?: state.chartModel(profiles)
    val cupFlow = state.brew.data.maxByOrNull { it.key }?.value?.weightPerSecond
    val cupFlowRate = cupFlow.takeIf { state.device.isBrewing }

    ClearScreenTheme {
      BoxWithConstraints(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).geeFlowInsetsPadding(),
      ) {
        val expanded = maxWidth >= 900.dp
        val phoneBottomClearance = if (expanded) 0.dp else PhoneStandClearance
        Column(Modifier.fillMaxSize().padding(bottom = phoneBottomClearance)) {
            if (!(expanded && showingBrew)) {
                TopBar(
                    device = state.device,
                    photoFileName = state.user.photoFileName,
                    scaleConnected = scaleConnected,
                    scaleConnecting = connectingScale,
                    scaleReceiving = scaleReceiving,
                    onScaleToggle = onScaleToggle,
                    compactPhoneTemperatures = true,
                    onEvent = onEvent,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
            HorizontalPager(
                state = pagerState,
                key = { it },
                userScrollEnabled = false,
                beyondViewportPageCount = 1,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { pageIndex ->
                val pageOffset = pagerState.getOffsetDistanceInPages(pageIndex).coerceIn(-1f, 1f)
                val scale by animateFloatAsState(1f - kotlin.math.abs(pageOffset) * .06f, label = "recipePageZoom")
                Box(Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale }) {
                    if (pageIndex == LibraryPage) {
                        RecipeLibraryPage(
                            profiles = profiles,
                            localRecipes = localRecipes,
                            onProfileListEvent = onProfileListEvent,
                            onLiveRecipeSelected = onLiveRecipeSelected,
                            onLiveRecipeFavoriteChanged = onLiveRecipeFavoriteChanged,
                            expanded = expanded,
                            liveScaleConnected = scaleConnected,
                        )
                    } else {
                        BrewSetupPage(
                            state = state,
                            profiles = profiles,
                            selectedProfile = selectedProfile,
                            liveRecipe = selectedLiveRecipe.takeUnless { state.brew.historyId != null },
                            active = active,
                            liveActive = liveActive || liveStopPending,
                            liveStopPending = liveStopPending,
                            liveIssues = liveIssues,
                            onStopLiveRecipe = onStopLiveRecipe,
                            onBackToLibrary = onBackToLibrary,
                            settings = settings,
                            onSettingsChange = {
                                val doseChanged = settings.doseGrams != it.doseGrams
                                settings = it
                                if (doseChanged) onEvent(DeviceDashboardEvent.ShotDoseChanged(it.doseGrams))
                            },
                            chart = chart,
                            onProfileListEvent = onProfileListEvent,
                            cupFlowRate = cupFlowRate,
                            liveCupFlowRate = cupFlowGramsPerSecond,
                            liveScaleWeightGrams = scaleWeightGrams,
                            scaleConnected = scaleConnected,
                            scaleReceiving = scaleReceiving,
                            onEvent = onEvent,
                            onStart = { onStartLiveRecipe(settings) },
                            expanded = expanded,
                            connectingScale = connectingScale,
                            onScaleToggle = onScaleToggle,
                            chartStateHolder = chartStateHolder,
                        )
                    }
                }
            }
        }
        SnackbarHost(
            snackbarState,
            Modifier.align(Alignment.BottomCenter).padding(
                start = 16.dp,
                end = 16.dp,
                bottom = phoneBottomClearance,
            ),
        )
        FlushOverlay(state, onEvent)
      }
    }
}

@Composable
private fun RecipeLibraryPage(
    profiles: ProfileListViewState,
    localRecipes: List<LiveRecipeDocument>,
    onProfileListEvent: (ProfileListEvent) -> Unit,
    onLiveRecipeSelected: (LiveRecipeDocument) -> Unit,
    onLiveRecipeFavoriteChanged: (LiveRecipeDocument) -> Unit,
    expanded: Boolean,
    liveScaleConnected: Boolean,
) {
    if (expanded) {
        val selectedProfile = profiles.profiles.firstOrNull { it.selected } ?: profiles.profiles.firstOrNull()
        if (profiles.showHistory || selectedProfile == null) {
            ProfileList(
                viewState = profiles,
                onEvent = onProfileListEvent,
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp),
                onLiveRecipeSelected = onLiveRecipeSelected,
                liveScaleConnected = liveScaleConnected,
                onLiveRecipeFavoriteChanged = onLiveRecipeFavoriteChanged,
            )
        } else {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProfileList(
                    viewState = profiles,
                    onEvent = onProfileListEvent,
                    modifier = Modifier.weight(.62f).fillMaxHeight(),
                    navigateOnProfileSelect = false,
                    onLiveRecipeSelected = onLiveRecipeSelected,
                    liveScaleConnected = liveScaleConnected,
                    onLiveRecipeFavoriteChanged = onLiveRecipeFavoriteChanged,
                )
                TabletRecipePreview(
                    profile = selectedProfile,
                    onUseProfile = { onProfileListEvent(ProfileListEvent.ProfileSelected(selectedProfile.id)) },
                    modifier = Modifier.weight(.38f).fillMaxHeight(),
                )
            }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileList(
                viewState = profiles,
                onEvent = onProfileListEvent,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                phone = true,
                onLiveRecipeSelected = onLiveRecipeSelected,
                liveScaleConnected = liveScaleConnected,
                onLiveRecipeFavoriteChanged = onLiveRecipeFavoriteChanged,
            )
        }
    }
}

@Composable
private fun TabletRecipePreview(
    profile: ProfileListViewState.Profile,
    onUseProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(profile.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            profile.description.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RecipeCurvePreview(profile, Modifier.fillMaxWidth().height(112.dp))
            val phases = (profile.program as? BrewProgram.Phases)?.phases.orEmpty()
            if (phases.isNotEmpty()) {
                HorizontalDivider()
                phases.forEachIndexed { index, phase ->
                    NativePhaseDetail(phase, index)
                }
            }
        }
        Button(onClick = onUseProfile, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(stringResource(Res.string.dashboard_use_profile))
        }
    }
}

@Composable
private fun BrewSetupPage(
    state: DeviceDashboardViewState,
    profiles: ProfileListViewState,
    selectedProfile: ProfileListViewState.Profile?,
    liveRecipe: LiveRecipeDocument?,
    active: Boolean,
    liveActive: Boolean,
    liveStopPending: Boolean,
    liveIssues: List<String>,
    onStopLiveRecipe: () -> Unit,
    onBackToLibrary: () -> Unit,
    settings: ShotRecipeSettings,
    onSettingsChange: (ShotRecipeSettings) -> Unit,
    chart: BrewChartModel,
    onProfileListEvent: (ProfileListEvent) -> Unit,
    cupFlowRate: Float?,
    liveCupFlowRate: Float?,
    liveScaleWeightGrams: Float?,
    scaleConnected: Boolean,
    scaleReceiving: Boolean,
    onEvent: (DeviceDashboardEvent) -> Unit,
    onStart: () -> Unit,
    expanded: Boolean,
    connectingScale: Boolean,
    onScaleToggle: () -> Unit,
    chartStateHolder: androidx.compose.runtime.saveable.SaveableStateHolder,
) {
    val nativeWeightStop = selectedProfile?.finishCondition is Condition.Weight
    val nativeScaleReady = nativeWeightStop && state.device.smartScaleConnected
    // The physical machine's native stop register accepts whole grams.
    val referenceGrams = if (nativeScaleReady && !state.device.isDemo && selectedProfile?.experimental == false) {
        settings.machineStopGrams
    } else settings.targetGrams
    val nativeTargetGrams = referenceGrams.takeIf { nativeScaleReady }
    val visibleCharts = state.visibleCharts
    val hasWeightSource = if (liveRecipe != null) scaleConnected else {
        scaleConnected || state.device.smartScaleConnected
    }
    val appCutoffNeeded = hasWeightSource &&
        (settings.stopAtYield || scaleConnected || state.device.smartScaleConnected)
    val showFooter = state.brew.startedAt != null || state.brew.historyId != null || active
    val blocker = when {
        active -> null
        liveIssues.isNotEmpty() -> if (liveRecipe?.mappings?.isNotEmpty() == true && !liveRecipe.mappingsAccepted) {
            stringResource(Res.string.dashboard_review_recipe)
        } else stringResource(Res.string.dashboard_recipe_unavailable)
        state.device.connectionStatus != DeviceDashboardViewState.Device.ConnectionStatus.Connected ->
            stringResource(Res.string.dashboard_machine_not_ready)
        state.device.alarm -> stringResource(Res.string.dashboard_machine_error)
        state.brewButtonState != BrewButtonState.Idle -> null
        liveRecipe == null && selectedProfile == null -> stringResource(Res.string.dashboard_recipe_unavailable)
        else -> null
    }
    val setup: @Composable () -> Unit = {
        if (state.brew.historyId != null) {
            HistoricalRecipeDetails(state, onEvent, onProfileListEvent)
        } else {
        DoseRatioControls(settings, referenceGrams, !active && state.brewButtonState == BrewButtonState.Idle,
            compact = expanded,
            grindValue = state.grindDraft,
            onGrindChange = { onEvent(DeviceDashboardEvent.GrindDraftChanged(it)) },
            showGrind = !active && state.brew.historyId == null && state.brew.startedAt == null,
            onChange = onSettingsChange,
            modifier = Modifier.fillMaxWidth())
        if (nativeTargetGrams != null || liveRecipe != null && scaleConnected || settings.stopAtYield) {
            Text(
                if (appCutoffNeeded) {
                    "Stop at ${settings.targetGrams.toOneDecimal()} g"
                } else if (nativeScaleReady || liveRecipe != null && scaleConnected) {
                    stringResource(Res.string.dashboard_machine_weight_target, referenceGrams.toOneDecimal())
                } else {
                    "Reference yield ${referenceGrams.toOneDecimal()} g"
                },
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        if (settings.stopAtYield && !hasWeightSource) {
            Text(
                stringResource(Res.string.dashboard_weight_stop_unavailable),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RecipeDetails(selectedProfile, liveRecipe, liveIssues)
        }
    }
    val brewControlsEvent: (DeviceDashboardEvent) -> Unit = { event ->
        if (event is DeviceDashboardEvent.BrewClicked) onEvent(event.copy(
            recipeDoseGrams = settings.doseGrams,
            recipeRatio = settings.ratio,
            recipeTargetGrams = referenceGrams,
        )) else onEvent(event)
    }
    val readouts: @Composable () -> Unit = {
        ExtractionReadout(
            state,
            profiles.profiles,
            { onEvent(DeviceDashboardEvent.ToggleChartVisibility(it)) },
            Modifier.fillMaxWidth(),
            compact = true,
            cupFlowGramsPerSecond = if (liveRecipe != null) liveCupFlowRate else cupFlowRate,
            scaleWeightGrams = liveScaleWeightGrams,
            scaleConnected = scaleConnected,
            columns = if (expanded) 4 else 2,
            targetWeightGrams = settings.targetGrams.takeIf { liveRecipe != null },
            nativeTargetWeightGrams = if (appCutoffNeeded && nativeWeightStop) settings.targetGrams else nativeTargetGrams,
            liveSample = liveSamplesLast(chart).takeIf { liveRecipe != null },
        )
    }
    val charts: @Composable (Modifier) -> Unit = { modifier ->
        BrewCharts(
            model = chart,
            visibleCharts = visibleCharts,
            modifier = modifier,
            combinedOnPhone = true,
            scrollPanels = true,
            forceCombined = true,
        )
    }

    val recipeName = if (state.brew.historyId != null) state.brew.name else liveRecipe?.name?.takeIf { it.isNotBlank() }
        ?: selectedProfile?.name?.takeIf { it.isNotBlank() }
        ?: state.brew.name
    val recipeTitle: @Composable (Modifier) -> Unit = { titleModifier ->
        if (recipeName.isNotBlank()) {
            Text(
                recipeName,
                modifier = titleModifier,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else Box(titleModifier)
    }
    val recipeLibraryButton: @Composable () -> Unit = {
        if (!active) {
            TextButton(onClick = onBackToLibrary) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Text(
                    stringResource(Res.string.dashboard_recipe_library),
                    modifier = Modifier.padding(start = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }
    val tabletBackButton: @Composable () -> Unit = {
        if (!active) {
            IconButton(onClick = onBackToLibrary) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.dashboard_recipe_library),
                )
            }
        }
    }
    val brewingState: @Composable () -> Unit = {
        if (active) {
            Text(
                stringResource(Res.string.dashboard_brewing_state),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
    val header: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            recipeLibraryButton()
            recipeTitle(Modifier.weight(1f))
            brewingState()
            ChartVisibilityMenu(visibleCharts) { onEvent(DeviceDashboardEvent.ToggleChartVisibility(it)) }
        }
    }
    val tabletHeader: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            tabletBackButton()
            Text(stringResource(Res.string.dashboard_recipe_library), style = MaterialTheme.typography.labelMedium)
            Text("  |  ", color = MaterialTheme.colorScheme.outline)
            recipeTitle(Modifier.weight(1f))
            brewingState()
            ChartVisibilityMenu(visibleCharts, modifier = Modifier.width(130.dp)) {
                onEvent(DeviceDashboardEvent.ToggleChartVisibility(it))
            }
        }
    }
    val targetWeight = when {
        appCutoffNeeded -> settings.targetGrams
        nativeTargetGrams != null -> referenceGrams
        else -> null
    }
    val footer: @Composable (Modifier) -> Unit = { footerModifier ->
        if (showFooter) {
            BrewShotSummaryFooter(
                state = state,
                profiles = profiles,
                measurements = chart.measurements,
                scaleSamples = chart.scaleMeasurements,
                phaseProgram = state.brew.phaseProgram ?: selectedProfile?.program.takeUnless { state.brew.historyId != null },
                phaseTransitions = state.brew.phaseTransitions,
                inProgress = active,
                onEvent = onEvent,
                onHistoryNavigate = { id -> onProfileListEvent(ProfileListEvent.HistoryBrewSelected(id)) },
                onHistoryGrindSettingChanged = { id, value ->
                    onProfileListEvent(ProfileListEvent.HistoryGrindSettingChanged(id, value))
                },
                modifier = footerModifier,
            )
        }
    }

    if (expanded) {
        Row(
            Modifier.fillMaxSize().padding(start = 12.dp, top = 4.dp, end = 8.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                tabletHeader()
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(
                        Modifier.weight(.29f).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        setup()
                        blocker?.let { StatusNote(it) }
                    }
                    Column(
                        Modifier.weight(.71f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        readouts()
                        chartStateHolder.SaveableStateProvider("profile-chart") {
                            charts(Modifier.weight(1f).fillMaxWidth().heightIn(min = 140.dp))
                        }
                        footer(Modifier.fillMaxWidth())
                    }
                }
            }
            BrewMachineSidebar(
                device = state.device,
                scaleConnected = scaleConnected,
                scaleConnecting = connectingScale,
                scaleReceiving = scaleReceiving,
                onScaleToggle = onScaleToggle,
                onEvent = onEvent,
                modifier = Modifier.width(190.dp).fillMaxHeight(),
            ) {
                DashboardBrewControlsRail(
                    viewState = state,
                    onEvent = brewControlsEvent,
                    modifier = Modifier.fillMaxWidth(),
                    targetWeightGrams = targetWeight,
                    stopAtYield = appCutoffNeeded,
                    onStartLiveRecipe = onStart.takeIf { liveRecipe != null },
                    onStopLiveRecipe = onStopLiveRecipe.takeIf { liveActive },
                    liveStopPending = liveStopPending,
                    startEnabled = blocker == null,
                )
            }
        }
    } else {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            header()
            if (active) {
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    readouts()
                    Box(Modifier.fillMaxWidth().height((visibleCharts.size.coerceAtLeast(1) * 170 + 24).dp)) {
                        chartStateHolder.SaveableStateProvider("profile-chart") { charts(Modifier.fillMaxSize()) }
                    }
                    footer(Modifier.fillMaxWidth())
                }
            } else {
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    setup()
                    readouts()
                    chartStateHolder.SaveableStateProvider("profile-chart") {
                        charts(Modifier.fillMaxWidth().height((visibleCharts.size.coerceAtLeast(1) * 170 + 24).dp))
                    }
                    footer(Modifier.fillMaxWidth())
                }
            }
            blocker?.let { StatusNote(it) }
            if (liveActive) {
                Button(
                    onClick = onStopLiveRecipe,
                    enabled = !liveStopPending,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(Res.string.device_dashboard_stop_brew)) }
            } else {
                DashboardBrewButton(
                    state,
                    brewControlsEvent,
                    Modifier.fillMaxWidth().height(64.dp),
                    targetWeightGrams = targetWeight,
                    stopAtYield = appCutoffNeeded,
                    onStartLiveRecipe = onStart.takeIf { liveRecipe != null },
                    startEnabled = blocker == null,
                )
            }
        }
    }
}

@Composable
private fun StatusNote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

private fun liveSamplesLast(chart: BrewChartModel) = chart.measurements.maxByOrNull { it.key }

@Composable
private fun ChartVisibilityMenu(
    visibleCharts: Set<DeviceDashboardViewState.DashboardChartType>,
    modifier: Modifier = Modifier,
    onToggle: (DeviceDashboardViewState.DashboardChartType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        DeviceDashboardViewState.DashboardChartType.Pressure to Res.string.device_dashboard_pressure_chart,
        DeviceDashboardViewState.DashboardChartType.FlowRate to Res.string.device_dashboard_water_flow_chart,
        DeviceDashboardViewState.DashboardChartType.WeightRate to Res.string.device_dashboard_cup_flow_chart,
        DeviceDashboardViewState.DashboardChartType.Volume to Res.string.device_dashboard_volume_relative_chart,
        DeviceDashboardViewState.DashboardChartType.Weight to Res.string.device_dashboard_weight_relative_chart,
    )
    Box(modifier) {
        TextButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Tune, contentDescription = null)
            Text(stringResource(Res.string.dashboard_charts), modifier = Modifier.padding(start = 4.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (chart, label) ->
                val checked = chart in visibleCharts
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    trailingIcon = {
                        Checkbox(checked = checked, onCheckedChange = { onToggle(chart) })
                    },
                    onClick = { onToggle(chart) },
                )
            }
        }
    }
}

@Composable
private fun HistoricalRecipeDetails(
    state: DeviceDashboardViewState,
    onEvent: (DeviceDashboardEvent) -> Unit,
    onProfileListEvent: (ProfileListEvent) -> Unit,
) {
    val brew = state.brew
    val id = brew.historyId ?: return
    var grind by remember(id) { mutableStateOf(brew.grindSetting.orEmpty()) }
    var grindSaved by remember(id) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(Res.string.dashboard_parameters), style = MaterialTheme.typography.titleSmall)
        HorizontalDivider()
        val (dose, ratio, target) = historicalRecipeValues(
            brew.doseGrams, brew.recipeRatio, brew.recipeTargetGrams,
            (brew.phaseProgram as? BrewProgram.Phases)?.recipe?.settings,
        )
        SettingAdjuster(stringResource(Res.string.dashboard_dose), dose?.let { "${it.toOneDecimal()} g" } ?: "—", false, {}, {})
        SettingAdjuster(stringResource(Res.string.dashboard_ratio), ratio?.let { "1:${it.toOneDecimal()}" } ?: "—", false, {}, {})
        SettingAdjuster(stringResource(Res.string.dashboard_reference_yield), target?.let { "${it.toOneDecimal()} g" } ?: "—", false, {}, {})
        OutlinedTextField(value = grind, onValueChange = { grind = it; grindSaved = false },
            label = { Text(stringResource(Res.string.dashboard_grind)) },
            trailingIcon = { if (grind.isBlank()) Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = {
            onEvent(DeviceDashboardEvent.GrindSettingChanged(grind))
            onProfileListEvent(ProfileListEvent.HistoryGrindSettingChanged(id.toString(), grind.trim().takeIf(String::isNotEmpty)))
            grindSaved = true
        }, enabled = grind.trim() != brew.grindSetting.orEmpty()) {
            Text(stringResource(Res.string.dashboard_history_save_grind))
        }
        if (grindSaved) Text(stringResource(Res.string.dashboard_history_grind_saved),
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(Res.string.profile_editor_details_description), style = MaterialTheme.typography.titleSmall)
        RecipeDescription(brew.description)
        (brew.phaseProgram as? BrewProgram.Phases)?.phases.orEmpty().forEachIndexed { index, phase ->
            NativePhaseDetail(phase, index)
        }
    }
}

@Composable
private fun RecipeDetails(
    profile: ProfileListViewState.Profile?,
    recipe: LiveRecipeDocument?,
    liveIssues: List<String>,
) {
    if (profile == null && recipe == null) return
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(Res.string.profile_editor_details_description), style = MaterialTheme.typography.titleSmall)
        RecipeDescription(profile?.description.orEmpty())
        val phases = (profile?.program as? BrewProgram.Phases)?.phases.orEmpty()
        phases.forEachIndexed { index, phase ->
            NativePhaseDetail(phase, index)
        }
        recipe?.phases?.forEachIndexed { index, phase ->
            val unit = when (phase.control) {
                RecipeControl.Pressure -> "bar"
                RecipeControl.MachineFlow -> "ml/s"
                RecipeControl.CupFlow -> "g/s"
                RecipeControl.Pause -> ""
            }
            val target = when {
                phase.control == RecipeControl.Pause -> stringResource(Res.string.dashboard_pause)
                phase.flowProbe != null -> stringResource(Res.string.dashboard_phase_dynamic)
                phase.target.source == RecipeValueSource.Fixed -> "${phase.target.value.toOneDecimal()} $unit"
                else -> stringResource(Res.string.dashboard_phase_dynamic)
            }
            val conditions = phase.exits.map { exit ->
                conditionText(recipeMetricLabel(exit.metric), exit.comparison, exit.threshold, exit.metric.unit.name, exit.scope) to (exit.metric == RecipeMetric.PhaseTime)
            }
            RecipePhaseDetail(phase.name.ifBlank { (index + 1).toString() }, target,
                phase.maximumDurationMillis, phase.minimumDurationMillis, 0, conditions, phase.conditionOperator)
        }
        (liveIssues + recipe?.mappings.orEmpty()).distinct().forEach {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RecipeDescription(description: String) {
    val (main, details) = remember(description) { recipeDescriptionSections(description) }
    var expanded by remember(description) { mutableStateOf(false) }
    Text(main.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)
    if (details.isNotBlank()) {
        TextButton(onClick = { expanded = !expanded }) { Text(stringResource(Res.string.dashboard_source_details)) }
        if (expanded) Text(details, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun NativePhaseDetail(phase: app.geeflow.data.brew.model.BrewPhase, index: Int) {
    val target = if (phase.adaptiveFlowProbe != null ||
        (phase.targetSource?.kind?.let { it != TargetSourceKind.Constant } == true)) {
        stringResource(Res.string.dashboard_phase_dynamic)
    } else when (val control = phase.control) {
        is PhaseControl.Pressure -> "${(phase.targetSource?.value ?: control.bar).toOneDecimal()} bar"
        is PhaseControl.Flow -> "${(phase.targetSource?.value ?: control.millilitresPerSecond).toOneDecimal()} ml/s"
        PhaseControl.PumpPause -> stringResource(Res.string.dashboard_pause)
    }
    val conditions = phase.exitConditions.map { exit ->
        val unit = when (exit.metric) {
            BrewMetric.PumpPressure, BrewMetric.GroupPressure, BrewMetric.BoilerPressure -> "Bar"
            BrewMetric.PumpFlow -> "MillilitresPerSecond"
            BrewMetric.PumpedVolume -> "Millilitres"
            BrewMetric.CupWeight -> "Grams"
            BrewMetric.PhaseTime -> "Seconds"
        }
        conditionText(brewMetricLabel(exit.metric), exit.comparison, exit.threshold, unit, exit.scope) to (exit.metric == BrewMetric.PhaseTime)
    }
    RecipePhaseDetail(phase.name.ifBlank { (index + 1).toString() }, target,
        phase.maximumDurationMillis, phase.minimumDurationMillis, phase.stabilityDurationMillis, conditions, phase.conditionOperator)
}

@Composable
private fun RecipePhaseDetail(name: String, target: String, durationMillis: Long, minimumMillis: Long,
    stableMillis: Long, conditions: List<Pair<String, Boolean>>, operator: ConditionOperator) {
    val maxDuration = stringResource(Res.string.dashboard_phase_max, (durationMillis / 1_000f).toOneDecimal())
    val andWord = stringResource(Res.string.dashboard_phase_and)
    val orWord = stringResource(Res.string.dashboard_phase_or)
    val times = conditions.filter { it.second }.map { it.first }
    val measurements = conditions.filterNot { it.second }.map { it.first }
    val measurementGroup = measurements.takeIf { it.isNotEmpty() }?.joinToString(
        if (operator == ConditionOperator.And) " $andWord " else " $orWord ",
        prefix = if (operator == ConditionOperator.And && measurements.size > 1 && times.isNotEmpty()) "(" else "",
        postfix = if (operator == ConditionOperator.And && measurements.size > 1 && times.isNotEmpty()) ")" else "",
    )
    val expression = (times + listOfNotNull(measurementGroup)).joinToString(" $orWord ")
    val timing = listOfNotNull(
        minimumMillis.takeIf { it > 0 }?.let { stringResource(Res.string.dashboard_phase_min, (it / 1_000f).toOneDecimal()) },
        stableMillis.takeIf { it > 0 }?.let { stringResource(Res.string.dashboard_phase_stable, (it / 1_000f).toOneDecimal()) },
        maxDuration,
    ).joinToString(" · ")
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Text(target, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End)
        }
        Text(if (conditions.isEmpty()) timing else stringResource(Res.string.dashboard_phase_until, expression, timing),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun conditionText(label: String, comparison: ThresholdComparison, threshold: Float,
    unit: String, scope: MeasurementScope): String {
    val sign = when (comparison) {
        ThresholdComparison.Above -> "≥"
        ThresholdComparison.Below -> "≤"
        ThresholdComparison.StrictlyAbove -> ">"
        ThresholdComparison.StrictlyBelow -> "<"
    }
    val suffix = when (unit) {
        "Bar" -> "bar"
        "MillilitresPerSecond" -> "ml/s"
        "GramsPerSecond" -> "g/s"
        "Millilitres" -> "ml"
        "Grams" -> "g"
        else -> "s"
    }
    val scopeLabel = stringResource(if (unit == "Seconds" || scope == MeasurementScope.Step) Res.string.dashboard_phase_stage else Res.string.dashboard_phase_total)
    return "$label ($scopeLabel) $sign ${threshold.toOneDecimal()} $suffix"
}

@Composable
private fun brewMetricLabel(metric: BrewMetric): String = stringResource(when (metric) {
    BrewMetric.PumpPressure -> Res.string.dashboard_phase_pressure_pump
    BrewMetric.GroupPressure -> Res.string.dashboard_phase_pressure_group
    BrewMetric.BoilerPressure -> Res.string.dashboard_phase_pressure_boiler
    BrewMetric.PumpFlow -> Res.string.dashboard_phase_water_flow
    BrewMetric.PumpedVolume -> Res.string.dashboard_phase_volume
    BrewMetric.CupWeight -> Res.string.dashboard_phase_weight
    BrewMetric.PhaseTime -> Res.string.dashboard_phase_time
})

@Composable
private fun recipeMetricLabel(metric: RecipeMetric): String = stringResource(when (metric) {
    RecipeMetric.Pressure -> Res.string.dashboard_phase_pressure_pump
    RecipeMetric.MachineFlow -> Res.string.dashboard_phase_water_flow
    RecipeMetric.CupFlow -> Res.string.dashboard_phase_cup_flow
    RecipeMetric.Weight -> Res.string.dashboard_phase_weight
    RecipeMetric.Volume -> Res.string.dashboard_phase_volume
    RecipeMetric.PhaseTime -> Res.string.dashboard_phase_time
})

@Composable
private fun DoseRatioControls(
    settings: ShotRecipeSettings,
    referenceGrams: Float,
    editable: Boolean,
    compact: Boolean,
    grindValue: String,
    onGrindChange: (String) -> Unit,
    showGrind: Boolean,
    onChange: (ShotRecipeSettings) -> Unit,
    modifier: Modifier,
) {
    Card(modifier, colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(Res.string.dashboard_parameters), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(6.dp))
            HorizontalDivider()
            SettingAdjuster(stringResource(Res.string.dashboard_dose), "${settings.doseGrams.toOneDecimal()} g", editable,
                onMinus = { onChange(settings.changeDose(-0.1f)) }, onPlus = { onChange(settings.changeDose(0.1f)) })
            if (!compact) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
                    listOf(15f, 18f).forEach { dose ->
                        FilterChip(
                            selected = settings.doseGrams == dose,
                            enabled = editable,
                            onClick = { onChange(settings.withDose(dose)) },
                            label = { Text("${dose.toInt()} g", fontSize = 13.sp) },
                        )
                    }
                }
            }
            SettingAdjuster(stringResource(Res.string.dashboard_ratio), "1:${settings.ratio.toOneDecimal()}", editable,
                onMinus = { onChange(settings.changeRatio(-0.1f)) }, onPlus = { onChange(settings.changeRatio(0.1f)) })
            SettingAdjuster(stringResource(Res.string.dashboard_reference_yield),
                "${referenceGrams.toOneDecimal()} g", editable,
                onMinus = { onChange(settings.changeTarget(-1f)) }, onPlus = { onChange(settings.changeTarget(1f)) })
            if (showGrind) {
                if (compact) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            stringResource(Res.string.dashboard_grind),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                        OutlinedTextField(
                            value = grindValue,
                            onValueChange = onGrindChange,
                            placeholder = { Text("—", style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(112.dp).heightIn(min = 48.dp, max = 56.dp),
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = grindValue,
                        onValueChange = onGrindChange,
                        label = { Text(stringResource(Res.string.dashboard_grind)) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingAdjuster(label: String, value: String, editable: Boolean, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            label,
            Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val decrease = stringResource(Res.string.dashboard_decrease, label)
        val increase = stringResource(Res.string.dashboard_increase, label)
        OutlinedIconButton(onClick = onMinus, enabled = editable, shape = MaterialTheme.shapes.small, modifier = Modifier.size(40.dp).semantics { contentDescription = decrease }) {
            Text("−", style = MaterialTheme.typography.titleSmall)
        }
        Text(
            value,
            Modifier.weight(1.1f),
            style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        OutlinedIconButton(onClick = onPlus, enabled = editable, shape = MaterialTheme.shapes.small, modifier = Modifier.size(40.dp).semantics { contentDescription = increase }) {
            Text("+", style = MaterialTheme.typography.titleSmall)
        }
    }
}

private fun liveRecipeChartModel(
    recipe: LiveRecipeDocument,
    liveSamples: List<LiveTestSample>,
    active: Boolean,
    shotEndSeconds: Double?,
): BrewChartModel {
    val measurements = liveSamples.associate { sample ->
        sample.elapsedMillis / 1_000f to ChartData(
            pressure = sample.pressureBar ?: 0f,
            weight = sample.weightGrams ?: 0f,
            weightPerSecond = 0f,
            volume = 0f,
            volumePerSecond = sample.machineFlowMlPerSecond ?: 0f,
        )
    }
    var elapsedMillis = 0L
    val targets = linkedMapOf<Float, ChartData>()
    val boundaries = mutableListOf<ChartPhaseBoundary>()
    recipe.phases.forEachIndexed { index, phase ->
        val atSeconds = elapsedMillis / 1_000f
        val fixedTarget = phase.target.takeIf { it.source == RecipeValueSource.Fixed }?.value ?: 0f
        targets[atSeconds] = when (phase.control) {
            RecipeControl.Pressure -> ChartData(fixedTarget, 0f, 0f, 0f, 0f)
            RecipeControl.MachineFlow -> ChartData(0f, 0f, 0f, 0f, fixedTarget)
            RecipeControl.CupFlow -> ChartData(0f, 0f, fixedTarget, 0f, 0f)
            RecipeControl.Pause -> ChartData(0f, 0f, 0f, 0f, 0f)
        }
        boundaries += ChartPhaseBoundary(atSeconds.toDouble(), index + 1, phase.name)
        elapsedMillis += phase.maximumDurationMillis
    }
    targets[elapsedMillis / 1_000f] = targets.values.lastOrNull() ?: ChartData(0f, 0f, 0f, 0f, 0f)
    val scaleMeasurements = liveSamples.mapNotNull { sample ->
        sample.weightGrams?.let {
            BrewScaleSample(
                elapsedMillis = sample.elapsedMillis,
                receivedAtEpochMillis = sample.scaleReceivedAt?.toEpochMilliseconds() ?: 0L,
                weightGrams = it,
                reportedFlowGramsPerSecond = sample.reportedCupFlowGramsPerSecond,
                derivedFlowGramsPerSecond = sample.derivedCupFlowGramsPerSecond,
                smoothedFlowGramsPerSecond = sample.derivedCupFlowGramsPerSecond,
                rawPacketHex = "",
                decoded = true,
            )
        }
    }
    return BrewChartModel(
        measurements = measurements,
        scaleMeasurements = scaleMeasurements,
        targets = targets,
        boundaries = boundaries,
        liveTimeline = active,
        fitToMeasurements = !active && hasElapsedChartMeasurements(measurements.keys),
        shotEndSeconds = shotEndSeconds,
    )
}

private fun Float.toOneDecimal() = ((kotlin.math.round(this * 10f) / 10f).let { if (it == 0f) 0f else it }).toString()

private const val LibraryPage = 0
private const val BrewPage = 1
private val PhoneStandClearance = 24.dp
