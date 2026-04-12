package dev.drobek.geeflow.presentation.feature.device.settings.maintenance

data class MaintenanceSettingsViewState(
    val cleaning: Cleaning = Cleaning(),
    val waterAlarm: Boolean = false,
    val applyButtonLoading: Boolean = false,
    val applyButtonVisible: Boolean = false,
) {
    data class Cleaning(
        val timeSec: String = "1",
        val timeList: List<String> = emptyList(),
        val restSec: String = "1",
        val restList: List<String> = emptyList(),
        val count: String = "1",
        val countList: List<String> = emptyList(),
    )
}
