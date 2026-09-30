package app.geeflow.data.device.impl

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.geeflow.data.device.model.AutoFlushSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class AutoFlushSettingsRepositoryTest {
    @Test
    fun `settings persist independently for each device and are removed with it`() = runBlocking {
        val directory = Files.createTempDirectory("auto-flush-test").toFile()
        val file = directory.resolve("settings.preferences_pb")
        val firstJob = SupervisorJob()
        val secondJob = SupervisorJob()
        try {
            val first = AutoFlushSettingsRepositoryImpl(
                PreferenceDataStoreFactory.create(
                    scope = CoroutineScope(Dispatchers.IO + firstJob),
                    produceFile = { file },
                ),
            )
            assertEquals(AutoFlushSettings(), first.observe(1).first())
            first.save(1, AutoFlushSettings(enabled = true, delaySeconds = 15))
            first.save(2, AutoFlushSettings(enabled = true, delaySeconds = 30))
            firstJob.cancelAndJoin()

            val second = AutoFlushSettingsRepositoryImpl(
                PreferenceDataStoreFactory.create(
                    scope = CoroutineScope(Dispatchers.IO + secondJob),
                    produceFile = { file },
                ),
            )
            assertEquals(AutoFlushSettings(true, 15), second.observe(1).first())
            assertEquals(AutoFlushSettings(true, 30), second.observe(2).first())
            second.remove(1)
            assertEquals(AutoFlushSettings(), second.observe(1).first())
            assertEquals(AutoFlushSettings(true, 30), second.observe(2).first())
        } finally {
            firstJob.cancelAndJoin()
            secondJob.cancelAndJoin()
            directory.deleteRecursively()
        }
    }
}
