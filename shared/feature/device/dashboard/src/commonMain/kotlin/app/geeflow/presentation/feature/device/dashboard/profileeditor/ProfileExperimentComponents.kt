package app.geeflow.presentation.feature.device.dashboard.profileeditor

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.RampStart
import app.geeflow.data.brew.model.RampStyle
import app.geeflow.data.brew.model.ThresholdComparison
import app.geeflow.ui.icons.Experiment
import app.geeflow.ui.icons.GeeFlowIcon
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_above
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_below
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_boiler_pressure
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_condition_summary
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_current_measurement
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_ease_in
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_ease_in_out
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_ease_out
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_flow
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_group_pressure
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_instant
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_linear
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_previous_target
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_pump_pressure
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_ramp_summary
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_title
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_unsupported
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_water
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_weight
import geeflow.shared.feature.device.dashboard.generated.resources.step_editor_time
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun BrewMetric.unitLabel(): String = when (this) {
    BrewMetric.PhaseTime -> stringResource(Res.string.step_editor_time)
    BrewMetric.PumpedVolume -> stringResource(Res.string.experimental_water)
    BrewMetric.CupWeight -> stringResource(Res.string.experimental_weight)
    BrewMetric.PumpFlow -> stringResource(Res.string.experimental_flow)
    BrewMetric.PumpPressure -> stringResource(Res.string.experimental_pump_pressure)
    BrewMetric.GroupPressure -> stringResource(Res.string.experimental_group_pressure)
    BrewMetric.BoilerPressure -> stringResource(Res.string.experimental_boiler_pressure)
}

@Composable
internal fun ExperimentButton(
    modifier: Modifier = Modifier,
    tonal: Boolean = false,
    onClick: () -> Unit,
) {
    if (tonal) {
        FilledTonalIconButton(onClick = onClick, modifier = modifier) {
            Icon(GeeFlowIcon.Experiment, contentDescription = stringResource(Res.string.experimental_title))
        }
    } else {
        IconButton(onClick = onClick, modifier = modifier) {
            Icon(GeeFlowIcon.Experiment, contentDescription = stringResource(Res.string.experimental_title))
        }
    }
}

@Composable
internal fun PhaseDetails(step: ProfileEditorViewState.Step) {
    Column {
        if (step.ramp.style != RampStyle.Instant) {
            Text(
                stringResource(
                    Res.string.experimental_ramp_summary,
                    enumLabel(step.ramp.style),
                    (step.ramp.durationMillis / MillisecondsPerSecond.toFloat()).toString(),
                ),
            )
        }
        step.exitConditions.forEach { condition ->
            Text(
                stringResource(
                    Res.string.experimental_condition_summary,
                    condition.metric.unitLabel(),
                    enumLabel(condition.comparison),
                    condition.threshold.toString(),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
internal fun <T> enumLabel(value: T): String = when (value) {
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
        },
    )
}

private const val MillisecondsPerSecond = 1000L
