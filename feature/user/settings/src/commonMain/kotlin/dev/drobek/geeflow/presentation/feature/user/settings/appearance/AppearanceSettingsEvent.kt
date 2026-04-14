package dev.drobek.geeflow.presentation.feature.user.settings.appearance

import dev.drobek.geeflow.data.user.model.AppTheme
import dev.drobek.geeflow.data.user.model.DarkMode

sealed interface AppearanceSettingsEvent {
    data object BackClicked : AppearanceSettingsEvent
    data object DarkModeClicked : AppearanceSettingsEvent
    data object AppThemeClicked : AppearanceSettingsEvent
    data object DialogDismissed : AppearanceSettingsEvent
    data class DarkModeChanged(val mode: DarkMode) : AppearanceSettingsEvent
    data class AppThemeChanged(val theme: AppTheme) : AppearanceSettingsEvent
    data class FullScreenChanged(val enabled: Boolean) : AppearanceSettingsEvent
    data class KeepScreenOnChanged(val enabled: Boolean) : AppearanceSettingsEvent
}
