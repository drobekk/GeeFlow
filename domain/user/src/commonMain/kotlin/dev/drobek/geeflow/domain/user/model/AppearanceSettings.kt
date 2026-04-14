package dev.drobek.geeflow.domain.user.model

import dev.drobek.geeflow.data.user.model.AppTheme
import dev.drobek.geeflow.data.user.model.DarkMode

data class AppearanceSettings(
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val appTheme: AppTheme = AppTheme.ESPRESSO,
    val fullScreenMode: Boolean = false,
    val keepScreenOn: Boolean = false,
)
