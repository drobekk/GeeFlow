package dev.drobek.geeflow.presentation.feature.device.dashboard.quickmaintenance

sealed interface QuickMaintenanceEvent {
    data object ToggleCleaningClicked : QuickMaintenanceEvent
    data object MoreSettingsClicked : QuickMaintenanceEvent
    data object CloseClicked : QuickMaintenanceEvent
}
