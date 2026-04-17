@file:Suppress("TooManyFunctions")

package dev.drobek.geeflow.data.user

import dev.drobek.geeflow.data.user.model.AppTheme
import dev.drobek.geeflow.data.user.model.ChartType
import dev.drobek.geeflow.data.user.model.DarkMode
import dev.drobek.geeflow.data.user.model.TemperatureUnit
import kotlinx.coroutines.flow.Flow

interface UserSettingsRepository {
    fun visibleCharts(userId: Long): Flow<Set<ChartType>>
    fun darkMode(userId: Long): Flow<DarkMode>
    fun appTheme(userId: Long): Flow<AppTheme>
    fun fullScreenMode(userId: Long): Flow<Boolean>
    fun keepScreenOn(userId: Long): Flow<Boolean>
    fun autoConnect(userId: Long): Flow<Boolean>
    fun temperatureUnit(userId: Long): Flow<TemperatureUnit>
    suspend fun setVisibleCharts(userId: Long, types: Set<ChartType>)
    suspend fun setDarkMode(userId: Long, mode: DarkMode)
    suspend fun setAppTheme(userId: Long, theme: AppTheme)
    suspend fun setFullScreenMode(userId: Long, enabled: Boolean)
    suspend fun setKeepScreenOn(userId: Long, enabled: Boolean)
    suspend fun setAutoConnect(userId: Long, enabled: Boolean)
    suspend fun setTemperatureUnit(userId: Long, unit: TemperatureUnit)
    suspend fun clearUserSettings(userId: Long)
}
