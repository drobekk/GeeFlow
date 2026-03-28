package dev.drobek.geeflow.presentation.feature.device.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.CleaningClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.ConnectedDevicesClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.ConnectionButtonClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.DeviceClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.SettingsClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.UserClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.User
import dev.drobek.geeflow.ui.HorizontalSpacer
import dev.drobek.geeflow.ui.components.GeeFlowUserAvatar
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TopBar(
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
        HorizontalSpacer(8.dp)
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
            modifier = Modifier.width(IntrinsicSize.Max)
        ) {
            ActionBarButton(
                painter = rememberVectorPainter(Icons.Filled.AutoAwesome),
                contentDescription = stringResource(Res.string.device_dashboard_clean),
                onClick = { onEvent(CleaningClicked) },
                modifier = Modifier.clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
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
                modifier = Modifier.clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
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
) = Box(
    modifier = modifier
        .clickable(onClick = onClick)
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .height(48.dp)
        .padding(horizontal = 16.dp),
    contentAlignment = Alignment.Center
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
        modifier = modifier.height(48.dp),
        onClick = { onEvent(ConnectionButtonClicked) },
        colors = ButtonDefaults.textButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(16.dp)
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

@Composable
internal fun DeviceTile(
    device: Device,
    onEvent: (DeviceDashboardEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
        Row(
            modifier = Modifier.animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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

    val textMeasurer = rememberTextMeasurer()
    val textWidth = LocalDensity.current.run {
        textMeasurer.measure(text = "100.0", style = MaterialTheme.typography.labelLarge).size.width.toDp()
    }

    Text(
        text = value ?: "  —",
        color = MaterialTheme.colorScheme.outline,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.widthIn(min = textWidth)
    )
}

@Composable
@GeeFlowScreenPreview
private fun PreviewLight() = GeeFlowTheme(false) {
    TopBar(
        device = Device(
            name = "Decent DE1",
            brewBoilerTemp = "93°C",
            steamBoilerTemp = "125°C",
            pressure = "9.0 bar",
            connectionStatus = Device.ConnectionStatus.Connected
        ),
        user = User(name = "Kamil"),
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
@GeeFlowScreenPreview
private fun PreviewDark() = GeeFlowTheme(true) {
    TopBar(
        device = Device(
            name = "Wendougee Data-S",
            brewBoilerTemp = "93°C",
            steamBoilerTemp = "125°C",
            pressure = "9.0 bar",
            connectionStatus = Device.ConnectionStatus.Connected
        ),
        user = User(name = "Chuck")
    )
}
