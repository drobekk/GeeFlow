package app.geeflow.presentation.feature.device.dashboard.main

sealed interface DeviceDashboardViewModelEvent {
    data class ShowSnackbar(val message: String) : DeviceDashboardViewModelEvent
    data object SwitchToDetails : DeviceDashboardViewModelEvent
}
