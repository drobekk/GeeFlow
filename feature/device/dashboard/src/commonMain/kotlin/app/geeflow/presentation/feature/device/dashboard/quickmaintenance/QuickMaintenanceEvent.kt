package app.geeflow.presentation.feature.device.dashboard.quickmaintenance

sealed interface QuickMaintenanceEvent {
    data object ToggleCleaningClicked : QuickMaintenanceEvent
    data class MoreSettingsClicked(val isExpanded: Boolean) : QuickMaintenanceEvent
    data object CloseClicked : QuickMaintenanceEvent
}
