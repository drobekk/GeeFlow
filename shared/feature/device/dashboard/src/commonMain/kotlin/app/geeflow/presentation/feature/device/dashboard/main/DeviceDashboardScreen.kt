package app.geeflow.presentation.feature.device.dashboard.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.presentation.feature.device.dashboard.components.BrewBar
import app.geeflow.presentation.feature.device.dashboard.components.BrewButton
import app.geeflow.presentation.feature.device.dashboard.components.BrewCharts
import app.geeflow.presentation.feature.device.dashboard.components.TopBar
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileList
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModel
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModelEvent.SelectProfile
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModelEvent.ShowHistoryBrew
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModelEvent.ShowSnackbar
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState
import app.geeflow.ui.EventsDispatcher
import app.geeflow.ui.GeeFlowInsets
import app.geeflow.ui.components.HorizontalSpacer
import app.geeflow.ui.isWidthExpanded
import app.geeflow.ui.modifier.geeFlowInsetsPadding
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import app.geeflow.ui.theme.disabled
import kotlinx.coroutines.launch

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
    val pagerState = rememberPagerState { CompactDashboardPage.entries.size }

    LifecycleResumeEffect(viewModel) {
        viewModel.handleEvent(DeviceDashboardEvent.Resumed)
        onPauseOrDispose { }
    }

    DeviceDashboardContent(
        viewState = viewState,
        onEvent = viewModel::handleEvent,
        profileListViewState = profileListViewState,
        snackbarState = snackbarState,
        onProfileListEvent = profileListViewModel::handleEvent,
        pagerState = pagerState,
    )

    EventsDispatcher(profileListViewModel.events) {
        when (it) {
            is SelectProfile -> viewModel.handleEvent(DeviceDashboardEvent.ProfileSelected(it.id))
            is ShowHistoryBrew -> {
                viewModel.handleEvent(
                    DeviceDashboardEvent.HistoryBrewSelected(
                        name = it.name,
                        durationSeconds = it.durationSeconds,
                        data = it.data,
                        targetData = it.targetData,
                        phaseProgram = it.phaseProgram,
                        phaseTransitions = it.phaseTransitions,
                    ),
                )
                coroutineScope.launch { pagerState.animateScrollToPage(CompactDashboardPage.Details.ordinal) }
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
                pagerState.animateScrollToPage(CompactDashboardPage.Details.ordinal)
            }

            is DeviceDashboardViewModelEvent.ShowSnackbar -> coroutineScope.launch {
                snackbarState.currentSnackbarData?.dismiss()
                snackbarState.showSnackbar(it.message)
            }
        }
    }

    viewState.dialog?.let {
        DeviceDashboardDialog(
            model = it,
            onEvent = viewModel::handleEvent,
        )
    }
}

@OptIn(ExperimentalTextApi::class, ExperimentalLayoutApi::class)
@Composable
private fun DeviceDashboardContent(
    viewState: DeviceDashboardViewState,
    onEvent: (DeviceDashboardEvent) -> Unit = {},
    profileListViewState: ProfileListViewState,
    snackbarState: SnackbarHostState = SnackbarHostState(),
    onProfileListEvent: (ProfileListEvent) -> Unit,
    pagerState: androidx.compose.foundation.pager.PagerState,
) {
    if (isWidthExpanded()) {
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
        )
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
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .geeFlowInsetsPadding(),
    ) {
        Row(Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(MainColumnWeight).padding(start = 16.dp)) {
                TopBar(
                    device = viewState.device,
                    photoFileName = viewState.user.photoFileName,
                    onEvent = onEvent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 16.dp),
                )
                BrewCharts(
                    brew = viewState.brew,
                    targetData = viewState.targetData(profileListViewState),
                    program = if (viewState.brew.historyTarget != null) {
                        viewState.brew.phaseProgram
                    } else {
                        profileListViewState.profiles.find { it.selected }?.program
                    },
                    visibleCharts = viewState.visibleCharts,
                    modifier = Modifier.weight(MainColumnWeight),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                ) {
                    BrewBar(
                        brew = viewState.brew,
                        isBrewing = viewState.device.isBrewing,
                        visibleCharts = viewState.visibleCharts,
                        onToggle = { onEvent(DeviceDashboardEvent.ToggleChartVisibility(it)) },
                        onHeaderClick = { onEvent(DeviceDashboardEvent.BrewDescriptionClicked) },
                        modifier = Modifier.weight(1f).padding(bottom = 10.dp),
                    )
                    HorizontalSpacer(16.dp)
                    BrewButton(
                        state = viewState.brewButtonState,
                        onStopClick = { onEvent(DeviceDashboardEvent.StopBrewClicked) },
                        onStartButtonClick = { onEvent(DeviceDashboardEvent.ManualBrewClicked) },
                        onHeroButtonClick = { onEvent(DeviceDashboardEvent.BrewClicked) },
                        onEndButtonClick = { onEvent(DeviceDashboardEvent.FlowControlClicked) },
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(SideColumnWeight)
                    .fillMaxHeight()
                    .padding(16.dp),
            ) {
                ProfileList(
                    viewState = profileListViewState,
                    onEvent = onProfileListEvent,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                )
            }
        }
        SnackbarHost(
            hostState = snackbarState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun CompactDashboard(
    viewState: DeviceDashboardViewState,
    profileListViewState: ProfileListViewState,
    snackbarState: SnackbarHostState,
    onEvent: (DeviceDashboardEvent) -> Unit = {},
    onProfileListEvent: (ProfileListEvent) -> Unit,
    pagerState: androidx.compose.foundation.pager.PagerState,
) {
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        contentWindowInsets = GeeFlowInsets.content,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .geeFlowInsetsPadding(),
        topBar = {
            TopBar(
                device = viewState.device,
                photoFileName = viewState.user.photoFileName,
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 24.dp, end = 24.dp),
            )
        },
        bottomBar = {
            BrewButton(
                state = viewState.brewButtonState,
                onStopClick = { onEvent(DeviceDashboardEvent.StopBrewClicked) },
                onStartButtonClick = { onEvent(DeviceDashboardEvent.ManualBrewClicked) },
                onHeroButtonClick = { onEvent(DeviceDashboardEvent.BrewClicked) },
                onEndButtonClick = { onEvent(DeviceDashboardEvent.FlowControlClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 18.dp),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarState, modifier = Modifier.padding(horizontal = 16.dp)) },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 24.dp),
                onSelected = { index ->
                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                },
            )
            HorizontalPager(
                state = pagerState,
                key = { it },
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (page) {
                    CompactDashboardPage.Details.ordinal -> Column(modifier = Modifier.fillMaxSize()) {
                        BrewCharts(
                            brew = viewState.brew,
                            visibleCharts = viewState.visibleCharts,
                            targetData = viewState.targetData(profileListViewState),
                            program = if (viewState.brew.historyTarget != null) {
                                viewState.brew.phaseProgram
                            } else {
                                profileListViewState.profiles.find { it.selected }?.program
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .padding(start = 24.dp, end = 24.dp),
                        )
                        BrewBar(
                            brew = viewState.brew,
                            isBrewing = viewState.device.isBrewing,
                            visibleCharts = viewState.visibleCharts,
                            onToggle = { onEvent(DeviceDashboardEvent.ToggleChartVisibility(it)) },
                            onHeaderClick = { onEvent(DeviceDashboardEvent.BrewDescriptionClicked) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, top = 16.dp, end = 24.dp),
                        )
                    }

                    CompactDashboardPage.Profiles.ordinal -> ProfileList(
                        viewState = profileListViewState,
                        onEvent = onProfileListEvent,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                    )

                    else -> Unit
                }
            }
        }
    }
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
                CompactDashboardPage.Details -> "Details"
                CompactDashboardPage.Profiles -> "Profiles"
            },
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .clickable { onSelected(index) }
                .padding(vertical = 12.dp)
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

/**
 * A replayed history brew brings its own target curve — only fall back to the profile selected in
 * the list while live data is on screen.
 */
private fun DeviceDashboardViewState.targetData(profileListViewState: ProfileListViewState) =
    brew.historyTarget ?: profileListViewState.profiles.find { it.selected }?.targetData.orEmpty()

private enum class CompactDashboardPage {
    Details,
    Profiles
}

private const val MainColumnWeight = 0.7f
private const val SideColumnWeight = 0.3f

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    val state = remember { mutableStateOf(getMockDeviceDashboardViewState()) }
    val pagerState = rememberPagerState { CompactDashboardPage.entries.size }
    DeviceDashboardContent(
        viewState = state.value,
        profileListViewState = getMockProfileListViewState(),
        onProfileListEvent = {},
        pagerState = pagerState,
    )
}
