package app.geeflow.presentation.feature.device.dashboard.freecontrol

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.presentation.feature.device.dashboard.components.BrewButtonState
import app.geeflow.presentation.feature.device.dashboard.components.BrewCharts
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.BackClicked
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.ModeChanged
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.RenameClicked
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.RenameConfirmed
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.RenameDismissed
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.SaveClicked
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.StartClicked
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.StopClicked
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.TargetChanged
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlEvent.ToggleChartVisibility
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType
import app.geeflow.presentation.feature.device.dashboard.model.BrewChartModel
import app.geeflow.ui.EventsDispatcher
import app.geeflow.ui.components.GeeFlowOutlinedTextField
import app.geeflow.ui.components.HorizontalSpacer
import app.geeflow.ui.icons.Flow
import app.geeflow.ui.icons.Pressure
import app.geeflow.ui.modifier.geeFlowInsetsPadding
import app.geeflow.ui.text.ClearText
import app.geeflow.ui.text.clearText
import app.geeflow.ui.theme.ClearScreenTheme
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import app.geeflow.ui.theme.GeeFlowTheme
import geeflow.shared.core.ui.generated.resources.common_cancel
import geeflow.shared.core.ui.generated.resources.common_confirm
import geeflow.shared.core.ui.generated.resources.common_sec
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.free_control_default_profile_name
import geeflow.shared.feature.device.dashboard.generated.resources.free_control_rename_dialog_name
import geeflow.shared.feature.device.dashboard.generated.resources.free_control_rename_dialog_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

@Composable
internal fun FreeControlScreen(
    viewModel: FreeControlViewModel,
    navigator: Navigator,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val defaultProfileName = stringResource(Res.string.free_control_default_profile_name)

    LaunchedEffect(Unit) {
        viewModel.handleEvent(RenameConfirmed(defaultProfileName))
    }

    NavigatorEffect(navigator, viewModel.navEvent)

    EventsDispatcher(viewModel.events) {
        when (it) {
            is FreeControlViewModelEvent.ShowSnackbar -> coroutineScope.launch {
                snackbarState.currentSnackbarData?.dismiss()
                snackbarState.showSnackbar(it.message)
            }
        }
    }

    FreeControlContent(viewState = viewState, snackbarState = snackbarState, onEvent = viewModel::handleEvent)

    if (viewState.showRenameDialog) {
        RenameProfileDialog(
            currentName = viewState.profileName,
            onConfirm = { viewModel.handleEvent(RenameConfirmed(it)) },
            onDismiss = { viewModel.handleEvent(RenameDismissed) },
        )
    }
}

@Composable
private fun FreeControlContent(
    viewState: FreeControlViewState,
    snackbarState: SnackbarHostState,
    onEvent: (FreeControlEvent) -> Unit,
) {
    var chartDialog by rememberSaveable { mutableStateOf(false) }
    ClearScreenTheme {
        if (chartDialog) ManualChartsDialog(viewState, onEvent) { chartDialog = false }
        BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).geeFlowInsetsPadding()) {
            val phone = maxWidth < 600.dp
            var showCharts by rememberSaveable { mutableStateOf(false) }
            Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FreeControlTopBar(viewState, onEvent, onCharts = { chartDialog = true }, phone = phone)
                if (phone) {
                    ManualLiveReadings(viewState, compact = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(!showCharts, { showCharts = false }, label = { Text(clearText(ClearText.Control)) })
                        FilterChip(showCharts, { showCharts = true }, label = { Text(clearText(ClearText.ShotCharts)) })
                    }
                    if (showCharts) {
                        ManualShotCharts(viewState, onEvent, Modifier.weight(1f))
                        ManualBrewButton(viewState, onEvent)
                    } else {
                        ManualControlPanel(viewState, onEvent, Modifier.weight(1f))
                    }
                } else {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ManualLiveReadings(viewState)
                            ManualShotCharts(viewState, onEvent, Modifier.weight(1f))
                        }
                        ManualControlPanel(viewState, onEvent, Modifier.width(340.dp).fillMaxHeight())
                    }
                }
            }
            SnackbarHost(snackbarState, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun ManualLiveReadings(state: FreeControlViewState, compact: Boolean = false) {
    val last = state.brew.data.entries.maxByOrNull { it.key }?.value
    val active = state.brewButtonState == BrewButtonState.Brewing
    val seconds = state.brew.time.coerceAtLeast(0)
    val pressure = state.currentPressure?.toFloatOrNull()?.let { (it * TargetPrecision).roundToInt() / TargetPrecision }
    val flow = if (active) {
        last?.volumePerSecond?.let {
            (it * TargetPrecision).roundToInt() / TargetPrecision
        } ?: 0f
    } else {
        0f
    }
    val output = last?.weight?.let { (it * TargetPrecision).roundToInt() / TargetPrecision } ?: 0f
    val readings = listOf(
        clearText(ClearText.Pressure) to "${if (state.connected) pressure ?: "—" else "—"} bar",
        clearText(ClearText.WaterFlow) to "${if (state.connected) flow else "—"} ml/s",
        clearText(ClearText.Time) to "$seconds ${stringResource(CoreRes.string.common_sec)}",
        clearText(
            ClearText.Output
        ) to if (state.scaleConnected && state.connected) "$output g" else clearText(ClearText.NotAvailable),
    )
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            readings.forEachIndexed { index, (label, value) ->
                val color = when (index) {
                    0 -> GeeFlowTheme.colors.pressure
                    1 -> GeeFlowTheme.colors.water
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Column(
                    Modifier.weight(1f).clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surface).padding(8.dp)
                ) {
                    Text(
                        label,
                        style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            value.substringBefore(" "),
                            color = color,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = if (compact) 16.sp else 32.sp,
                            maxLines = 1,
                        )
                        val unit = value.substringAfter(" ", "")
                        if (unit.isNotBlank()) {
                            Text(
                                unit,
                                color = color,
                                fontSize = if (compact) 12.sp else 16.sp,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
        Text(
            if (!state.connected) {
                clearText(ClearText.DisconnectedControls)
            } else if (active) {
                clearText(ClearText.LiveRecording)
            } else if (state.sessionCompleted) {
                clearText(ClearText.LiveLastShot)
            } else {
                clearText(ClearText.ReadyRecord)
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ManualShotCharts(state: FreeControlViewState, onEvent: (FreeControlEvent) -> Unit, modifier: Modifier) {
    BrewCharts(
        model = BrewChartModel(measurements = state.brew.data),
        visibleCharts = state.visibleCharts,
        modifier = modifier,
    )
}

@Composable
private fun ManualControlPanel(
    state: FreeControlViewState,
    onEvent: (FreeControlEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pressure = state.mode == ControlMode.Pressure
    val color = if (pressure) GeeFlowTheme.colors.pressure else GeeFlowTheme.colors.water
    val value = if (pressure) state.pressureTarget else state.flowTarget
    val range = if (pressure) state.pressureRange else state.flowRange
    val unit = if (pressure) "bar" else "ml/s"
    var fine by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier.clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surface).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                ControlMethodButton(
                    clearText(ClearText.Pressure),
                    ControlMode.Pressure,
                    state,
                    GeeFlowTheme.colors.pressure,
                    onEvent
                )
            }
            if (state.flowControlSupported) {
                Box(Modifier.weight(1f)) {
                    ControlMethodButton(
                        clearText(ClearText.WaterFlow),
                        ControlMode.Flow,
                        state,
                        GeeFlowTheme.colors.water,
                        onEvent
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (pressure) clearText(ClearText.TargetPressure) else clearText(ClearText.TargetWaterFlow),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    "${(value * TargetPrecision).roundToInt() / TargetPrecision} $unit",
                    color = color,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            FilterChip(
                fine,
                { fine = !fine },
                label = { Text(if (fine) clearText(ClearText.FineQuarter) else clearText(ClearText.Fine)) }
            )
        }
        if (!pressure) {
            val rangeLabel = if (value > state.flowChartRange.maxMillilitersPerSecond) {
                clearText(ClearText.RangeFlag, state.flowChartRange.maxMillilitersPerSecond)
            } else {
                " "
            }
            Text(
                rangeLabel,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        ManualGesturePad(value, range, unit, fine, state.connected, color, {
            onEvent(TargetChanged(it))
        }, Modifier.weight(1f).fillMaxWidth())
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = {
                onEvent(TargetChanged(steppedManualTarget(value, -TargetStep, range)))
            }, enabled = state.connected, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Filled.Remove, contentDescription = clearText(ClearText.DecreaseTenth))
            }
            Text(
                clearText(ClearText.DragHold),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            OutlinedButton(onClick = {
                onEvent(TargetChanged(steppedManualTarget(value, TargetStep, range)))
            }, enabled = state.connected, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Filled.Add, contentDescription = clearText(ClearText.IncreaseTenth))
            }
        }
        ManualBrewButton(state, onEvent)
    }
}

@Composable
private fun ManualBrewButton(state: FreeControlViewState, onEvent: (FreeControlEvent) -> Unit) {
    val active = state.brewButtonState == BrewButtonState.Brewing
    Button(
        onClick = { onEvent(if (active) StopClicked else StartClicked) },
        enabled = state.connected && state.brewButtonState in setOf(BrewButtonState.Idle, BrewButtonState.Brewing),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
    ) {
        Icon(if (active) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null)
        HorizontalSpacer(8.dp)
        Text(
            when {
                active -> clearText(ClearText.StopBrew)
                state.brewButtonState == BrewButtonState.Syncing -> clearText(ClearText.Starting)
                else -> clearText(ClearText.StartBrew)
            }
        )
    }
}

@Composable
private fun ControlMethodButton(
    label: String,
    mode: ControlMode,
    state: FreeControlViewState,
    color: androidx.compose.ui.graphics.Color,
    onEvent: (FreeControlEvent) -> Unit,
) {
    val selected = state.mode == mode
    OutlinedButton(
        onClick = { onEvent(ModeChanged(mode)) },
        enabled = state.brewButtonState == BrewButtonState.Idle,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = color,
            containerColor = if (selected) color.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            if (selected) 1.5.dp else 0.5.dp,
            if (selected) color else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { this.selected = selected },
    ) {
        if (selected) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
        HorizontalSpacer(6.dp)
        Text(label)
    }
}

@Composable
private fun FreeControlTopBar(
    state: FreeControlViewState,
    onEvent: (FreeControlEvent) -> Unit,
    onCharts: () -> Unit,
    phone: Boolean,
) {
    Column {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onEvent(BackClicked) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Text(clearText(ClearText.Back))
            }
            Text(
                clearText(ClearText.ManualControl),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (!phone) {
                HorizontalSpacer(12.dp)
                Text(
                    if (state.deviceName in setOf(
                            "Demo",
                            "LITA BA · Demo"
                        )
                    ) {
                        clearText(ClearText.LitaBaDemo)
                    } else {
                        state.deviceName
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.weight(1f))
                ManualRecordingActions(state, onEvent, onCharts)
            }
        }
        if (phone) ManualRecordingActions(state, onEvent, onCharts)
    }
}

@Composable
private fun ManualRecordingActions(state: FreeControlViewState, onEvent: (FreeControlEvent) -> Unit, onCharts: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onCharts, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.Filled.Tune, contentDescription = null)
            Text(clearText(ClearText.Charts))
        }
        TextButton(onClick = { onEvent(RenameClicked) }, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.Filled.Edit, contentDescription = null)
            Text(clearText(ClearText.Name))
        }
        Button(
            onClick = { onEvent(SaveClicked) },
            enabled = state.sessionCompleted && state.brewButtonState == BrewButtonState.Idle,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Icon(Icons.Filled.Save, contentDescription = null)
            HorizontalSpacer(6.dp)
            Text(clearText(ClearText.SaveProfile))
        }
    }
}

@Composable
private fun ManualChartsDialog(state: FreeControlViewState, onEvent: (FreeControlEvent) -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(clearText(ClearText.VisibleCharts)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    DashboardChartType.Pressure to clearText(ClearText.PressureUnit),
                    DashboardChartType.FlowRate to clearText(ClearText.FlowUnit),
                    DashboardChartType.WeightRate to clearText(ClearText.CupFlowUnit),
                    DashboardChartType.Volume to clearText(ClearText.WaterVolumeUnit),
                    DashboardChartType.Weight to clearText(ClearText.CupWeightUnit),
                ).forEach { (chart, label) ->
                    FilterChip(
                        selected = chart in state.visibleCharts,
                        onClick = { onEvent(ToggleChartVisibility(chart)) },
                        label = { Text(label) },
                        leadingIcon = {
                            if (chart in state.visibleCharts) Icon(Icons.Filled.Check, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(clearText(ClearText.Done)) } },
    )
}

@Composable
private fun RenameProfileDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val nameState = rememberTextFieldState(initialText = currentName)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.free_control_rename_dialog_title)) },
        text = {
            GeeFlowOutlinedTextField(
                state = nameState,
                lineLimits = TextFieldLineLimits.SingleLine,
                label = { Text(stringResource(Res.string.free_control_rename_dialog_name)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(nameState.text.toString()) },
                enabled = nameState.text.isNotBlank(),
            ) {
                Text(stringResource(CoreRes.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(CoreRes.string.common_cancel))
            }
        },
    )
}

private const val TargetPrecision = 10f
private const val TargetStep = 0.1f

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    FreeControlContent(
        viewState = FreeControlViewState(
            deviceName = "GeeFlow Pro",
            profileName = "New freehand profile",
            mode = ControlMode.Pressure,
            pressureTarget = 6.0f,
            flowTarget = 3.5f,
            pressureRange = 0f..12f,
            flowRange = 0f..12f,
            brewButtonState = BrewButtonState.Idle,
            sessionCompleted = false,
            visibleCharts = setOf(
                DashboardChartType.Pressure,
                DashboardChartType.FlowRate,
                DashboardChartType.WeightRate,
            ),
        ),
        snackbarState = remember { SnackbarHostState() },
        onEvent = {},
    )
}
