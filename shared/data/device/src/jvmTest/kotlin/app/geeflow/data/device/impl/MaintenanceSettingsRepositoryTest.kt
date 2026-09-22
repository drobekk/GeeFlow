package app.geeflow.data.device.impl

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.geeflow.data.device.model.CleaningProgram
import app.geeflow.data.device.model.CleaningType
import app.geeflow.data.device.model.MaintenanceSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MaintenanceSettingsRepositoryTest {
    @Test
    fun `settings survive reopening and initialization never overwrites a device`() = runBlocking {
        val directory = Files.createTempDirectory("maintenance-test").toFile()
        val file = directory.resolve("settings.preferences_pb")
        val firstJob = SupervisorJob()
        val secondJob = SupervisorJob()
        val initial = MaintenanceSettings(
            daily = CleaningProgram(flushSeconds = 5, restSeconds = 5, cycles = 3),
            deep = CleaningProgram(flushSeconds = 5, restSeconds = 5, cycles = 6),
        )
        try {
            val first = MaintenanceSettingsRepositoryImpl(
                PreferenceDataStoreFactory.create(
                    scope = CoroutineScope(Dispatchers.IO + firstJob),
                    produceFile = { file },
                ),
            )
            first.initialize(1, initial)
            first.initialize(2, initial)
            first.save(1, initial.copy(dailyReminder = initial.dailyReminder.copy(enabled = true)), 100)
            first.postpone(1, setOf(CleaningType.Daily), 120)
            first.initialize(1, initial.copy(daily = initial.daily.copy(cycles = 9)))
            assertEquals(121L, first.observe(1).first()?.dailyReminder?.nextDueDay)
            assertEquals(initial, first.observe(2).first())
            firstJob.cancelAndJoin()
            val second = MaintenanceSettingsRepositoryImpl(
                PreferenceDataStoreFactory.create(
                    scope = CoroutineScope(Dispatchers.IO + secondJob),
                    produceFile = { file },
                ),
            )
            assertEquals(121L, second.observe(1).first()?.dailyReminder?.nextDueDay)
            assertEquals(initial.daily, second.observe(1).first()?.daily)
            second.remove(1)
            assertNull(second.observe(1).first())
            assertEquals(initial, second.observe(2).first())
        } finally {
            firstJob.cancelAndJoin()
            secondJob.cancelAndJoin()
            directory.deleteRecursively()
        }
    }
}
