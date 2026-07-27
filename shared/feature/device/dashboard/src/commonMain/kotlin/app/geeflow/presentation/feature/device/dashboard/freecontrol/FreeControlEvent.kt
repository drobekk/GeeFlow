package app.geeflow.presentation.feature.device.dashboard.freecontrol

import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType

sealed interface FreeControlEvent {
    data object BackClicked : FreeControlEvent
    data class ModeChanged(val mode: ControlMode) : FreeControlEvent
    data class TargetChanged(val value: Float) : FreeControlEvent
    data object StartClicked : FreeControlEvent
    data object StopClicked : FreeControlEvent
    data object SaveClicked : FreeControlEvent
    data object RenameClicked : FreeControlEvent
    data class RenameConfirmed(val name: String) : FreeControlEvent
    data object RenameDismissed : FreeControlEvent
    data class ToggleChartVisibility(val type: DashboardChartType) : FreeControlEvent
}
