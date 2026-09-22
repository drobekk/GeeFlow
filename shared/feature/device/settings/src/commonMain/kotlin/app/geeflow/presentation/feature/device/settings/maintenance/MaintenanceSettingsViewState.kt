package app.geeflow.presentation.feature.device.settings.maintenance

import app.geeflow.data.device.model.CleaningReminder

data class MaintenanceSettingsViewState(
    val cleaning: Cleaning = Cleaning(),
    val deepCleaning: Cleaning = Cleaning(reminder = CleaningReminder(intervalDays = 7)),
    val waterAlarm: Boolean = false,
    val applyButtonLoading: Boolean = false,
    val applyButtonVisible: Boolean = false,
) {
    data class Cleaning(
        val reminder: CleaningReminder = CleaningReminder(),
        val timeSec: String = "1",
        val timeList: List<String> = emptyList(),
        val restSec: String = "1",
        val restList: List<String> = emptyList(),
        val count: String = "1",
        val countList: List<String> = emptyList(),
    )
}
