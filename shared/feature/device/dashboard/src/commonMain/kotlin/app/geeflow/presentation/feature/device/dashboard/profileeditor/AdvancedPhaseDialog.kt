package app.geeflow.presentation.feature.device.dashboard.profileeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.ExitCondition
import app.geeflow.data.brew.model.PhaseRamp
import app.geeflow.data.brew.model.RampStart
import app.geeflow.data.brew.model.RampStyle
import app.geeflow.data.brew.model.ThresholdComparison
import app.geeflow.data.device.model.ProfileIssueCode
import app.geeflow.ui.components.GeeFlowDialog
import app.geeflow.ui.components.GeeFlowOutlinedTextField
import app.geeflow.ui.icons.Experiment
import app.geeflow.ui.icons.GeeFlowIcon
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_above
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_add_condition
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_app_control
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_below
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_boiler_pressure
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_comparison
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_condition_summary
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_control
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_copy
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_current_measurement
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_ease_in
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_ease_in_out
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_ease_out
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_flow
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_group_pressure
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_instant
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_invalid
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_linear
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_measurement
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_minimum_time
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_mode_switch
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_phase_limit
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_phase_name
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_phase_transition
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_preview
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_previous_target
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_pump_pressure
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_ramp
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_ramp_duration
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_ramp_summary
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_remove_condition
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_save_transition
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_selection
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_sensor
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_start_from
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_title
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_transition
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_unsupported
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_water
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_weight
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AdvancedPhaseDialog(step: ProfileEditorViewState.Step, onEvent: (ProfileEditorEvent) -> Unit) {
    var rampStyle by remember { mutableStateOf(step.ramp.style) }
    var rampStart by remember { mutableStateOf(step.ramp.start) }
    val duration = rememberTextFieldState((step.ramp.durationMillis / MillisecondsPerSecond.toFloat()).toString())
    val minimum = rememberTextFieldState((step.minimumDurationMillis / MillisecondsPerSecond.toFloat()).toString())
    val name = rememberTextFieldState(step.phaseName)
    var conditions by remember { mutableStateOf(step.exitConditions.mapIndexed { i, condition -> i to condition }) }
    var nextConditionId by remember { mutableStateOf(conditions.size) }
    GeeFlowDialog(onDismissRequest = { onEvent(ProfileEditorEvent.DialogDismissed) }) {
        Column(
            Modifier.widthIn(max = 480.dp).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(Res.string.experimental_phase_transition), style = MaterialTheme.typography.titleLarge)
            GeeFlowOutlinedTextField(state = name, label = { Text(stringResource(Res.string.experimental_phase_name)) })
            Text(stringResource(Res.string.experimental_phase_limit, step.timeSec))
            EnumSelector(
                stringResource(Res.string.experimental_ramp),
                if (step.type == StepType.Wait) listOf(RampStyle.Instant) else RampStyle.entries,
                rampStyle
            ) { rampStyle = it }
            if (rampStyle != RampStyle.Instant) {
                EnumSelector(stringResource(Res.string.experimental_start_from), RampStart.entries, rampStart) {
                    rampStart = it
                }
                GeeFlowOutlinedTextField(
                    state = duration,
                    label = { Text(stringResource(Res.string.experimental_ramp_duration)) }
                )
            }
            GeeFlowOutlinedTextField(state = minimum, label = {
                Text(stringResource(Res.string.experimental_minimum_time))
            })
            conditions.forEach { (id, condition) ->
                key(id) {
                    ConditionRow(condition, onChange = { changed ->
                        conditions = conditions.map { old -> if (old.first == id) id to changed else old }
                    }, onRemove = { conditions = conditions.filterNot { it.first == id } })
                }
            }
            TextButton(
                onClick = {
                    conditions = conditions + (
                        nextConditionId++ to ExitCondition(
                        BrewMetric.PumpedVolume,
                        ThresholdComparison.Above,
                        DefaultPhaseWaterMl
                    )
                    )
                }
            ) {
                Text(stringResource(Res.string.experimental_add_condition))
            }
            val rampMs = duration.text.toString().toFloatOrNull()?.takeIf {
                it.isFinite()
            }?.let { (it * MillisecondsPerSecond).toLong() }
            val minMs = minimum.text.toString().toFloatOrNull()?.takeIf {
                it.isFinite()
            }?.let { (it * MillisecondsPerSecond).toLong() }
            val valid = minMs != null && minMs in 0..step.timeSec * MillisecondsPerSecond &&
                (rampStyle == RampStyle.Instant || rampMs != null && rampMs in 1..step.timeSec * MillisecondsPerSecond) &&
                conditions.all { it.second.threshold.isFinite() && it.second.threshold >= 0 }
            Button(enabled = valid, onClick = {
                onEvent(
                    ProfileEditorEvent.AdvancedConfirmed(
                        step.copy(
                            phaseName = name.text.toString(),
                            ramp = PhaseRamp(
                                rampStyle,
                                if (rampStyle == RampStyle.Instant) 0 else requireNotNull(rampMs),
                                rampStart
                            ),
                            minimumDurationMillis = requireNotNull(minMs),
                            exitConditions = conditions.map { it.second },
                        )
                    )
                )
            }) { Text(stringResource(Res.string.experimental_save_transition)) }
        }
    }
}

@Composable
private fun ConditionRow(condition: ExitCondition, onChange: (ExitCondition) -> Unit, onRemove: () -> Unit) {
    val threshold = rememberTextFieldState(condition.threshold.toString())
    LaunchedEffect(threshold.text) {
        onChange(condition.copy(threshold = threshold.text.toString().toFloatOrNull() ?: Float.NaN))
    }
    Column {
        EnumSelector(
            stringResource(Res.string.experimental_measurement),
            listOf(
                BrewMetric.PumpedVolume,
                BrewMetric.CupWeight,
                BrewMetric.PumpPressure,
                BrewMetric.PumpFlow
            ),
            condition.metric
        ) { onChange(condition.copy(metric = it)) }
        EnumSelector(
            stringResource(Res.string.experimental_comparison),
            ThresholdComparison.entries,
            condition.comparison
        ) { onChange(condition.copy(comparison = it)) }
        GeeFlowOutlinedTextField(state = threshold, label = { Text(condition.metric.unitLabel()) })
        TextButton(onClick = onRemove) { Text(stringResource(Res.string.experimental_remove_condition)) }
    }
}

@Composable
internal fun BrewMetric.unitLabel(): String = when (this) {
    BrewMetric.PumpedVolume -> stringResource(Res.string.experimental_water)
    BrewMetric.CupWeight -> stringResource(Res.string.experimental_weight)
    BrewMetric.PumpFlow -> stringResource(Res.string.experimental_flow)
    BrewMetric.PumpPressure -> stringResource(Res.string.experimental_pump_pressure)
    BrewMetric.GroupPressure -> stringResource(Res.string.experimental_group_pressure)
    BrewMetric.BoilerPressure -> stringResource(Res.string.experimental_boiler_pressure)
}

@Composable
private fun <T> EnumSelector(label: String, values: List<T>, selected: T, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true }
        ) { Text(stringResource(Res.string.experimental_selection, label, enumLabel(selected))) }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            values.forEach { value ->
                DropdownMenuItem(
                    text = { Text(enumLabel(value)) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
internal fun ExperimentButton(onClick: () -> Unit) {
    IconButton(onClick) {
        Icon(GeeFlowIcon.Experiment, contentDescription = stringResource(Res.string.experimental_title))
    }
}

@Composable
internal fun PhaseDetails(step: ProfileEditorViewState.Step, onEvent: (ProfileEditorEvent) -> Unit) {
    Column {
        Row {
            if (step.experimental) ExperimentButton { onEvent(ProfileEditorEvent.ExperimentClicked) }
            TextButton(
                onClick = { onEvent(ProfileEditorEvent.AdvancedClicked(step.id)) }
            ) { Text(stringResource(Res.string.experimental_transition)) }
            TextButton(
                onClick = { onEvent(ProfileEditorEvent.StepDuplicated(step.id)) }
            ) { Text(stringResource(Res.string.experimental_copy)) }
        }
        if (step.ramp.style != RampStyle.Instant) {
            Text(
                stringResource(
                    Res.string.experimental_ramp_summary,
                    enumLabel(step.ramp.style),
                    (step.ramp.durationMillis / MillisecondsPerSecond.toFloat()).toString()
                )
            )
        }
        step.exitConditions.forEach {
            Text(
                stringResource(
                    Res.string.experimental_condition_summary,
                    it.metric.unitLabel(),
                    enumLabel(it.comparison),
                    it.threshold.toString()
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
internal fun ExperimentalNotice(state: ProfileEditorViewState, onEvent: (ProfileEditorEvent) -> Unit) {
    if (state.experimental) {
        Row {
            ExperimentButton { onEvent(ProfileEditorEvent.ExperimentClicked) }
            Text(stringResource(Res.string.experimental_app_control), modifier = Modifier.padding(top = 12.dp))
        }
    }
    if (state.steps.any { it.exitConditions.isNotEmpty() || it.ramp.style != RampStyle.Instant }) {
        Text(
            stringResource(Res.string.experimental_preview),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
    state.supportIssues.forEach { issue ->
        val message = when (issue) {
            ProfileIssueCode.InvalidProgram -> Res.string.experimental_invalid
            ProfileIssueCode.ModeSwitch -> Res.string.experimental_mode_switch
            ProfileIssueCode.UnsupportedMetric, ProfileIssueCode.NotAvailable -> Res.string.experimental_sensor
            ProfileIssueCode.UnsupportedControl -> Res.string.experimental_control
            else -> Res.string.experimental_unsupported
        }
        Text(stringResource(message), color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun <T> enumLabel(value: T): String = when (value) {
    is BrewMetric -> value.unitLabel()
    else -> stringResource(
        when (value) {
            RampStyle.Instant -> Res.string.experimental_instant
            RampStyle.Linear -> Res.string.experimental_linear
            RampStyle.EaseIn -> Res.string.experimental_ease_in
            RampStyle.EaseOut -> Res.string.experimental_ease_out
            RampStyle.EaseInOut -> Res.string.experimental_ease_in_out
            RampStart.PreviousTarget -> Res.string.experimental_previous_target
            RampStart.CurrentMeasurement -> Res.string.experimental_current_measurement
            ThresholdComparison.Above -> Res.string.experimental_above
            ThresholdComparison.Below -> Res.string.experimental_below
            else -> Res.string.experimental_unsupported
        }
    )
}

private const val DefaultPhaseWaterMl = 40f

private const val MillisecondsPerSecond = 1000L
