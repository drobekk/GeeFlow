package app.geeflow.presentation.feature.device.dashboard.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CoffeeMaker
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.AlarmClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.CleaningClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.ConnectionButtonClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.DeviceClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.LiveBrewTestClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.SettingsClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.TemperatureClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.UserClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Device
import app.geeflow.ui.components.GeeFlowUserAvatar
import app.geeflow.ui.components.HorizontalSpacer
import app.geeflow.ui.text.ClearText
import app.geeflow.ui.text.clearText
import app.geeflow.ui.theme.GeeFlowComponentPreview
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import geeflow.shared.core.ui.generated.resources.common_settings
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_alarm
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_clean
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_connected
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_connecting
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_disconnected
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_profile_label
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_syncing
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_brew_controls
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_connect_machine
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_disconnect_machine
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_profile_settings
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

@Composable
internal fun TopBar(
    device: Device,
    photoFileName: String? = null,
    profileName: String? = null,
    statusText: String? = null,
    scaleConnected: Boolean = false,
    scaleConnecting: Boolean = false,
    scaleReceiving: Boolean = false,
    onScaleToggle: () -> Unit = {},
    compactPhoneTemperatures: Boolean = false,
    onEvent: (DeviceDashboardEvent) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val avatarLabel = stringResource(Res.string.dashboard_profile_settings)
    BoxWithConstraints(modifier) {
        if (maxWidth < 600.dp) {
            val separateTemperatures = !compactPhoneTemperatures &&
                (maxWidth < 420.dp || LocalDensity.current.fontScale > 1.15f)
            val inlineConnections = maxWidth >= 520.dp
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    GeeFlowUserAvatar(
                        photoFileName = photoFileName,
                        modifier = Modifier.size(48.dp).semantics { contentDescription = avatarLabel },
                        onClick = { onEvent(UserClicked) },
                    )
                    if (compactPhoneTemperatures) {
                        CompactBoilerTemperatures(device, Modifier.weight(1f), onEvent = { onEvent(TemperatureClicked) })
                    } else if (!separateTemperatures) {
                        BoilerTemperatures(device, { onEvent(TemperatureClicked) }, Modifier.weight(1f), compact = true)
                    }
                    ConnectionStatusButton(device, onEvent)
                    if (inlineConnections) {
                        ScaleToggleButton(scaleConnected, scaleConnecting, scaleReceiving, onScaleToggle)
                    }
                    if (separateTemperatures) Spacer(Modifier.weight(1f))
                    IconButton(onClick = { onEvent(SettingsClicked) }) {
                        Icon(Icons.Filled.Settings, contentDescription = clearText(ClearText.Settings))
                    }
                    DashboardMenu(
                        actions = listOf(stringResource(Res.string.device_dashboard_clean) to CleaningClicked),
                        onEvent = onEvent,
                    )
                }
                if (!inlineConnections) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ScaleToggleButton(scaleConnected, scaleConnecting, scaleReceiving, onScaleToggle)
                    }
                }
                if (separateTemperatures) {
                    BoilerTemperatures(device, { onEvent(TemperatureClicked) }, Modifier.fillMaxWidth(), compact = true)
                }
            }
        } else {
            val compactActions = maxWidth < 950.dp
            Row(
                modifier = Modifier.heightIn(min = 72.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    GeeFlowUserAvatar(
                        photoFileName = photoFileName,
                        modifier = Modifier.size(48.dp).semantics { contentDescription = avatarLabel },
                        onClick = { onEvent(UserClicked) },
                    )
                    HorizontalSpacer(8.dp)
                    DeviceTile(device, onEvent, Modifier.weight(1f), profileName, statusText)
                }
                BoilerTemperatures(device, { onEvent(TemperatureClicked) }, Modifier.weight(1f))
                ActionBar(
                    scaleConnected = scaleConnected,
                    scaleConnecting = scaleConnecting,
                    scaleReceiving = scaleReceiving,
                    onScaleToggle = onScaleToggle,
                    onEvent = onEvent,
                    compact = compactActions,
                    modifier = if (compactActions) Modifier.wrapContentWidth() else Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
internal fun BrewMachineSidebar(
    device: Device,
    scaleConnected: Boolean,
    scaleConnecting: Boolean,
    scaleReceiving: Boolean,
    onScaleToggle: () -> Unit,
    onEvent: (DeviceDashboardEvent) -> Unit,
    modifier: Modifier = Modifier,
    displayOptions: @Composable ColumnScope.() -> Unit = {},
    controls: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .width(190.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            DeviceTile(device, onEvent, Modifier.weight(1f), sidebar = true)
            DashboardMenu(actions = emptyList(), onEvent = onEvent)
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            BoilerTemperature(
                label = clearText(ClearText.BrewBoiler),
                value = device.brewBoilerTemp,
                compact = true,
                modifier = Modifier.fillMaxWidth(),
                horizontalLayout = true,
            ) {
                onEvent(TemperatureClicked)
            }
            BoilerTemperature(
                label = clearText(ClearText.SteamBoiler),
                value = device.steamBoilerTemp,
                compact = true,
                modifier = Modifier.fillMaxWidth(),
                horizontalLayout = true,
            ) {
                onEvent(TemperatureClicked)
            }
        }
        HorizontalDivider()
        Text(
            stringResource(CoreRes.string.common_settings),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        ScaleToggleButton(scaleConnected, scaleConnecting, scaleReceiving, onScaleToggle, Modifier.fillMaxWidth())
        CleaningButton(onClick = { onEvent(CleaningClicked) }, modifier = Modifier.fillMaxWidth())
        SettingsButton(onClick = { onEvent(SettingsClicked) }, modifier = Modifier.fillMaxWidth())
        displayOptions()
        Spacer(Modifier.weight(1f))
        HorizontalDivider()
        Text(
            stringResource(Res.string.device_dashboard_brew_controls),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        controls()
    }
}

@Composable
private fun ActionBar(
    scaleConnected: Boolean,
    scaleConnecting: Boolean,
    scaleReceiving: Boolean,
    onScaleToggle: () -> Unit,
    onEvent: (DeviceDashboardEvent) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(48.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ScaleToggleButton(scaleConnected, scaleConnecting, scaleReceiving, onScaleToggle)
        if (compact) {
            HorizontalSpacer(6.dp)
            DashboardMenu(
                listOf(
                    stringResource(Res.string.device_dashboard_clean) to CleaningClicked,
                    stringResource(CoreRes.string.common_settings) to SettingsClicked,
                ),
                onEvent,
            )
        } else {
            HorizontalSpacer(6.dp)
            CleaningButton(onClick = { onEvent(CleaningClicked) })
            HorizontalSpacer(6.dp)
            SettingsButton(onClick = { onEvent(SettingsClicked) })
            HorizontalSpacer(6.dp)
            DashboardMenu(emptyList(), onEvent)
        }
    }
}

@Composable
private fun DashboardMenu(
    actions: List<Pair<String, DeviceDashboardEvent>>,
    onEvent: (DeviceDashboardEvent) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = clearText(ClearText.Menu))
        }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            (actions + ("Live test" to LiveBrewTestClicked)).forEach { (label, event) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        open = false
                        onEvent(event)
                    },
                )
            }
        }
    }
}

@Composable
private fun CleaningButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = DashboardActionButton(
    text = stringResource(Res.string.device_dashboard_clean),
    painter = rememberVectorPainter(Icons.Filled.CleaningServices),
    onClick = onClick,
    modifier = modifier,
)

@Composable
private fun ErrorButton(onClick: () -> Unit) {
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
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick, role = Role.Button)
            .background(MaterialTheme.colorScheme.errorContainer)
            .height(48.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = rememberVectorPainter(Icons.Filled.Error),
            contentDescription = stringResource(Res.string.device_dashboard_alarm),
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(20.dp).scale(scale),
        )
        Text(stringResource(Res.string.device_dashboard_alarm), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DashboardActionButton(
        text = stringResource(CoreRes.string.common_settings),
        painter = rememberVectorPainter(Icons.Filled.Settings),
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun DashboardActionButton(
    text: String,
    painter: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(enabled = enabled, onClick = onClick, role = Role.Button)
            .height(48.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(painter = painter, contentDescription = null, modifier = Modifier.size(20.dp), tint = contentColor)
        Text(text, style = MaterialTheme.typography.labelLarge, color = contentColor, maxLines = 1)
    }
}

@Composable
private fun ScaleToggleButton(
    connected: Boolean,
    connecting: Boolean,
    receiving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = DashboardActionButton(
    text = when {
        connecting -> "秤连接中"
        connected && receiving -> "秤有读数"
        connected -> "秤无读数"
        else -> "连接秤"
    },
    painter = rememberVectorPainter(
        when {
            connecting -> Icons.Filled.Bluetooth
            connected -> Icons.Filled.BluetoothConnected
            else -> Icons.Filled.BluetoothDisabled
        },
    ),
    onClick = onClick,
    modifier = modifier,
    enabled = !connecting,
    contentColor = if (connected && receiving) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
)

@Composable
private fun ConnectionStatusButton(device: Device, onEvent: (DeviceDashboardEvent) -> Unit) {
    val status = device.connectionStatus
    val label = stringResource(
        when (status) {
            Device.ConnectionStatus.Connected -> Res.string.device_dashboard_connected
            Device.ConnectionStatus.Disconnected -> Res.string.device_dashboard_disconnected
            Device.ConnectionStatus.Connecting -> Res.string.device_dashboard_connecting
            Device.ConnectionStatus.Synchronizing -> Res.string.device_dashboard_syncing
        },
    )
    val canToggle = status == Device.ConnectionStatus.Connected || status == Device.ConnectionStatus.Disconnected
    val actionLabel = stringResource(if (status == Device.ConnectionStatus.Connected) Res.string.dashboard_disconnect_machine else Res.string.dashboard_connect_machine)
    val statusColor = if (status == Device.ConnectionStatus.Connected) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(enabled = canToggle, onClickLabel = actionLabel, role = Role.Button) { onEvent(ConnectionButtonClicked) }
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(statusColor))
        Text(label, style = MaterialTheme.typography.labelMedium, color = statusColor, maxLines = 1)
    }
}

@Composable
internal fun BrewProfileSummary(
    profileName: String?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(Res.string.device_dashboard_profile_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
        Text(
            profileName?.takeIf { it.isNotBlank() } ?: "—",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun DeviceTile(
    device: Device,
    onEvent: (DeviceDashboardEvent) -> Unit,
    modifier: Modifier = Modifier,
    profileName: String? = null,
    statusText: String? = null,
    sidebar: Boolean = false,
) {
    val connectionLabel = stringResource(
        when (device.connectionStatus) {
            Device.ConnectionStatus.Connected -> Res.string.device_dashboard_connected
            Device.ConnectionStatus.Disconnected -> Res.string.device_dashboard_disconnected
            Device.ConnectionStatus.Connecting -> Res.string.device_dashboard_connecting
            Device.ConnectionStatus.Synchronizing -> Res.string.device_dashboard_syncing
        },
    )
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = { onEvent(DeviceClicked) })
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val displayName = if (device.isDemo && device.name in setOf("Demo", "LITA BA · Demo")) {
            clearText(ClearText.LitaBaDemo)
        } else {
            device.name
        }
        if (sidebar) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    displayName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            }
            ConnectionStatusButton(device, onEvent)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    displayName,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                Text(
                    connectionLabel,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable(
                            enabled = device.connectionStatus == Device.ConnectionStatus.Connected ||
                                device.connectionStatus == Device.ConnectionStatus.Disconnected,
                            role = Role.Button,
                        ) { onEvent(ConnectionButtonClicked) }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (device.connectionStatus == Device.ConnectionStatus.Connected) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                )
            }
        }
        if (device.alarm) {
            ErrorButton(onClick = { onEvent(AlarmClicked) })
        } else {
            val context = listOfNotNull(
                profileName?.takeIf { it.isNotBlank() },
                statusText?.takeIf { it.isNotBlank() && device.connectionStatus == Device.ConnectionStatus.Connected },
            ).joinToString(" · ")
            if (context.isNotBlank()) Text(
                context,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BoilerTemperatures(
    device: Device,
    onTemperatureClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BoilerTemperature(clearText(ClearText.BrewBoiler), device.brewBoilerTemp, compact, onClick = onTemperatureClick)
        HorizontalSpacer(if (compact) 4.dp else 28.dp)
        BoilerTemperature(clearText(ClearText.SteamBoiler), device.steamBoilerTemp, compact, onClick = onTemperatureClick)
    }
}

@Composable
private fun BoilerTemperature(
    label: String,
    value: String?,
    compact: Boolean,
    modifier: Modifier = Modifier,
    horizontalLayout: Boolean = false,
    onClick: () -> Unit,
) {
    val displayValue = value?.let { if (it.endsWith("°")) it else "$it°" } ?: "—"
    val containerModifier = modifier
        .clip(MaterialTheme.shapes.medium)
        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        .clickable(role = Role.Button, onClick = onClick)
        .heightIn(min = 48.dp)
        .padding(horizontal = 8.dp, vertical = 2.dp)
    if (horizontalLayout) {
        Row(
            modifier = containerModifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = displayValue,
                style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                fontSize = if (compact) 24.sp else 32.sp,
                lineHeight = if (compact) 28.sp else 36.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    } else {
        Column(
            modifier = containerModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = displayValue,
                style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                fontSize = if (compact) 24.sp else 32.sp,
                lineHeight = if (compact) 28.sp else 36.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun CompactBoilerTemperatures(
    device: Device,
    modifier: Modifier = Modifier,
    onEvent: () -> Unit,
) {
    val brewLabel = clearText(ClearText.BrewBoiler)
    val steamLabel = clearText(ClearText.SteamBoiler)
    val brewValue = device.brewBoilerTemp?.let { if (it.endsWith("°")) it else "$it°" } ?: "—"
    val steamValue = device.steamBoilerTemp?.let { if (it.endsWith("°")) it else "$it°" } ?: "—"
    Row(
        modifier = modifier.heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onEvent)
            .semantics { contentDescription = "$brewLabel $brewValue, $steamLabel $steamValue" },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$brewValue · $steamValue",
            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
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
