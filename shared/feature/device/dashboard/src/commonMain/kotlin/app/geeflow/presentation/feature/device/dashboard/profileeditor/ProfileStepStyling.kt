package app.geeflow.presentation.feature.device.dashboard.profileeditor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorViewState.FinishTarget
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorViewState.Step
import app.geeflow.ui.icons.Flow
import app.geeflow.ui.icons.GeeFlowIcon
import app.geeflow.ui.icons.Pressure
import app.geeflow.ui.icons.Weight
import app.geeflow.ui.theme.GeeFlowTheme
import geeflow.shared.core.ui.generated.resources.common_sec
import geeflow.shared.core.ui.generated.resources.unit_bar
import geeflow.shared.core.ui.generated.resources.unit_grams
import geeflow.shared.core.ui.generated.resources.unit_milliliters
import geeflow.shared.core.ui.generated.resources.unit_milliliters_per_second
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_finish_volume
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_finish_weight
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_step_flow
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_step_pressure
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_step_wait
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

private const val DecimalScale = 10f

internal fun StepType.icon(): ImageVector = when (this) {
    StepType.Flow -> GeeFlowIcon.Flow
    StepType.Pressure -> GeeFlowIcon.Pressure
    StepType.Wait -> Icons.Filled.HourglassEmpty
}

@Composable
internal fun StepType.color(): Color = when (this) {
    StepType.Flow -> GeeFlowTheme.colors.water
    StepType.Pressure -> MaterialTheme.colorScheme.error
    StepType.Wait -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
internal fun StepType.label(): String = stringResource(
    when (this) {
        StepType.Flow -> Res.string.profile_editor_step_flow
        StepType.Pressure -> Res.string.profile_editor_step_pressure
        StepType.Wait -> Res.string.profile_editor_step_wait
    },
)

@Composable
internal fun FinishTargetType.label(): String = stringResource(
    when (this) {
        FinishTargetType.Volume -> Res.string.profile_editor_finish_volume
        FinishTargetType.Weight -> Res.string.profile_editor_finish_weight
    },
)

internal fun FinishTargetType.icon(): ImageVector = when (this) {
    FinishTargetType.Volume -> GeeFlowIcon.Flow
    FinishTargetType.Weight -> GeeFlowIcon.Weight
}

@Composable
internal fun FinishTargetType.color(): Color = when (this) {
    FinishTargetType.Volume -> GeeFlowTheme.colors.water
    FinishTargetType.Weight -> MaterialTheme.colorScheme.error
}

@Composable
internal fun Step.timeLabel(): String = "$timeSec ${stringResource(CoreRes.string.common_sec)}"

@Composable
internal fun Step.valueLabel(): String = when (type) {
    StepType.Wait -> stringResource(Res.string.profile_editor_step_wait)
    StepType.Flow -> "${value.formatValue()} ${stringResource(CoreRes.string.unit_milliliters_per_second)}"
    StepType.Pressure -> "${value.formatValue()} ${stringResource(CoreRes.string.unit_bar)}"
}

@Composable
internal fun FinishTarget.valueLabel(type: FinishTargetType): String = when (type) {
    FinishTargetType.Volume -> "${volume.toInt()} ${stringResource(CoreRes.string.unit_milliliters)}"
    FinishTargetType.Weight -> "${weight.toInt()} ${stringResource(CoreRes.string.unit_grams)}"
}

/** Renders a step value with a single decimal, e.g. `9.0`. */
internal fun Float.formatValue(): String = ((this * DecimalScale).roundToInt() / DecimalScale).toString()
