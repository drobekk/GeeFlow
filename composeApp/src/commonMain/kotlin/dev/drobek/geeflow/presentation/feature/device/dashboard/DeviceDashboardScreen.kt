package dev.drobek.geeflow.presentation.feature.device.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.drobek.geeflow.presentation.feature.device.DeviceNavigation
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.CleaningClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.ConnectedDevicesClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.ConnectionButtonClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.DeviceClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.SettingsClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.UserClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Profile
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.User
import dev.drobek.geeflow.ui.EventsDispatcher
import dev.drobek.geeflow.ui.HorizontalSpacer
import dev.drobek.geeflow.ui.components.GeeFlowUserAvatar
import dev.drobek.geeflow.ui.icons.AppLogo
import dev.drobek.geeflow.ui.icons.GeeFlowIcon
import dev.drobek.geeflow.ui.icons.Pressure
import dev.drobek.geeflow.ui.icons.Steam
import dev.drobek.geeflow.ui.icons.Temperature
import dev.drobek.geeflow.ui.theme.GeeFlowScreenPreview
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import geeflow.composeapp.generated.resources.Res
import geeflow.composeapp.generated.resources.common_settings
import geeflow.composeapp.generated.resources.device_dashboard_clean
import geeflow.composeapp.generated.resources.device_dashboard_connected
import geeflow.composeapp.generated.resources.device_dashboard_connected_devices
import geeflow.composeapp.generated.resources.device_dashboard_connecting
import geeflow.composeapp.generated.resources.device_dashboard_disconnected
import geeflow.composeapp.generated.resources.device_list_screen_set_as_default
import org.jetbrains.compose.resources.stringResource

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
            Navigation.Back -> navigation.back()
            Navigation.DeviceList -> navigation.showDevicesList()
        }
    }

    viewState.dialog?.let {
        DeviceDashboardDialog(
            model = it,
            onEvent = viewModel::handleEvent
        )
    }
}

@Composable
private fun DeviceDashboardContent(
    viewState: DeviceDashboardViewState,
    onEvent: (DeviceDashboardEvent) -> Unit = {}
) {
    Scaffold(
        modifier = Modifier.statusBarsPadding(),
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
        floatingActionButton = {
            FloatingButton(onClick = { onEvent(DeviceDashboardEvent.BrewClicked) })
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
        ) {

        }
    }
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
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = { onEvent(DeviceClicked) })
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(modifier = Modifier.padding(start = 3.dp)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(
                    painter = rememberVectorPainter(Icons.Filled.ArrowDropDown),
                    tint = MaterialTheme.colorScheme.outline,
                    contentDescription = null
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ParameterItem(
                    value = device.brewBoilerTemp,
                    painter = rememberVectorPainter(GeeFlowIcon.Temperature),
                )
                ParameterItem(
                    value = device.steamBoilerTemp,
                    painter = rememberVectorPainter(GeeFlowIcon.Steam)
                )
                ParameterItem(
                    value = device.pressure,
                    painter = rememberVectorPainter(GeeFlowIcon.Pressure)
                )
            }
        }
        HorizontalSpacer(24.dp)
        ActionBar(
            connectionStatus = device.connectionStatus,
            onEvent = onEvent,
            modifier = Modifier.weight(1f, false)
        )
    }
}

@Composable
private fun ActionBar(
    connectionStatus: Device.ConnectionStatus,
    onEvent: (DeviceDashboardEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(Modifier.weight(1f)) {
            ConnectionStatusButton(
                connectionStatus = connectionStatus,
                onEvent = onEvent,
                modifier = Modifier.fillMaxHeight()
            )
        }
        HorizontalSpacer(16.dp)
        Row(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
        ) {
            ActionBarButton(
                painter = rememberVectorPainter(Icons.Filled.AutoAwesome),
                contentDescription = stringResource(Res.string.device_dashboard_clean),
                onClick = { onEvent(CleaningClicked) }
            )
            VerticalDivider(color = MaterialTheme.colorScheme.background)
            ActionBarButton(
                painter = rememberVectorPainter(Icons.Filled.DeviceHub),
                contentDescription = stringResource(Res.string.device_dashboard_connected_devices),
                onClick = { onEvent(ConnectedDevicesClicked) }
            )
            VerticalDivider(color = MaterialTheme.colorScheme.background)
            ActionBarButton(
                painter = rememberVectorPainter(Icons.Filled.Tune),
                contentDescription = stringResource(Res.string.common_settings),
                onClick = { onEvent(SettingsClicked) },
            )
        }
    }
}

@Composable
private fun ActionBarButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) = IconButton(
    onClick = onClick,
    modifier = modifier
) {
    Icon(
        painter = painter,
        modifier = Modifier.size(20.dp),
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ConnectionStatusButton(
    connectionStatus: Device.ConnectionStatus,
    onEvent: (DeviceDashboardEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        when (connectionStatus) {
            Device.ConnectionStatus.Disconnected -> MaterialTheme.colorScheme.errorContainer
            Device.ConnectionStatus.Connecting -> MaterialTheme.colorScheme.surfaceContainer
            Device.ConnectionStatus.Connected -> MaterialTheme.colorScheme.primary
        }
    )

    val text = when (connectionStatus) {
        Device.ConnectionStatus.Disconnected -> stringResource(Res.string.device_dashboard_disconnected)
        Device.ConnectionStatus.Connecting -> stringResource(Res.string.device_dashboard_connecting)
        Device.ConnectionStatus.Connected -> stringResource(Res.string.device_dashboard_connected)
    }

    val contentColor by animateColorAsState(
        when (connectionStatus) {
            Device.ConnectionStatus.Disconnected -> MaterialTheme.colorScheme.onErrorContainer
            Device.ConnectionStatus.Connecting -> MaterialTheme.colorScheme.onSurfaceVariant
            Device.ConnectionStatus.Connected -> MaterialTheme.colorScheme.onPrimary
        }
    )

    TextButton(
        modifier = modifier,
        onClick = { onEvent(ConnectionButtonClicked) },
        colors = ButtonDefaults.textButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (connectionStatus == Device.ConnectionStatus.Connecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = contentColor,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = rememberVectorPainter(
                    when (connectionStatus) {
                        Device.ConnectionStatus.Connected -> Icons.Filled.BluetoothConnected
                        Device.ConnectionStatus.Connecting -> Icons.Filled.BluetoothAudio
                        Device.ConnectionStatus.Disconnected -> Icons.Filled.BluetoothDisabled
                    }
                ),
                contentDescription = stringResource(Res.string.device_list_screen_set_as_default)
            )
        }
        Text(
            text = text,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) = MediumFloatingActionButton(
    onClick = onClick,
    shape = CircleShape,
    modifier = modifier
) {
    Icon(
        painter = rememberVectorPainter(GeeFlowIcon.AppLogo),
        contentDescription = null,
        modifier = Modifier.size(36.dp)
    )
}

@Composable
private fun ParameterItem(
    value: String?,
    painter: Painter,
    modifier: Modifier = Modifier
) = Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
) {
    Icon(
        painter = painter,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.outline
    )
    Text(
        text = value ?: "—",
        color = MaterialTheme.colorScheme.outline,
        style = MaterialTheme.typography.labelLarge
    )
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
