package app.geeflow.presentation.feature.device.dashboard.profileeditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.PhaseRamp
import app.geeflow.data.brew.model.RampStyle
import app.geeflow.ui.components.GeeFlowDialog
import app.geeflow.ui.components.GeeFlowDialogTopBar
import app.geeflow.ui.components.GeeFlowInputPad
import app.geeflow.ui.components.GeeFlowOutlinedTextField
import app.geeflow.ui.components.VerticalSpacer
import geeflow.shared.core.ui.generated.resources.common_confirm
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.step_editor_metric_boiler
import geeflow.shared.feature.device.dashboard.generated.resources.step_editor_metric_flow
import geeflow.shared.feature.device.dashboard.generated.resources.step_editor_metric_group
import geeflow.shared.feature.device.dashboard.generated.resources.step_editor_metric_pump
import geeflow.shared.feature.device.dashboard.generated.resources.step_editor_metric_water
import geeflow.shared.feature.device.dashboard.generated.resources.step_editor_metric_weight
import geeflow.shared.feature.device.dashboard.generated.resources.step_editor_name
import geeflow.shared.feature.device.dashboard.generated.resources.step_editor_ramp_style
import geeflow.shared.feature.device.dashboard.generated.resources.step_editor_rename
import geeflow.shared.feature.device.dashboard.generated.resources.step_editor_required_time
import geeflow.shared.feature.device.dashboard.generated.resources.step_editor_threshold_flow
import geeflow.shared.feature.device.dashboard.generated.resources.step_editor_threshold_pressure
import geeflow.shared.feature.device.dashboard.generated.resources.step_editor_threshold_time
import geeflow.shared.feature.device.dashboard.generated.resources.step_editor_threshold_volume
import geeflow.shared.feature.device.dashboard.generated.resources.step_editor_threshold_weight
import geeflow.shared.feature.device.dashboard.generated.resources.step_unit_flow
import geeflow.shared.feature.device.dashboard.generated.resources.step_unit_pressure
import geeflow.shared.feature.device.dashboard.generated.resources.step_unit_time
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

@Composable
internal fun EditorButton(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(12.dp),
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(end = 12.dp).size(20.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.labelSmall)
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
        trailing?.invoke()
    }
}

@Composable
internal fun EditorField(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    EditorButton(
        title = label,
        value = value,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
internal fun <T> EditorChoice(
    title: String,
    values: List<T>,
    selected: T,
    modifier: Modifier = Modifier,
    label: @Composable (T) -> String,
    icon: ((T) -> ImageVector)? = null,
    experimental: (T) -> Boolean = { false },
    onExplain: () -> Unit = {},
    onSelect: (T) -> Unit,
) {
    val isInteractive = values.size > 1
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        EditorButton(
            title = title,
            value = label(selected),
            onClick = { if (isInteractive) expanded = true },
            enabled = isInteractive,
            icon = icon?.invoke(selected),
            trailing = if (experimental(selected)) {
                { ExperimentButton(onClick = onExplain, modifier = Modifier.size(40.dp)) }
            } else {
                null
            },
        )
        if (isInteractive) {
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                values.forEach { value ->
                    DropdownMenuItem(
                        text = { Text(text = label(value)) },
                        onClick = {
                            onSelect(value)
                            expanded = false
                        },
                        trailingIcon = {
                            if (experimental(value)) ExperimentButton(onClick = onExplain)
                        },
                    )
                }
            }
        }
    }
}

internal val PressureAndFlowMetrics = setOf(
    BrewMetric.PumpPressure,
    BrewMetric.GroupPressure,
    BrewMetric.BoilerPressure,
    BrewMetric.PumpFlow,
)

@Composable
internal fun BrewMetric.thresholdLabel(): String = stringResource(
    when (this) {
        BrewMetric.PhaseTime -> Res.string.step_editor_threshold_time
        BrewMetric.PumpedVolume -> Res.string.step_editor_threshold_volume
        BrewMetric.CupWeight -> Res.string.step_editor_threshold_weight
        BrewMetric.PumpFlow -> Res.string.step_editor_threshold_flow
        else -> Res.string.step_editor_threshold_pressure
    },
)

internal val ConditionMetrics = listOf(
    BrewMetric.PhaseTime,
    BrewMetric.PumpedVolume,
    BrewMetric.CupWeight,
    BrewMetric.PumpPressure,
    BrewMetric.PumpFlow,
)

@Composable
internal fun RenameStepDialog(name: String, onEvent: (StepEditorEvent) -> Unit) {
    val text = rememberTextFieldState(name)
    val onConfirm = {
        if (text.text.isNotBlank()) {
            onEvent(StepEditorEvent.NameChanged(text.text.toString()))
        }
    }
    GeeFlowDialog(onDismissRequest = { onEvent(StepEditorEvent.RenameDismissed) }) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GeeFlowDialogTopBar(
                title = stringResource(Res.string.step_editor_rename),
                onCloseClick = { onEvent(StepEditorEvent.RenameDismissed) },
            )
            VerticalSpacer(16.dp)
            GeeFlowOutlinedTextField(
                state = text,
                lineLimits = TextFieldLineLimits.SingleLine,
                label = { Text(stringResource(Res.string.step_editor_name)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                onKeyboardAction = { onConfirm() },
                modifier = Modifier.fillMaxWidth(),
            )
            VerticalSpacer(24.dp)
            Button(
                onClick = onConfirm,
                enabled = text.text.isNotBlank(),
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(CoreRes.string.common_confirm))
            }
        }
    }
}

@Composable
internal fun BrewMetric.conditionLabel(): String = stringResource(
    when (this) {
        BrewMetric.PhaseTime -> Res.string.step_editor_required_time
        BrewMetric.PumpedVolume -> Res.string.step_editor_metric_water
        BrewMetric.CupWeight -> Res.string.step_editor_metric_weight
        BrewMetric.PumpFlow -> Res.string.step_editor_metric_flow
        BrewMetric.PumpPressure -> Res.string.step_editor_metric_pump
        BrewMetric.GroupPressure -> Res.string.step_editor_metric_group
        BrewMetric.BoilerPressure -> Res.string.step_editor_metric_boiler
    },
)

@Composable
internal fun StepInputDialog(state: StepEditorViewState, onEvent: (StepEditorEvent) -> Unit) {
    val label = when (val input = state.input) {
        StepInput.Target -> stringResource(
            if (state.type == StepType.Pressure) Res.string.step_unit_pressure else Res.string.step_unit_flow,
        )

        StepInput.RampDuration -> stringResource(Res.string.step_unit_time)
        is StepInput.Condition -> state.conditions.first { it.id == input.id }.metric.thresholdLabel()
        null -> return
    }
    Dialog(onDismissRequest = { onEvent(StepEditorEvent.InputDismissed) }) {
        Surface(shape = MaterialTheme.shapes.large) {
            GeeFlowInputPad(
                valueRange = state.inputRange(),
                unit = label,
                onConfirm = { onEvent(StepEditorEvent.InputConfirmed(it)) },
                onBack = { onEvent(StepEditorEvent.InputDismissed) },
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
internal fun RampSelector(
    state: StepEditorViewState,
    onEvent: (StepEditorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        RampDrawing(style = state.rampStyle, modifier = Modifier.size(16.dp))
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.step_editor_ramp_style),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = enumLabel(state.rampStyle),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (state.rampExperimental) {
                    ExperimentButton(onClick = { onEvent(StepEditorEvent.ExperimentClicked) })
                }
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            RampStyle.entries.forEach { style ->
                DropdownMenuItem(
                    text = { Text(text = enumLabel(style)) },
                    enabled = state.rampSupported(style),
                    leadingIcon = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                RampDrawing(style = style, modifier = Modifier.size(14.dp))
                            }
                        }
                    },
                    trailingIcon = {
                        if (style !in state.capabilities.native?.ramps.orEmpty()) {
                            ExperimentButton(onClick = { onEvent(StepEditorEvent.ExperimentClicked) })
                        }
                    },
                    onClick = {
                        onEvent(StepEditorEvent.RampChanged(style))
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
internal fun RampDrawing(style: RampStyle, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()
        val halfStroke = strokeWidth / 2f
        val w = size.width - strokeWidth
        val h = size.height - strokeWidth
        if (w <= 0f || h <= 0f) return@Canvas
        val path = Path()
        val ramp = PhaseRamp(style = style, durationMillis = CurveDurationMillis)
        path.moveTo(halfStroke, halfStroke + h)
        for (sample in 0..CurveSamples) {
            val fractionX = sample / CurveSamples.toFloat()
            val fractionY = ramp.fraction(sample * CurveDurationMillis / CurveSamples)
            path.lineTo(
                halfStroke + w * fractionX,
                halfStroke + h * (1f - fractionY),
            )
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

private const val CurveSamples = 100
private const val CurveDurationMillis = 1000L
