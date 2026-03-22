package dev.drobek.geeflow.presentation.feature.device.settings.main

sealed interface DeviceLitViewModelEvent

sealed interface Navigation : DeviceLitViewModelEvent {
    data object Back : Navigation
    data class BrewingSettings(val deviceId: String) : Navigation
    data class MaintenanceSettings(val deviceId: String) : Navigation
    data class ConnectivitySettings(val deviceId: String) : Navigation
}
