package app.geeflow.data.device.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import app.geeflow.data.device.DeviceBrewingSettingsRepository
import app.geeflow.data.device.model.DeviceBrewingSettings
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
class DeviceBrewingSettingsRepositoryImpl(private val dataStore: DataStore<Preferences>) : DeviceBrewingSettingsRepository {
    override fun observe(deviceId: Long) = dataStore.data.map { preferences ->
        DeviceBrewingSettings(
            treatManualAsFlush = preferences[treatManualAsFlushKey(deviceId)] ?: true,
            autoFlushEnabled = preferences[enabledKey(deviceId)] ?: false,
            autoFlushDelaySeconds = preferences[delayKey(deviceId)] ?: DeviceBrewingSettings.DefaultDelaySeconds,
        )
    }

    override suspend fun save(deviceId: Long, settings: DeviceBrewingSettings) {
        dataStore.edit {
            it[treatManualAsFlushKey(deviceId)] = settings.treatManualAsFlush
            it[enabledKey(deviceId)] = settings.autoFlushEnabled
            it[delayKey(deviceId)] = settings.autoFlushDelaySeconds
        }
    }

    override suspend fun remove(deviceId: Long) {
        dataStore.edit {
            it.remove(treatManualAsFlushKey(deviceId))
            it.remove(enabledKey(deviceId))
            it.remove(delayKey(deviceId))
        }
    }

    private fun treatManualAsFlushKey(deviceId: Long) = booleanPreferencesKey("treat_manual_as_flush_$deviceId")
    private fun enabledKey(deviceId: Long) = booleanPreferencesKey("auto_flush_enabled_$deviceId")
    private fun delayKey(deviceId: Long) = intPreferencesKey("auto_flush_delay_$deviceId")
}
