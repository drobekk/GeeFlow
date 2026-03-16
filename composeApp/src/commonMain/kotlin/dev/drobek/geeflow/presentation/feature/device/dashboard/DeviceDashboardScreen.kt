package dev.drobek.geeflow.presentation.feature.device.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.drobek.geeflow.presentation.feature.device.dashboard.CompactDashboardPage.Details
import dev.drobek.geeflow.presentation.feature.device.dashboard.CompactDashboardPage.Profiles
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.BrewClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.ConnectionButtonClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.FlowControlClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.ManualBrewClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.StopBrewClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.ToggleChartVisibility
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Brew
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device
import dev.drobek.geeflow.presentation.feature.device.dashboard.components.BrewBar
import dev.drobek.geeflow.presentation.feature.device.dashboard.components.BrewButton
import dev.drobek.geeflow.presentation.feature.device.dashboard.components.BrewCharts
import dev.drobek.geeflow.presentation.feature.device.dashboard.components.ProfileList
import dev.drobek.geeflow.presentation.feature.device.dashboard.components.TopBar
import dev.drobek.geeflow.presentation.feature.device.dashboard.navigation.DeviceNavigation
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
    navigation: DeviceNavigation
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    DeviceDashboardContent(
        viewState = viewState,
        onEvent = viewModel::handleEvent
    )

    EventsDispatcher(viewModel.events) {
        when (it) {
            is Navigation.Back -> navigation.back()
            is Navigation.DeviceList -> navigation.showDevicesList()
            is Navigation.Settings -> navigation.showQuickSettings(it.id)
            is Navigation.Clean -> navigation.showClean(it.id)
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
    onEvent: (DeviceDashboardEvent) -> Unit = {}
) {
    if (isWidthExpanded()) {
        ExpandedDashboard(
            viewState = viewState,
            onEvent = onEvent
        )
    } else {
        CompactDashboard(
            viewState = viewState,
            onEvent = onEvent
        )
    }
}

@Composable
private fun ExpandedDashboard(
    viewState: DeviceDashboardViewState,
    onEvent: (DeviceDashboardEvent) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.systemBarsPadding()) {
        Column(modifier = Modifier.weight(0.7f).padding(start = 16.dp)) {
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
                profiles = viewState.brewProfiles,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun CompactDashboard(
    viewState: DeviceDashboardViewState,
    onEvent: (DeviceDashboardEvent) -> Unit = {},
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
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 24.dp),
                onSelected = { index -> coroutineScope.launch { pagerState.animateScrollToPage(index) } }
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    Details.ordinal -> Column {
                        BrewCharts(
                            brew = viewState.brew,
                            visibleCharts = viewState.visibleCharts,
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .padding(start = 24.dp, end = 24.dp)
                        )
                        BrewBar(
                            brew = viewState.brew,
                            visibleCharts = viewState.visibleCharts,
                            onToggle = { onEvent(ToggleChartVisibility(it)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, top = 16.dp, end = 24.dp)
                        )
                    }

                    Profiles.ordinal -> ProfileList(
                        profiles = viewState.brewProfiles,
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
            onEvent = { event ->
                when (event) {
                    StopBrewClicked -> {
                        state.value = state.value.copy(
                            device = state.value.device.copy(brewStatus = Device.BrewStatus.Idle),
                            brew = state.value.brew.copy(data = emptyMap())
                        )
                    }

                    ManualBrewClicked -> {
                        state.value = getMockDeviceDashboardViewState()
                    }

                    ConnectionButtonClicked -> {
                        val currentData = state.value.brew.data
                        val nextTime = (currentData.keys.maxOrNull() ?: 0f) + 1f
                        val newDataPoint = Brew.Data(
                            pressure = 8f + (kotlin.math.sin(nextTime) * 0.5f),
                            weight = nextTime * 2f,
                            weightPerSecond = 2f,
                            volume = nextTime * 2.2f,
                            volumePerSecond = 2.2f
                        )
                        state.value = state.value.copy(
                            brew = state.value.brew.copy(
                                data = currentData + (nextTime to newDataPoint),
                                time = nextTime
                            )
                        )
                    }

                    is ToggleChartVisibility -> {
                        val current = state.value.visibleCharts
                        val new = if (current.contains(event.type)) {
                            current - event.type
                        } else {
                            current + event.type
                        }
                        state.value = state.value.copy(visibleCharts = new)
                    }

                    else -> Unit
                }
            }
        )
    }
}

@Composable
@GeeFlowScreenPreview
private fun PreviewLight() = DeviceDashboardPreview(false)

@Composable
@GeeFlowScreenPreview
private fun PreviewDark() = DeviceDashboardPreview(true)
