@file:Suppress("TooManyFunctions")

package app.geeflow.data.user.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.data.user.model.AppPaletteStyle
import app.geeflow.data.user.model.AppTheme
import app.geeflow.data.user.model.ChartType
import app.geeflow.data.user.model.FlowChartRange
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
            ?: ChartType.entries.toSet()
    }

    override fun flowChartRange(userId: Long): Flow<FlowChartRange> = dataStore.data.map { preferences ->
        preferences[flowChartRangeKey(userId)]
            ?.let { stored -> FlowChartRange.entries.firstOrNull { it.maxMillilitersPerSecond == stored } }
            ?: FlowChartRange.FOUR
    }

    override fun favoriteProfiles(userId: Long): Flow<Set<String>> = dataStore.data.map {
        it[favoriteProfilesKey(userId)].orEmpty()
    }

    override suspend fun toggleFavoriteProfile(userId: Long, profileId: String) {
        dataStore.edit { preferences ->
            val favorites = preferences[favoriteProfilesKey(userId)].orEmpty()
            preferences[
                favoriteProfilesKey(
                    userId
                )
            ] = if (profileId in favorites) favorites - profileId else favorites + profileId
        }
    }

    override suspend fun setFavoriteProfile(userId: Long, profileId: String, favorite: Boolean) {
        dataStore.edit { preferences ->
            val favorites = preferences[favoriteProfilesKey(userId)].orEmpty()
            preferences[favoriteProfilesKey(userId)] = if (favorite) favorites + profileId else favorites - profileId
        }
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

    override fun keepScreenOnOnlyWhileCharging(userId: Long): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[keepScreenOnOnlyWhileChargingKey(userId)] ?: false
    }

    override fun screensaverEnabled(userId: Long): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[screensaverEnabledKey(userId)] ?: false
    }

    override fun screensaverTimeoutMinutes(userId: Long): Flow<Int> = dataStore.data.map { preferences ->
        preferences[screensaverTimeoutMinutesKey(userId)] ?: DefaultScreensaverTimeoutMinutes
    }

    override fun autoConnect(userId: Long): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[autoConnectKey(userId)] ?: true
    }

    override fun temperatureUnit(userId: Long): Flow<TemperatureUnit> = dataStore.data.map { preferences ->
        preferences[temperatureUnitKey(userId)]
            ?.let { TemperatureUnit.entries.find { entry -> entry.name == it } }
            ?: TemperatureUnit.CELSIUS
    }

    override fun preferredShotDoseGrams(userId: Long): Flow<Float> = dataStore.data.map { preferences ->
        preferences[shotDoseKey(userId)]
            ?.takeIf { it.isFinite() && it in MIN_SHOT_DOSE_GRAMS..MAX_SHOT_DOSE_GRAMS }
            ?: DEFAULT_SHOT_DOSE_GRAMS
    }

    override suspend fun setVisibleCharts(userId: Long, types: Set<ChartType>) {
        dataStore.edit { preferences ->
            preferences[visibleChartsKey(userId)] = types.map { it.name }.toSet()
        }
    }

    override suspend fun setFlowChartRange(userId: Long, range: FlowChartRange) {
        dataStore.edit { preferences ->
            preferences[flowChartRangeKey(userId)] = range.maxMillilitersPerSecond
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

    override suspend fun setKeepScreenOnOnlyWhileCharging(userId: Long, enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[keepScreenOnOnlyWhileChargingKey(userId)] = enabled
        }
    }

    override suspend fun setScreensaverEnabled(userId: Long, enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[screensaverEnabledKey(userId)] = enabled
        }
    }

    override suspend fun setScreensaverTimeoutMinutes(userId: Long, minutes: Int) {
        dataStore.edit { preferences ->
            preferences[screensaverTimeoutMinutesKey(userId)] = minutes
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

    override suspend fun setPreferredShotDoseGrams(userId: Long, doseGrams: Float) {
        require(doseGrams.isFinite() && doseGrams in MIN_SHOT_DOSE_GRAMS..MAX_SHOT_DOSE_GRAMS)
        dataStore.edit { preferences -> preferences[shotDoseKey(userId)] = doseGrams }
    }

    override suspend fun clearUserSettings(userId: Long) {
        dataStore.edit { preferences ->
            preferences.remove(visibleChartsKey(userId))
            preferences.remove(flowChartRangeKey(userId))
            preferences.remove(favoriteProfilesKey(userId))
            preferences.remove(darkModeKey(userId))
            preferences.remove(appThemeKey(userId))
            preferences.remove(fullScreenModeKey(userId))
            preferences.remove(keepScreenOnKey(userId))
            preferences.remove(keepScreenOnOnlyWhileChargingKey(userId))
            preferences.remove(screensaverEnabledKey(userId))
            preferences.remove(screensaverTimeoutMinutesKey(userId))
            preferences.remove(autoConnectKey(userId))
            preferences.remove(temperatureUnitKey(userId))
            preferences.remove(shotDoseKey(userId))
            preferences.remove(paletteStyleKey(userId))
            preferences.remove(customSeedColorKey(userId))
        }
    }

    private fun key(name: String, userId: Long) = "${name}_$userId"

    private fun visibleChartsKey(userId: Long) = stringSetPreferencesKey(key("visible_charts", userId))
    private fun favoriteProfilesKey(userId: Long) = stringSetPreferencesKey(key("favorite_profiles", userId))
    private fun flowChartRangeKey(userId: Long) = intPreferencesKey(key("flow_chart_range", userId))
    private fun darkModeKey(userId: Long) = stringPreferencesKey(key("dark_mode", userId))
    private fun appThemeKey(userId: Long) = stringPreferencesKey(key("app_theme", userId))
    private fun paletteStyleKey(userId: Long) = stringPreferencesKey(key("palette_style", userId))
    private fun customSeedColorKey(userId: Long) = intPreferencesKey(key("custom_seed_color", userId))
    private fun fullScreenModeKey(userId: Long) = booleanPreferencesKey(key("full_screen_mode", userId))
    private fun keepScreenOnKey(userId: Long) = booleanPreferencesKey(key("keep_screen_on", userId))
    private fun keepScreenOnOnlyWhileChargingKey(userId: Long) =
        booleanPreferencesKey(key("keep_screen_on_only_while_charging", userId))
    private fun screensaverEnabledKey(userId: Long) = booleanPreferencesKey(key("screensaver_enabled", userId))
    private fun screensaverTimeoutMinutesKey(userId: Long) = intPreferencesKey(key("screensaver_timeout_minutes", userId))
    private fun autoConnectKey(userId: Long) = booleanPreferencesKey(key("auto_connect", userId))
    private fun temperatureUnitKey(userId: Long) = stringPreferencesKey(key("temperature_unit", userId))
    private fun shotDoseKey(userId: Long) = floatPreferencesKey(key("shot_dose_grams", userId))

    companion object {
        private const val DefaultCustomSeedColor = 0xFF1E88E5.toInt()
        private const val DefaultScreensaverTimeoutMinutes = 5
        private const val DEFAULT_SHOT_DOSE_GRAMS = 18f
        private const val MIN_SHOT_DOSE_GRAMS = 0.1f
        private const val MAX_SHOT_DOSE_GRAMS = 100f
    }
}
