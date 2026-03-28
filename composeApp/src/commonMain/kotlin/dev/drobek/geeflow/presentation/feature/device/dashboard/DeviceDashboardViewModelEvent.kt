package dev.drobek.geeflow.presentation.feature.device.dashboard

sealed interface DeviceDashboardViewModelEvent {
    data class ShowSnackbar(val message: String) : DeviceDashboardViewModelEvent
}

sealed interface Navigation : DeviceDashboardViewModelEvent {
    data object Back : Navigation
    data object DeviceList : Navigation
    data class Clean(val id: String) : Navigation
    data class QuickSettings(val id: String) : Navigation
}
