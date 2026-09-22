package app.geeflow.data.device.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MaintenanceSettingsTest {
    private val program = CleaningProgram(flushSeconds = 5, restSeconds = 5, cycles = 3)
    private val settings = MaintenanceSettings(
        daily = program,
        deep = program.copy(cycles = 6),
        dailyReminder = CleaningReminder(enabled = true, intervalDays = 1, nextDueDay = 100),
        deepReminder = CleaningReminder(enabled = true, intervalDays = 7, nextDueDay = 100),
    )

    @Test
    fun `enabling and changing the interval schedule from today`() {
        val off = CleaningReminder(intervalDays = 7)
        val enabled = off.copy(enabled = true).saved(off, 100)
        assertEquals(107L, enabled.nextDueDay)
        assertEquals(103L, enabled.copy(intervalDays = 3).saved(enabled, 100).nextDueDay)
        assertEquals(enabled, enabled.saved(enabled, 110))
        assertEquals(7, enabled.copy(enabled = false).saved(enabled, 100).intervalDays)
    }

    @Test
    fun `overdue reminders are combined and skip advances from today`() {
        assertEquals(CleaningType.entries.toSet(), settings.dueTypes(120))
        val skipped = settings.postpone(settings.dueTypes(120), 120)
        assertEquals(121L, skipped.dailyReminder.nextDueDay)
        assertEquals(127L, skipped.deepReminder.nextDueDay)
        assertTrue(skipped.dueTypes(120).isEmpty())
        assertFalse(skipped.dailyReminder.isDue(120))
    }

    @Test
    fun `daily leaves deep unchanged while deep advances both reminders`() {
        val daily = settings.postpone(setOf(CleaningType.Daily), 120)
        assertEquals(settings.deepReminder, daily.deepReminder)
        val deep = settings.postpone(CleaningType.entries.toSet(), 120)
        assertEquals(121L, deep.dailyReminder.nextDueDay)
        assertEquals(127L, deep.deepReminder.nextDueDay)
    }
}
