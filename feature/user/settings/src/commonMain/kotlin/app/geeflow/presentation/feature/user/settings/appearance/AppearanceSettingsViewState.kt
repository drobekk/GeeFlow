package app.geeflow.presentation.feature.user.settings.appearance

import app.geeflow.data.user.model.AppTheme
import app.geeflow.data.user.model.ThemeMode

data class AppearanceSettingsViewState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
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
