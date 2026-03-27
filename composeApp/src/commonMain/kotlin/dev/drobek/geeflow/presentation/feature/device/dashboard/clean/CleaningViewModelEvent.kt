package dev.drobek.geeflow.presentation.feature.device.dashboard.clean

sealed interface CleanViewModelEvent

sealed interface Navigation : CleanViewModelEvent {
    data object Back : Navigation
    data class DeviceSettings(val deviceId: String) : Navigation
}
