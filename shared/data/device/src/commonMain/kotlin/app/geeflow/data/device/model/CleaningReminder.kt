package app.geeflow.data.device.model

import kotlinx.serialization.Serializable

@Serializable
data class CleaningReminder(
    val enabled: Boolean = false,
    val intervalDays: Int = 1,
    val nextDueDay: Long? = null,
) {
    init {
        require(intervalDays in 1..MAX_INTERVAL_DAYS)
    }

    fun isDue(today: Long): Boolean = enabled && nextDueDay?.let { it <= today } == true

    fun postponed(today: Long): CleaningReminder = copy(nextDueDay = if (enabled) today + intervalDays else null)

    fun saved(previous: CleaningReminder, today: Long): CleaningReminder = when {
        !enabled -> copy(nextDueDay = null)
        !previous.enabled || intervalDays != previous.intervalDays || previous.nextDueDay == null -> postponed(today)
        else -> copy(nextDueDay = previous.nextDueDay)
    }
    companion object {
        const val MAX_INTERVAL_DAYS = 365
    }
}
