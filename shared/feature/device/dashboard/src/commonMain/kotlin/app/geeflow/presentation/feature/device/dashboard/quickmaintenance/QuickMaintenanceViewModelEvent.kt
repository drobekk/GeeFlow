package app.geeflow.presentation.feature.device.dashboard.quickmaintenance

sealed interface QuickMaintenanceViewModelEvent {
    data class ShowSnackbar(val message: String) : QuickMaintenanceViewModelEvent
}
