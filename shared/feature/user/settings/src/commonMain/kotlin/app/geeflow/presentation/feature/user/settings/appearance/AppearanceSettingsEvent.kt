package app.geeflow.presentation.feature.user.settings.appearance

import app.geeflow.data.user.model.AppPaletteStyle
import app.geeflow.data.user.model.AppTheme
import app.geeflow.data.user.model.ThemeMode

sealed interface AppearanceSettingsEvent {
    data object BackClicked : AppearanceSettingsEvent
    data object DarkModeClicked : AppearanceSettingsEvent
    data object AppThemeClicked : AppearanceSettingsEvent
    data object PaletteStyleClicked : AppearanceSettingsEvent
    data object DialogDismissed : AppearanceSettingsEvent
    data class DarkModeChanged(val mode: ThemeMode) : AppearanceSettingsEvent
    data class AppThemeChanged(val theme: AppTheme) : AppearanceSettingsEvent
    data class PaletteStyleChanged(val style: AppPaletteStyle) : AppearanceSettingsEvent
    data object CustomColorClicked : AppearanceSettingsEvent
    data class CustomColorChanged(val colorArgb: Int) : AppearanceSettingsEvent
    data class CustomColorConfirmed(val colorArgb: Int) : AppearanceSettingsEvent
    data class FullScreenChanged(val enabled: Boolean) : AppearanceSettingsEvent
    data class KeepScreenOnChanged(val enabled: Boolean) : AppearanceSettingsEvent
    data class ScreensaverChanged(val enabled: Boolean) : AppearanceSettingsEvent
    data object ScreensaverTimeoutClicked : AppearanceSettingsEvent
    data class ScreensaverTimeoutConfirmed(val minutes: Int) : AppearanceSettingsEvent
}
