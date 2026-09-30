package app.geeflow.data.device.impl

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import app.geeflow.data.device.model.DeviceBrewingSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceBrewingSettingsRepositoryTest {
    @Test
    fun `legacy auto flush values survive and old user settings do not affect paddle settings`() = runBlocking {
        val directory = Files.createTempDirectory("paddle-legacy-test").toFile()
        val job = SupervisorJob()
        try {
            val store = PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Dispatchers.IO + job),
                produceFile = { directory.resolve("settings.preferences_pb") },
            )
            store.edit {
                it[booleanPreferencesKey("auto_flush_enabled_1")] = true
                it[intPreferencesKey("auto_flush_delay_1")] = 25
                it[booleanPreferencesKey("skip_manual_brew_history_1")] = false
            }
            val repository = DeviceBrewingSettingsRepositoryImpl(store)
            assertEquals(DeviceBrewingSettings(true, true, 25), repository.observe(1).first())
            val updated = DeviceBrewingSettings(false, false, 40)
            repository.save(1, updated)
            assertEquals(updated, repository.observe(1).first())
            assertEquals(false, store.data.first()[booleanPreferencesKey("treat_manual_as_flush_1")])
            assertEquals(false, store.data.first()[booleanPreferencesKey("auto_flush_enabled_1")])
            assertEquals(40, store.data.first()[intPreferencesKey("auto_flush_delay_1")])
            repository.remove(1)
            assertEquals(DeviceBrewingSettings(), repository.observe(1).first())
            assertEquals(false, store.data.first()[booleanPreferencesKey("skip_manual_brew_history_1")])
        } finally {
            job.cancelAndJoin()
            directory.deleteRecursively()
        }
    }

    @Test
    fun `settings persist independently for each device and are removed with it`() = runBlocking {
        val directory = Files.createTempDirectory("auto-flush-test").toFile()
        val file = directory.resolve("settings.preferences_pb")
        val firstJob = SupervisorJob()
        val secondJob = SupervisorJob()
        try {
            val first = DeviceBrewingSettingsRepositoryImpl(
                PreferenceDataStoreFactory.create(
                    scope = CoroutineScope(Dispatchers.IO + firstJob),
                    produceFile = { file },
                ),
            )
            assertEquals(DeviceBrewingSettings(), first.observe(1).first())
            first.save(1, DeviceBrewingSettings(treatManualAsFlush = false, autoFlushEnabled = true, autoFlushDelaySeconds = 15))
            first.save(2, DeviceBrewingSettings(treatManualAsFlush = false, autoFlushEnabled = true, autoFlushDelaySeconds = 30))
            firstJob.cancelAndJoin()

            val second = DeviceBrewingSettingsRepositoryImpl(
                PreferenceDataStoreFactory.create(
                    scope = CoroutineScope(Dispatchers.IO + secondJob),
                    produceFile = { file },
                ),
            )
            assertEquals(DeviceBrewingSettings(false, true, 15), second.observe(1).first())
            assertEquals(DeviceBrewingSettings(false, true, 30), second.observe(2).first())
            second.remove(1)
            assertEquals(DeviceBrewingSettings(), second.observe(1).first())
            assertEquals(DeviceBrewingSettings(false, true, 30), second.observe(2).first())
        } finally {
            firstJob.cancelAndJoin()
            secondJob.cancelAndJoin()
            directory.deleteRecursively()
        }
    }
}
