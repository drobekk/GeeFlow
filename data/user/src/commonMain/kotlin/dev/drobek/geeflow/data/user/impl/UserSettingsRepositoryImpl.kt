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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
class UserSettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : UserSettingsRepository {

    private val visibleChartsKey = stringSetPreferencesKey("visible_charts")
    private val darkModeKey = stringPreferencesKey("dark_mode")
    private val appThemeKey = stringPreferencesKey("app_theme")
    private val fullScreenModeKey = booleanPreferencesKey("full_screen_mode")
    private val keepScreenOnKey = booleanPreferencesKey("keep_screen_on")

    override val visibleCharts: Flow<Set<ChartType>> = dataStore.data.map { preferences ->
        val stringSet = preferences[visibleChartsKey]
        stringSet
            ?.mapNotNull { ChartType.entries.find { entry -> entry.name == it } }
            ?.toSet()
            ?: setOf(ChartType.PRESSURE, ChartType.FLOW_RATE, ChartType.WEIGHT_RATE)
    }

    override val darkMode: Flow<DarkMode> = dataStore.data.map { preferences ->
        preferences[darkModeKey]
            ?.let { DarkMode.entries.find { entry -> entry.name == it } }
            ?: DarkMode.SYSTEM
    }

    override val appTheme: Flow<AppTheme> = dataStore.data.map { preferences ->
        preferences[appThemeKey]
            ?.let { AppTheme.entries.find { entry -> entry.name == it } }
            ?: AppTheme.ESPRESSO
    }

    override val fullScreenMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[fullScreenModeKey] ?: false
    }

    override val keepScreenOn: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[keepScreenOnKey] ?: false
    }

    override suspend fun setVisibleCharts(types: Set<ChartType>) {
        dataStore.edit { preferences ->
            preferences[visibleChartsKey] = types.map { it.name }.toSet()
        }
    }

    override suspend fun setDarkMode(mode: DarkMode) {
        dataStore.edit { preferences ->
            preferences[darkModeKey] = mode.name
        }
    }

    override suspend fun setAppTheme(theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[appThemeKey] = theme.name
        }
    }

    override suspend fun setFullScreenMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[fullScreenModeKey] = enabled
        }
    }

    override suspend fun setKeepScreenOn(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[keepScreenOnKey] = enabled
        }
    }
}
