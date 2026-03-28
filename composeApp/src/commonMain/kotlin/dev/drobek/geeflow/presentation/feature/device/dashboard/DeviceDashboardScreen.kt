package dev.drobek.geeflow.presentation.feature.device.dashboard

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.drobek.geeflow.presentation.feature.device.dashboard.CompactDashboardPage.Details
import dev.drobek.geeflow.presentation.feature.device.dashboard.CompactDashboardPage.Profiles
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.BrewClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.FlowControlClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.ManualBrewClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.ProfileSelected
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.StopBrewClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.ToggleChartVisibility
import dev.drobek.geeflow.presentation.feature.device.dashboard.components.BrewBar
import dev.drobek.geeflow.presentation.feature.device.dashboard.components.BrewButton
import dev.drobek.geeflow.presentation.feature.device.dashboard.components.BrewCharts
import dev.drobek.geeflow.presentation.feature.device.dashboard.components.TopBar
import dev.drobek.geeflow.presentation.feature.device.dashboard.navigation.DeviceDashboardNavigation
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileList
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModel
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModelEvent.SelectProfile
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModelEvent.ShowSnackbar
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState
import dev.drobek.geeflow.ui.EventsDispatcher
import dev.drobek.geeflow.ui.HorizontalSpacer
import dev.drobek.geeflow.ui.isWidthExpanded
import dev.drobek.geeflow.ui.theme.GeeFlowScreenPreview
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import dev.drobek.geeflow.ui.theme.disabled
import kotlinx.coroutines.launch

@Composable
internal fun DeviceDashboardScreen(
    viewModel: DeviceDashboardViewModel,
    profileListViewModel: ProfileListViewModel,
    navigation: DeviceDashboardNavigation
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val snackbarState = remember { SnackbarHostState() }
    val profileListViewState by profileListViewModel.viewState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.handleEvent(DeviceDashboardEvent.Resumed)
        onPauseOrDispose { }
    }

    DeviceDashboardContent(
        viewState = viewState,
        onEvent = viewModel::handleEvent,
        profileListViewState = profileListViewState,
        snackbarState = snackbarState,
        onProfileListEvent = profileListViewModel::handleEvent
    )

    EventsDispatcher(profileListViewModel.events) {
        when (it) {
            is SelectProfile -> viewModel.handleEvent(ProfileSelected(it.id))
            is ShowSnackbar -> coroutineScope.launch { snackbarState.showSnackbar(it.message) }
        }
    }

    EventsDispatcher(viewModel.events) {
        when (it) {
            is Navigation.Back -> navigation.back()
            is Navigation.DeviceList -> navigation.showDevicesList()
            is Navigation.Settings -> navigation.showQuickSettings(it.id)
            is Navigation.Clean -> navigation.showClean(it.id)
            is DeviceDashboardViewModelEvent.ShowSnackbar -> coroutineScope.launch { snackbarState.showSnackbar(it.message) }
        }
    }

    viewState.dialog?.let {
        DeviceDashboardDialog(
            model = it,
            onEvent = viewModel::handleEvent
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
    onProfileListEvent: (ProfileListEvent) -> Unit
) {
    if (isWidthExpanded()) {
        ExpandedDashboard(
            viewState = viewState,
            profileListViewState = profileListViewState,
            snackbarState = snackbarState,
            onEvent = onEvent,
            onProfileListEvent = onProfileListEvent
        )
    } else {
        CompactDashboard(
            viewState = viewState,
            snackbarState = snackbarState,
            onEvent = onEvent,
            profileListViewState = profileListViewState,
            onProfileListEvent = onProfileListEvent
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
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.systemBarsPadding()) {
        Row(Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(0.7f).padding(start = 16.dp)) {
                val selectedProfile = profileListViewState.profiles.find { it.selected }
                TopBar(
                    device = viewState.device,
                    user = viewState.user,
                    onEvent = onEvent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 16.dp)
                )
                BrewCharts(
                    brew = viewState.brew,
                    selectedProfile = selectedProfile,
                    visibleCharts = viewState.visibleCharts,
                    modifier = Modifier.weight(0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                ) {
                    BrewBar(
                        brew = viewState.brew,
                        isBrewing = viewState.device.isBrewing,
                        visibleCharts = viewState.visibleCharts,
                        onToggle = { onEvent(ToggleChartVisibility(it)) },
                        modifier = Modifier.weight(1f).padding(bottom = 10.dp)
                    )
                    HorizontalSpacer(16.dp)
                    BrewButton(
                        isBrewing = viewState.device.isBrewing,
                        onStopClick = { onEvent(StopBrewClicked) },
                        onManualClick = { onEvent(ManualBrewClicked) },
                        onFlowClick = { onEvent(BrewClicked) },
                        onManualFlowClick = { onEvent(FlowControlClicked) }
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                ProfileList(
                    viewState = profileListViewState,
                    onEvent = onProfileListEvent,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            }
        }
        SnackbarHost(
            hostState = snackbarState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
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
) {
    val pagerState = rememberPagerState { CompactDashboardPage.entries.size }
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            TopBar(
                device = viewState.device,
                user = viewState.user,
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 24.dp, end = 24.dp)
            )
        },
        bottomBar = {
            BrewButton(
                isBrewing = viewState.device.isBrewing,
                onStopClick = { onEvent(StopBrewClicked) },
                onManualClick = { onEvent(ManualBrewClicked) },
                onFlowClick = { onEvent(BrewClicked) },
                onManualFlowClick = { onEvent(FlowControlClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 18.dp)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarState, modifier = Modifier.padding(horizontal = 16.dp)) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 24.dp),
                onSelected = { index ->
                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                }
            )
            HorizontalPager(
                state = pagerState,
                key = { it },
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    Details.ordinal -> Column(modifier = Modifier.fillMaxSize()) {
                        val selectedProfile = profileListViewState.profiles.find { it.selected }
                        BrewCharts(
                            brew = viewState.brew,
                            visibleCharts = viewState.visibleCharts,
                            selectedProfile = selectedProfile,
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .padding(start = 24.dp, end = 24.dp)
                        )
                        BrewBar(
                            brew = viewState.brew,
                            isBrewing = viewState.device.isBrewing,
                            visibleCharts = viewState.visibleCharts,
                            onToggle = { onEvent(ToggleChartVisibility(it)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, top = 16.dp, end = 24.dp)
                        )
                    }

                    Profiles.ordinal -> ProfileList(
                        viewState = profileListViewState,
                        onEvent = onProfileListEvent,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
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
    onSelected: (Int) -> Unit = {}
) = Row(modifier = modifier) {
    CompactDashboardPage.entries.forEachIndexed { index, page ->
        val color by animateColorAsState(
            targetValue = if (index == selectedTabIndex) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.primary.disabled()
            }
        )
        Text(
            text = when (page) {
                Details -> "Details"
                Profiles -> "Profiles"
            },
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable { onSelected(index) }
                .padding(vertical = 12.dp)
                .fillMaxWidth()
                .weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = color
        )
    }
}

private enum class CompactDashboardPage {
    Details, Profiles
}

@Composable
private fun DeviceDashboardPreview(isDark: Boolean) {
    val state = remember { mutableStateOf(getMockDeviceDashboardViewState()) }
    GeeFlowTheme(isDark) {
        DeviceDashboardContent(
            viewState = state.value,
            profileListViewState = getMockProfileListViewState(),
            onProfileListEvent = {},
        )
    }
}

@Composable
@GeeFlowScreenPreview
private fun PreviewLight() = DeviceDashboardPreview(false)

@Composable
@GeeFlowScreenPreview
private fun PreviewDark() = DeviceDashboardPreview(true)
