package dev.drobek.geeflow.presentation.feature.device.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.drobek.geeflow.presentation.feature.device.dashboard.navigation.DeviceNavigation
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.UserClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Brew
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Profile
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.User
import dev.drobek.geeflow.presentation.feature.device.dashboard.components.ActionBar
import dev.drobek.geeflow.presentation.feature.device.dashboard.components.BrewBar
import dev.drobek.geeflow.presentation.feature.device.dashboard.components.DeviceTile
import dev.drobek.geeflow.ui.EventsDispatcher
import dev.drobek.geeflow.ui.HorizontalSpacer
import dev.drobek.geeflow.ui.components.GeeFlowUserAvatar
import dev.drobek.geeflow.ui.conditional
import dev.drobek.geeflow.ui.isExpanded
import dev.drobek.geeflow.ui.theme.GeeFlowScreenPreview
import dev.drobek.geeflow.ui.theme.GeeFlowTheme

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
        }
    }

    viewState.dialog?.let {
        DeviceDashboardDialog(
            model = it,
            onEvent = viewModel::handleEvent
        )
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun DeviceDashboardContent(
    viewState: DeviceDashboardViewState,
    onEvent: (DeviceDashboardEvent) -> Unit = {}
) {
    Scaffold(
        modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            TopBar(
                device = viewState.device,
                user = viewState.user,
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            )
        },
        bottomBar = { BottomBar(viewState.device, onEvent) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
        ) {

        }
    }
}

@Composable
private fun BottomBar(
    device: Device,
    onEvent: (DeviceDashboardEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    BrewBar(
        isBrewing = device.isBrewing,
        onStopClick = { onEvent(DeviceDashboardEvent.StopBrewClicked) },
        onManualClick = { onEvent(DeviceDashboardEvent.ManualBrewClicked) },
        onFlowClick = { onEvent(DeviceDashboardEvent.BrewClicked) },
        onManualFlowClick = { onEvent(DeviceDashboardEvent.FlowControlClicked) },
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .conditional(
                condition = isExpanded(),
                ifTrue = { wrapContentWidth() },
                ifFalse = { fillMaxWidth() }
            )
    )
}

@Composable
private fun TopBar(
    device: Device,
    user: User, // TODO Fill user profile image
    onEvent: (DeviceDashboardEvent) -> Unit = {},
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GeeFlowUserAvatar(
            modifier = Modifier.size(48.dp),
            onClick = { onEvent(UserClicked) }
        )
        HorizontalSpacer(8.dp)
        DeviceTile(device, onEvent)
        HorizontalSpacer(24.dp)
        ActionBar(
            connectionStatus = device.connectionStatus,
            onEvent = onEvent,
            modifier = Modifier.weight(1f, false)
        )
    }
}

private val mockViewState = DeviceDashboardViewState(
    user = User(
        id = "1",
        name = "John Doe"
    ),
    device = Device(
        id = "B0234556",
        name = "DATA-S",
        brewBoilerTemp = "93°",
        steamBoilerTemp = "125°",
        pressure = "0.9",
        connectionStatus = Device.ConnectionStatus.Connected
    ),
    brew = Brew(
        "Manual",
        data = mapOf(
            1 to Brew.Data(
                pressure = 0.9f,
                weight = 0f,
                weightPerSecond = 1f,
                volume = 2f,
                volumePerSecond = 3f
            ),
            30 to Brew.Data(
                pressure = 6.1f,
                weight = 5f,
                weightPerSecond = 0f,
                volume = 5f,
                volumePerSecond = 5f
            ),
        )
    ),
    brewProfiles = listOf(
        Profile(
            name = "Espresso Classic",
            description = "Traditional 1:2 ratio, 30s",
            brewByWeight = true
        ),
        Profile(
            name = "Morning Lungo",
            description = "High yield, smooth body",
            brewByWeight = false
        ),
        Profile(
            name = "Bloom & Flow",
            description = "Experimental pre-infusion",
            brewByWeight = true
        )
    )
)


@Composable
@GeeFlowScreenPreview
private fun PreviewLight() = GeeFlowTheme(false) {
    DeviceDashboardContent(mockViewState)
}

@Composable
@GeeFlowScreenPreview
private fun PreviewDark() = GeeFlowTheme(true) {
    DeviceDashboardContent(mockViewState)
}
