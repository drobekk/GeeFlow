package app.geeflow.presentation.feature.device.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState
import app.geeflow.ui.text.ClearText
import app.geeflow.ui.text.clearText

@Composable
internal fun FlushOverlay(state: DeviceDashboardViewState, onEvent: (DeviceDashboardEvent) -> Unit) {
    if (state.device.brewStatus != DeviceDashboardViewState.Device.BrewStatus.Manual) return
    val connected = state.device.connectionStatus == DeviceDashboardViewState.Device.ConnectionStatus.Connected
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier.fillMaxWidth(if (maxWidth >= 1100.dp) TabletWidth else 1f).padding(12.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(
                        clearText(ClearText.Flushing),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        clearText(ClearText.PressureTargetValue, state.flushPressure),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        clearText(ClearText.ElapsedSeconds, state.flushElapsedSeconds),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (state.flushDurationSeconds > 0) {
                        LinearProgressIndicator(
                            progress = {
                                (state.flushElapsedSeconds.toFloat() / state.flushDurationSeconds).coerceIn(0f, 1f)
                            },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                        )
                        Text(clearText(ClearText.FlushDuration, state.flushDurationSeconds))
                    } else {
                        Text(clearText(ClearText.UntimedFlush))
                    }
                    if (!connected) {
                        Text(
                            clearText(ClearText.DisconnectedFlush),
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(onClick = { onEvent(DeviceDashboardEvent.ConnectedDevicesClicked) }) {
                            Text(clearText(ClearText.OpenDevices))
                        }
                    }
                    Button(
                        onClick = { onEvent(DeviceDashboardEvent.StopBrewClicked) },
                        enabled = connected,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null)
                        Text(
                            clearText(ClearText.StopFlush),
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

private const val TabletWidth = 0.70f
