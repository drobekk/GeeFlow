package dev.drobek.geeflow.data.user.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dev.drobek.geeflow.data.user.UserSettingsRepository
import dev.drobek.geeflow.data.user.model.AppTheme
import dev.drobek.geeflow.data.user.model.ChartType
import dev.drobek.geeflow.data.user.model.DarkMode
import dev.drobek.geeflow.data.user.model.TemperatureUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
class UserSettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : UserSettingsRepository {

    override fun visibleCharts(userId: Long): Flow<Set<ChartType>> = dataStore.data.map { preferences ->
        preferences[visibleChartsKey(userId)]
            ?.mapNotNull { ChartType.entries.find { entry -> entry.name == it } }
            ?.toSet()
            ?: setOf(ChartType.PRESSURE, ChartType.FLOW_RATE, ChartType.WEIGHT_RATE)
    }

    override fun darkMode(userId: Long): Flow<DarkMode> = dataStore.data.map { preferences ->
        preferences[darkModeKey(userId)]
            ?.let { DarkMode.entries.find { entry -> entry.name == it } }
            ?: DarkMode.SYSTEM
    }

    override fun appTheme(userId: Long): Flow<AppTheme> = dataStore.data.map { preferences ->
        preferences[appThemeKey(userId)]
            ?.let { AppTheme.entries.find { entry -> entry.name == it } }
            ?: AppTheme.ESPRESSO
    }

    override fun fullScreenMode(userId: Long): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[fullScreenModeKey(userId)] ?: false
    }

    override fun keepScreenOn(userId: Long): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[keepScreenOnKey(userId)] ?: false
    }

    override fun autoConnect(userId: Long): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[autoConnectKey(userId)] ?: true
    }

    override fun temperatureUnit(userId: Long): Flow<TemperatureUnit> = dataStore.data.map { preferences ->
        preferences[temperatureUnitKey(userId)]
            ?.let { TemperatureUnit.entries.find { entry -> entry.name == it } }
            ?: TemperatureUnit.CELSIUS
    }

    override suspend fun setVisibleCharts(userId: Long, types: Set<ChartType>) {
        dataStore.edit { preferences ->
            preferences[visibleChartsKey(userId)] = types.map { it.name }.toSet()
        }
    }

    override suspend fun setDarkMode(userId: Long, mode: DarkMode) {
        dataStore.edit { preferences ->
            preferences[darkModeKey(userId)] = mode.name
        }
    }

    override suspend fun setAppTheme(userId: Long, theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[appThemeKey(userId)] = theme.name
        }
    }

    override suspend fun setFullScreenMode(userId: Long, enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[fullScreenModeKey(userId)] = enabled
        }
    }

    override suspend fun setKeepScreenOn(userId: Long, enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[keepScreenOnKey(userId)] = enabled
        }
    }

    override suspend fun setAutoConnect(userId: Long, enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[autoConnectKey(userId)] = enabled
        }
    }

    override suspend fun setTemperatureUnit(userId: Long, unit: TemperatureUnit) {
        dataStore.edit { preferences ->
            preferences[temperatureUnitKey(userId)] = unit.name
        }
    }

    override suspend fun clearUserSettings(userId: Long) {
        dataStore.edit { preferences ->
            preferences.remove(visibleChartsKey(userId))
            preferences.remove(darkModeKey(userId))
            preferences.remove(appThemeKey(userId))
            preferences.remove(fullScreenModeKey(userId))
            preferences.remove(keepScreenOnKey(userId))
            preferences.remove(autoConnectKey(userId))
            preferences.remove(temperatureUnitKey(userId))
        }
    }

    private fun key(name: String, userId: Long) = "${name}_$userId"

    private fun visibleChartsKey(userId: Long) = stringSetPreferencesKey(key("visible_charts", userId))
    private fun darkModeKey(userId: Long) = stringPreferencesKey(key("dark_mode", userId))
    private fun appThemeKey(userId: Long) = stringPreferencesKey(key("app_theme", userId))
    private fun fullScreenModeKey(userId: Long) = booleanPreferencesKey(key("full_screen_mode", userId))
    private fun keepScreenOnKey(userId: Long) = booleanPreferencesKey(key("keep_screen_on", userId))
    private fun autoConnectKey(userId: Long) = booleanPreferencesKey(key("auto_connect", userId))
    private fun temperatureUnitKey(userId: Long) = stringPreferencesKey(key("temperature_unit", userId))
}
