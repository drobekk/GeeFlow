package dev.drobek.geeflow.presentation.feature.device.dashboard.quickmaintenance

sealed interface QuickMaintenanceViewModelEvent

sealed interface Navigation : QuickMaintenanceViewModelEvent {
    data object Back : Navigation
    data class MaintenanceSettings(val deviceId: String) : Navigation
}
