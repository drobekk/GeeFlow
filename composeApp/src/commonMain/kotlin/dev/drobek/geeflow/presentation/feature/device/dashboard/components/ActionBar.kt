package dev.drobek.geeflow.presentation.feature.device.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.CleaningClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.ConnectedDevicesClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.ConnectionButtonClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.SettingsClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device
import dev.drobek.geeflow.ui.HorizontalSpacer
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
internal fun ActionBar(
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
