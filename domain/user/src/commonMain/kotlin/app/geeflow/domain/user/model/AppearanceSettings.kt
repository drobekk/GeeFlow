package app.geeflow.domain.user.model

import app.geeflow.data.user.model.AppPaletteStyle
import app.geeflow.data.user.model.AppTheme
import app.geeflow.data.user.model.ThemeMode

data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appTheme: AppTheme = AppTheme.ESPRESSO,
    val paletteStyle: AppPaletteStyle = AppPaletteStyle.TONAL_SPOT,
    val customSeedColor: Int = 0xFF1E88E5.toInt(),
    val fullScreenMode: Boolean = false,
    val keepScreenOn: Boolean = false,
)
