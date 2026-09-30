package app.geeflow.data.device.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import app.geeflow.data.device.AutoFlushSettingsRepository
import app.geeflow.data.device.model.AutoFlushSettings
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
class AutoFlushSettingsRepositoryImpl(private val dataStore: DataStore<Preferences>) : AutoFlushSettingsRepository {
    override fun observe(deviceId: Long) = dataStore.data.map { preferences ->
        AutoFlushSettings(
            enabled = preferences[enabledKey(deviceId)] ?: false,
            delaySeconds = preferences[delayKey(deviceId)] ?: AutoFlushSettings.DefaultDelaySeconds,
        )
    }

    override suspend fun save(deviceId: Long, settings: AutoFlushSettings) {
        dataStore.edit {
            it[enabledKey(deviceId)] = settings.enabled
            it[delayKey(deviceId)] = settings.delaySeconds
        }
    }

    override suspend fun remove(deviceId: Long) {
        dataStore.edit {
            it.remove(enabledKey(deviceId))
            it.remove(delayKey(deviceId))
        }
    }

    private fun enabledKey(deviceId: Long) = booleanPreferencesKey("auto_flush_enabled_$deviceId")
    private fun delayKey(deviceId: Long) = intPreferencesKey("auto_flush_delay_$deviceId")
}
