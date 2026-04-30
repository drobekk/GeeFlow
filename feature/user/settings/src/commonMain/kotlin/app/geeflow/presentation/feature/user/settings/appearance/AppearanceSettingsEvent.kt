package app.geeflow.presentation.feature.user.settings.appearance

import app.geeflow.data.user.model.AppTheme
import app.geeflow.data.user.model.ThemeMode

sealed interface AppearanceSettingsEvent {
    data object BackClicked : AppearanceSettingsEvent
    data object DarkModeClicked : AppearanceSettingsEvent
    data object AppThemeClicked : AppearanceSettingsEvent
    data object DialogDismissed : AppearanceSettingsEvent
    data class DarkModeChanged(val mode: ThemeMode) : AppearanceSettingsEvent
    data class AppThemeChanged(val theme: AppTheme) : AppearanceSettingsEvent
    data class FullScreenChanged(val enabled: Boolean) : AppearanceSettingsEvent
    data class KeepScreenOnChanged(val enabled: Boolean) : AppearanceSettingsEvent
}
