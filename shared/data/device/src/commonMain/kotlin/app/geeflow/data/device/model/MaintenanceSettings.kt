package app.geeflow.data.device.model

import kotlinx.serialization.Serializable

@Serializable
data class MaintenanceSettings(
    val daily: CleaningProgram,
    val deep: CleaningProgram,
    val dailyReminder: CleaningReminder = CleaningReminder(),
    val deepReminder: CleaningReminder = CleaningReminder(intervalDays = 7),
) {
    fun program(type: CleaningType): CleaningProgram = if (type == CleaningType.Daily) daily else deep
    fun reminder(type: CleaningType): CleaningReminder = if (type == CleaningType.Daily) dailyReminder else deepReminder
    fun dueTypes(today: Long): Set<CleaningType> = CleaningType.entries.filter { reminder(it).isDue(today) }.toSet()

    fun postpone(types: Set<CleaningType>, today: Long): MaintenanceSettings = copy(
        dailyReminder = if (CleaningType.Daily in types) dailyReminder.postponed(today) else dailyReminder,
        deepReminder = if (CleaningType.Deep in types) deepReminder.postponed(today) else deepReminder,
    )
}
