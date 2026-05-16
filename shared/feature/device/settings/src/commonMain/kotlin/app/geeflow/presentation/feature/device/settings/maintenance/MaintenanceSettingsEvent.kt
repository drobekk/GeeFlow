package app.geeflow.presentation.feature.device.settings.maintenance

sealed interface MaintenanceSettingsEvent {
    data class CleaningTimeChanged(val timeSec: String) : MaintenanceSettingsEvent
    data class CleaningRestChanged(val standbySec: String) : MaintenanceSettingsEvent
    data class CleaningCountChanged(val count: String) : MaintenanceSettingsEvent
    data class WaterAlarmToggled(val enabled: Boolean) : MaintenanceSettingsEvent
    data object ApplyClicked : MaintenanceSettingsEvent
    data object CloseClicked : MaintenanceSettingsEvent
}
