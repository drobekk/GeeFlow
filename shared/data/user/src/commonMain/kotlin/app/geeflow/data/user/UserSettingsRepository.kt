@file:Suppress("TooManyFunctions")

package app.geeflow.data.user

import app.geeflow.data.user.model.AppPaletteStyle
import app.geeflow.data.user.model.AppTheme
import app.geeflow.data.user.model.ChartType
import app.geeflow.data.user.model.TemperatureUnit
import app.geeflow.data.user.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface UserSettingsRepository {
    fun visibleCharts(userId: Long): Flow<Set<ChartType>>
    fun darkMode(userId: Long): Flow<ThemeMode>
    fun appTheme(userId: Long): Flow<AppTheme>
    fun paletteStyle(userId: Long): Flow<AppPaletteStyle>
    fun customSeedColor(userId: Long): Flow<Int>
    fun fullScreenMode(userId: Long): Flow<Boolean>
    fun keepScreenOn(userId: Long): Flow<Boolean>
    fun autoConnect(userId: Long): Flow<Boolean>
    fun temperatureUnit(userId: Long): Flow<TemperatureUnit>
    suspend fun setVisibleCharts(userId: Long, types: Set<ChartType>)
    suspend fun setDarkMode(userId: Long, mode: ThemeMode)
    suspend fun setAppTheme(userId: Long, theme: AppTheme)
    suspend fun setPaletteStyle(userId: Long, style: AppPaletteStyle)
    suspend fun setCustomSeedColor(userId: Long, colorArgb: Int)
    suspend fun setFullScreenMode(userId: Long, enabled: Boolean)
    suspend fun setKeepScreenOn(userId: Long, enabled: Boolean)
    suspend fun setAutoConnect(userId: Long, enabled: Boolean)
    suspend fun setTemperatureUnit(userId: Long, unit: TemperatureUnit)
    suspend fun clearUserSettings(userId: Long)
}
