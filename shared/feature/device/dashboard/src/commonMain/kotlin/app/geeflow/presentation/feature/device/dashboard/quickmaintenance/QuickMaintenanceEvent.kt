package app.geeflow.presentation.feature.device.dashboard.quickmaintenance

import app.geeflow.data.device.model.CleaningType

sealed interface QuickMaintenanceEvent {
    data class TypeSelected(val type: CleaningType) : QuickMaintenanceEvent
    data object ToggleCleaningClicked : QuickMaintenanceEvent
    data class MoreSettingsClicked(val isExpanded: Boolean) : QuickMaintenanceEvent
    data object CloseClicked : QuickMaintenanceEvent
}
