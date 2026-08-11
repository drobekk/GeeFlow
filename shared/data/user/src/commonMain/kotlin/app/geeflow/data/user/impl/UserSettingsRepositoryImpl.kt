package app.geeflow.data.user.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.data.user.model.AppPaletteStyle
import app.geeflow.data.user.model.AppTheme
import app.geeflow.data.user.model.ChartType
import app.geeflow.data.user.model.TemperatureUnit
import app.geeflow.data.user.model.ThemeMode
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

    override fun darkMode(userId: Long): Flow<ThemeMode> = dataStore.data.map { preferences ->
        preferences[darkModeKey(userId)]
            ?.let { ThemeMode.entries.find { entry -> entry.name == it } }
            ?: ThemeMode.SYSTEM
    }

    override fun appTheme(userId: Long): Flow<AppTheme> = dataStore.data.map { preferences ->
        preferences[appThemeKey(userId)]
            ?.let { AppTheme.entries.find { entry -> entry.name == it } }
            ?: AppTheme.ESPRESSO
    }

    override fun paletteStyle(userId: Long): Flow<AppPaletteStyle> = dataStore.data.map { preferences ->
        preferences[paletteStyleKey(userId)]
            ?.let { AppPaletteStyle.entries.find { entry -> entry.name == it } }
            ?: AppPaletteStyle.TONAL_SPOT
    }

    override fun customSeedColor(userId: Long): Flow<Int> = dataStore.data.map { preferences ->
        preferences[customSeedColorKey(userId)] ?: DefaultCustomSeedColor
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

    override fun skipManualBrewHistory(userId: Long): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[skipManualBrewHistoryKey(userId)] ?: true
    }

    override suspend fun setVisibleCharts(userId: Long, types: Set<ChartType>) {
        dataStore.edit { preferences ->
            preferences[visibleChartsKey(userId)] = types.map { it.name }.toSet()
        }
    }

    override suspend fun setDarkMode(userId: Long, mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[darkModeKey(userId)] = mode.name
        }
    }

    override suspend fun setAppTheme(userId: Long, theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[appThemeKey(userId)] = theme.name
        }
    }

    override suspend fun setPaletteStyle(userId: Long, style: AppPaletteStyle) {
        dataStore.edit { preferences ->
            preferences[paletteStyleKey(userId)] = style.name
        }
    }

    override suspend fun setCustomSeedColor(userId: Long, colorArgb: Int) {
        dataStore.edit { preferences ->
            preferences[customSeedColorKey(userId)] = colorArgb
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

    override suspend fun setSkipManualBrewHistory(userId: Long, enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[skipManualBrewHistoryKey(userId)] = enabled
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
            preferences.remove(skipManualBrewHistoryKey(userId))
            preferences.remove(paletteStyleKey(userId))
            preferences.remove(customSeedColorKey(userId))
        }
    }

    private fun key(name: String, userId: Long) = "${name}_$userId"

    private fun visibleChartsKey(userId: Long) = stringSetPreferencesKey(key("visible_charts", userId))
    private fun darkModeKey(userId: Long) = stringPreferencesKey(key("dark_mode", userId))
    private fun appThemeKey(userId: Long) = stringPreferencesKey(key("app_theme", userId))
    private fun paletteStyleKey(userId: Long) = stringPreferencesKey(key("palette_style", userId))
    private fun customSeedColorKey(userId: Long) = intPreferencesKey(key("custom_seed_color", userId))
    private fun fullScreenModeKey(userId: Long) = booleanPreferencesKey(key("full_screen_mode", userId))
    private fun keepScreenOnKey(userId: Long) = booleanPreferencesKey(key("keep_screen_on", userId))
    private fun autoConnectKey(userId: Long) = booleanPreferencesKey(key("auto_connect", userId))
    private fun temperatureUnitKey(userId: Long) = stringPreferencesKey(key("temperature_unit", userId))
    private fun skipManualBrewHistoryKey(userId: Long) = booleanPreferencesKey(key("skip_manual_brew_history", userId))

    companion object {
        private const val DefaultCustomSeedColor = 0xFF1E88E5.toInt()
    }
}
