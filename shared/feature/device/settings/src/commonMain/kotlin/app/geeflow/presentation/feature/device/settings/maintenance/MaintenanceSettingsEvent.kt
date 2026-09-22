package app.geeflow.presentation.feature.device.settings.maintenance

import app.geeflow.data.device.model.CleaningReminder
import app.geeflow.data.device.model.CleaningType

sealed interface MaintenanceSettingsEvent {
    data class CleaningTimeChanged(val timeSec: String, val type: CleaningType = CleaningType.Daily) : MaintenanceSettingsEvent
    data class CleaningRestChanged(val standbySec: String, val type: CleaningType = CleaningType.Daily) : MaintenanceSettingsEvent
    data class CleaningCountChanged(val count: String, val type: CleaningType = CleaningType.Daily) : MaintenanceSettingsEvent
    data class WaterAlarmToggled(val enabled: Boolean) : MaintenanceSettingsEvent
    data class ReminderChanged(val type: CleaningType, val reminder: CleaningReminder) : MaintenanceSettingsEvent
    data object ApplyClicked : MaintenanceSettingsEvent
    data object CloseClicked : MaintenanceSettingsEvent
}
