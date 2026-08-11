package app.geeflow.presentation.feature.device.dashboard.profileeditor

import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType

internal sealed interface ProfileEditorEvent {
    data object BackClicked : ProfileEditorEvent
    data object SaveClicked : ProfileEditorEvent
    data object TestClicked : ProfileEditorEvent
    data object StopClicked : ProfileEditorEvent
    data class ToggleChartVisibility(val type: DashboardChartType) : ProfileEditorEvent
    data object EditDetailsClicked : ProfileEditorEvent
    data class DetailsConfirmed(val name: String, val description: String) : ProfileEditorEvent
    data object AddStepClicked : ProfileEditorEvent
    data class StepTypeSelected(val type: StepType) : ProfileEditorEvent
    data class StepClicked(val id: Long) : ProfileEditorEvent
    data class StepRemoved(val id: Long) : ProfileEditorEvent
    data class StepsReordered(val from: Int, val to: Int) : ProfileEditorEvent
    data class StepValuesConfirmed(val timeSec: Int, val value: Float) : ProfileEditorEvent
    data class FinishTargetClicked(val type: FinishTargetType) : ProfileEditorEvent
    data class FinishTargetConfirmed(val target: Float) : ProfileEditorEvent
    data object DialogDismissed : ProfileEditorEvent
}
