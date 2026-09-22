package app.geeflow.data.device.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.geeflow.data.device.MaintenanceSettingsRepository
import app.geeflow.data.device.model.CleaningType
import app.geeflow.data.device.model.MaintenanceSettings
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
class MaintenanceSettingsRepositoryImpl(private val dataStore: DataStore<Preferences>) : MaintenanceSettingsRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override fun observe(deviceId: Long) = dataStore.data.map { it[key(deviceId)]?.let(::decode) }

    override suspend fun initialize(deviceId: Long, settings: MaintenanceSettings) {
        dataStore.edit { if (it[key(deviceId)] == null) it[key(deviceId)] = json.encodeToString(settings) }
    }

    override suspend fun save(deviceId: Long, settings: MaintenanceSettings, today: Long) {
        dataStore.edit { preferences ->
            val old = preferences[key(deviceId)]?.let(::decode) ?: settings
            val updated = settings.copy(
                dailyReminder = settings.dailyReminder.saved(old.dailyReminder, today),
                deepReminder = settings.deepReminder.saved(old.deepReminder, today),
            )
            preferences[key(deviceId)] = json.encodeToString(updated)
        }
    }

    override suspend fun postpone(deviceId: Long, types: Set<CleaningType>, today: Long) {
        dataStore.edit { preferences ->
            val settings = requireNotNull(preferences[key(deviceId)]) { "Maintenance settings are not initialized" }
            preferences[key(deviceId)] = json.encodeToString(decode(settings).postpone(types, today))
        }
    }

    override suspend fun remove(deviceId: Long) {
        dataStore.edit { it.remove(key(deviceId)) }
    }

    private fun decode(value: String): MaintenanceSettings = json.decodeFromString(value)
    private fun key(deviceId: Long) = stringPreferencesKey("maintenance_$deviceId")
}
