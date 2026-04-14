package dev.drobek.geeflow.data.user

import dev.drobek.geeflow.data.user.model.AppTheme
import dev.drobek.geeflow.data.user.model.ChartType
import dev.drobek.geeflow.data.user.model.DarkMode
import kotlinx.coroutines.flow.Flow

interface UserSettingsRepository {
    val visibleCharts: Flow<Set<ChartType>>
    val darkMode: Flow<DarkMode>
    val appTheme: Flow<AppTheme>
    val fullScreenMode: Flow<Boolean>
    val keepScreenOn: Flow<Boolean>
    suspend fun setVisibleCharts(types: Set<ChartType>)
    suspend fun setDarkMode(mode: DarkMode)
    suspend fun setAppTheme(theme: AppTheme)
    suspend fun setFullScreenMode(enabled: Boolean)
    suspend fun setKeepScreenOn(enabled: Boolean)
}
