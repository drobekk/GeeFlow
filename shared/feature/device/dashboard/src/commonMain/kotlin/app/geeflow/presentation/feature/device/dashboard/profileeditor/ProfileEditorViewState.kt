package app.geeflow.presentation.feature.device.dashboard.profileeditor

import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Brew
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.FlowRate
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.Pressure
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.WeightRate
import app.geeflow.presentation.feature.device.dashboard.model.ChartData

internal data class ProfileEditorViewState(
    val profileName: String = "",
    val description: String = "",
    val steps: List<Step> = emptyList(),
    val finishTarget: FinishTarget = FinishTarget(),
    val targetData: Map<Float, ChartData> = emptyMap(),
    val pressureRange: ClosedFloatingPointRange<Float> = 0f..0f,
    val flowRange: ClosedFloatingPointRange<Float> = 0f..0f,
    val brew: Brew = Brew(),
    val visibleCharts: Set<DashboardChartType> = setOf(Pressure, FlowRate, WeightRate),
    val isBrewing: Boolean = false,
    val dialog: ProfileEditorDialog? = null,
) {
    /** A profile made up of nothing but waits would never brew, so it can be neither tested nor saved. */
    val canTest: Boolean = steps.any { it.type != StepType.Wait }
    val canSave: Boolean = profileName.isNotBlank() && canTest

    data class Step(
        val id: Long,
        val type: StepType,
        val timeSec: Int,
        val value: Float,
    )

    /** Both targets are kept so switching type does not lose the value entered for the other one. */
    data class FinishTarget(
        val type: FinishTargetType = FinishTargetType.Volume,
        val volume: Float = ProfileEditorDefaults.VolumeMl,
        val weight: Float = ProfileEditorDefaults.WeightG,
    ) {
        val target: Float = when (type) {
            FinishTargetType.Volume -> volume
            FinishTargetType.Weight -> weight
        }
    }
}

internal enum class StepType {
    Flow,
    Pressure,
    Wait,
}

internal enum class FinishTargetType {
    Volume,
    Weight,
}

internal sealed interface ProfileEditorDialog {
    data object Details : ProfileEditorDialog

    data object StepTypePicker : ProfileEditorDialog

    /** Value entry for a new step when [stepId] is `null`, otherwise for the step being edited. */
    data class StepValues(
        val type: StepType,
        val stepId: Long?,
        val timeSec: Int,
        val value: Float,
    ) : ProfileEditorDialog

    data class FinishTargetValue(val type: FinishTargetType, val target: Float) : ProfileEditorDialog
}
