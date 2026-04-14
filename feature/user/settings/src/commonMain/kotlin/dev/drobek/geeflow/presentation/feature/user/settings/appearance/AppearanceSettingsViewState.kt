package dev.drobek.geeflow.presentation.feature.user.settings.appearance

import dev.drobek.geeflow.data.user.model.AppTheme
import dev.drobek.geeflow.data.user.model.DarkMode

data class AppearanceSettingsViewState(
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val appTheme: AppTheme = AppTheme.ESPRESSO,
    val fullScreenMode: Boolean = false,
    val keepScreenOn: Boolean = false,
    val dialog: Dialog? = null,
) {
    sealed interface Dialog {
        data object DarkMode : Dialog
        data object AppTheme : Dialog
    }
}
