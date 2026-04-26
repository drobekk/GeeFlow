package app.geeflow.presentation.feature.device.dashboard.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Error
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.AlarmClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.ConnectedDevicesClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.ConnectionButtonClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.DeviceClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.QuickSettingsClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.UserClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Device
import app.geeflow.ui.components.GeeFlowUserAvatar
import app.geeflow.ui.components.HorizontalSpacer
import app.geeflow.ui.icons.DeviceHub
import app.geeflow.ui.icons.GeeFlowIcon
import app.geeflow.ui.icons.Pressure
import app.geeflow.ui.icons.Steam
import app.geeflow.ui.icons.Temperature
import app.geeflow.ui.theme.GeeFlowComponentPreview
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import geeflow.core.ui.generated.resources.common_settings
import geeflow.feature.device.dashboard.generated.resources.Res
import geeflow.feature.device.dashboard.generated.resources.device_dashboard_alarm
import geeflow.feature.device.dashboard.generated.resources.device_dashboard_clean
import geeflow.feature.device.dashboard.generated.resources.device_dashboard_connected
import geeflow.feature.device.dashboard.generated.resources.device_dashboard_connected_devices
import geeflow.feature.device.dashboard.generated.resources.device_dashboard_connecting
import geeflow.feature.device.dashboard.generated.resources.device_dashboard_disconnected
import geeflow.feature.device.dashboard.generated.resources.device_dashboard_synchronizing
import org.jetbrains.compose.resources.stringResource
import geeflow.core.ui.generated.resources.Res as CoreRes

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TopBar(
    device: Device,
    onEvent: (DeviceDashboardEvent) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GeeFlowUserAvatar(
            modifier = Modifier.size(48.dp),
            onClick = { onEvent(UserClicked) },
        )
        HorizontalSpacer(8.dp)
        DeviceTile(device, onEvent)
        HorizontalSpacer(8.dp)
        ActionBar(
            device = device,
            onEvent = onEvent,
            modifier = Modifier.weight(1f, false),
        )
    }
}

@Composable
private fun ActionBar(
    device: Device,
    onEvent: (DeviceDashboardEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(Modifier.weight(1f)) {
            ConnectionStatusButton(
                connectionStatus = device.connectionStatus,
                onEvent = onEvent,
                modifier = Modifier.fillMaxHeight(),
            )
        }
        HorizontalSpacer(16.dp)
        Row(
            modifier = Modifier.width(IntrinsicSize.Max),
        ) {
            AlarmButton(
                visible = device.alarm,
                onClick = { onEvent(AlarmClicked) },
                modifier = Modifier.clip(
                    MaterialTheme.shapes.large.copy(
                        topEnd = CornerSize(0.dp),
                        bottomEnd = CornerSize(0.dp),
                    ),
                ),
            )
            VerticalDivider(color = MaterialTheme.colorScheme.background)
            ConnectivityButton(
                smartScaleConnected = device.smartScaleConnected,
                onClick = { onEvent(ConnectedDevicesClicked) },
            )
            VerticalDivider(color = MaterialTheme.colorScheme.background)
            SettingsButton(
                onClick = { onEvent(QuickSettingsClicked) },
                modifier = Modifier.clip(
                    MaterialTheme.shapes.large.copy(
                        topStart = CornerSize(0.dp),
                        bottomStart = CornerSize(0.dp),
                    ),
                ),
            )
        }
    }
}

@Composable
private fun AlarmButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Box(
    modifier = modifier
        .clickable(onClick = onClick)
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .height(48.dp)
        .padding(horizontal = 16.dp),
    contentAlignment = Alignment.Center,
) {
    AnimatedContent(targetState = visible) {
        if (it) {
            val animation = rememberInfiniteTransition("warningSizeAnimation")
            val scale by animation.animateFloat(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(WarningAnimationMs),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "scale",
            )
            Icon(
                painter = rememberVectorPainter(Icons.Filled.Error),
                contentDescription = stringResource(Res.string.device_dashboard_alarm),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp).scale(scale),
            )
        } else {
            Icon(
                painter = rememberVectorPainter(Icons.Filled.AutoAwesome),
                contentDescription = stringResource(Res.string.device_dashboard_clean),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Box(
    modifier = modifier
        .clickable(onClick = onClick)
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .height(48.dp)
        .padding(horizontal = 16.dp),
    contentAlignment = Alignment.Center,
) {
    Icon(
        painter = rememberVectorPainter(Icons.Filled.Tune),
        contentDescription = stringResource(CoreRes.string.common_settings),
        modifier = Modifier.size(20.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ConnectivityButton(
    smartScaleConnected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Box(
    modifier = modifier
        .clickable(onClick = onClick)
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .height(48.dp)
        .padding(horizontal = 16.dp),
    contentAlignment = Alignment.Center,
) {
    if (smartScaleConnected) {
        Box(
            modifier = Modifier
                .padding(bottom = 10.dp)
                .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                .size(4.dp),
        )
    }
    Icon(
        painter = rememberVectorPainter(GeeFlowIcon.DeviceHub),
        modifier = Modifier.size(20.dp),
        contentDescription = stringResource(Res.string.device_dashboard_connected_devices),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ConnectionStatusButton(
    connectionStatus: Device.ConnectionStatus,
    onEvent: (DeviceDashboardEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        when (connectionStatus) {
            Device.ConnectionStatus.Disconnected -> MaterialTheme.colorScheme.errorContainer
            Device.ConnectionStatus.Connecting -> MaterialTheme.colorScheme.surfaceContainer
            Device.ConnectionStatus.Synchronizing -> MaterialTheme.colorScheme.secondaryContainer
            Device.ConnectionStatus.Connected -> MaterialTheme.colorScheme.primary
        },
    )

    val text = when (connectionStatus) {
        Device.ConnectionStatus.Disconnected -> stringResource(Res.string.device_dashboard_disconnected)
        Device.ConnectionStatus.Connecting -> stringResource(Res.string.device_dashboard_connecting)
        Device.ConnectionStatus.Synchronizing -> stringResource(Res.string.device_dashboard_synchronizing)
        Device.ConnectionStatus.Connected -> stringResource(Res.string.device_dashboard_connected)
    }

    val contentColor by animateColorAsState(
        when (connectionStatus) {
            Device.ConnectionStatus.Disconnected -> MaterialTheme.colorScheme.onErrorContainer
            Device.ConnectionStatus.Connecting -> MaterialTheme.colorScheme.onSurfaceVariant
            Device.ConnectionStatus.Synchronizing -> MaterialTheme.colorScheme.onSecondaryContainer
            Device.ConnectionStatus.Connected -> MaterialTheme.colorScheme.onPrimary
        },
    )

    TextButton(
        modifier = modifier.height(48.dp),
        onClick = { onEvent(ConnectionButtonClicked) },
        colors = ButtonDefaults.textButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        if (connectionStatus == Device.ConnectionStatus.Connecting || connectionStatus == Device.ConnectionStatus.Synchronizing) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = contentColor,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = rememberVectorPainter(
                    when (connectionStatus) {
                        Device.ConnectionStatus.Connected -> Icons.Filled.BluetoothConnected
                        Device.ConnectionStatus.Connecting -> Icons.Filled.BluetoothAudio
                        Device.ConnectionStatus.Synchronizing -> Icons.Filled.BluetoothAudio
                        Device.ConnectionStatus.Disconnected -> Icons.Filled.BluetoothDisabled
                    },
                ),
                contentDescription = null,
            )
        }
        Text(
            text = text,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
internal fun DeviceTile(
    device: Device,
    onEvent: (DeviceDashboardEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = { onEvent(DeviceClicked) })
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(modifier = Modifier.padding(start = 3.dp)) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Icon(
                painter = rememberVectorPainter(Icons.Filled.ArrowDropDown),
                tint = MaterialTheme.colorScheme.outline,
                contentDescription = null,
            )
        }
        Row(
            modifier = Modifier.animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ParameterItem(
                value = device.brewBoilerTemp,
                painter = rememberVectorPainter(GeeFlowIcon.Temperature),
            )
            ParameterItem(
                value = device.steamBoilerTemp,
                painter = rememberVectorPainter(GeeFlowIcon.Steam),
            )
            ParameterItem(
                value = device.pressure,
                painter = rememberVectorPainter(GeeFlowIcon.Pressure),
            )
        }
    }
}

@Composable
private fun ParameterItem(
    value: String?,
    painter: Painter,
    modifier: Modifier = Modifier,
) = Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
) {
    Icon(
        painter = painter,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.outline,
    )

    val textMeasurer = rememberTextMeasurer()
    val textWidth = LocalDensity.current.run {
        textMeasurer.measure(text = "100.0", style = MaterialTheme.typography.labelLarge).size.width.toDp()
    }

    Text(
        text = value ?: "  —",
        color = MaterialTheme.colorScheme.outline,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.widthIn(min = textWidth),
    )
}

private const val WarningAnimationMs = 500

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowComponentPreview
private fun Preview() {
    var alarmOn by remember { mutableStateOf(false) }
    TopBar(
        device = Device(
            name = "Wendougee Data-S",
            brewBoilerTemp = "93°",
            steamBoilerTemp = "125°",
            pressure = "9.0",
            connectionStatus = Device.ConnectionStatus.Connected,
            smartScaleConnected = true,
            alarm = alarmOn,
        ),
        modifier = Modifier.padding(16.dp),
        onEvent = {
            alarmOn = !alarmOn
        },
    )
}
