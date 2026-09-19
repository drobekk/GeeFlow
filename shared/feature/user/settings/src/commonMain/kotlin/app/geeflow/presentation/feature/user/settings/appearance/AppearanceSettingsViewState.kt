package app.geeflow.presentation.feature.user.settings.appearance

import app.geeflow.data.user.model.AppPaletteStyle
import app.geeflow.data.user.model.AppTheme
import app.geeflow.data.user.model.ThemeMode

data class AppearanceSettingsViewState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appTheme: AppTheme = AppTheme.ESPRESSO,
    val paletteStyle: AppPaletteStyle = AppPaletteStyle.TONAL_SPOT,
    val customSeedColor: Int = 0xFF1E88E5.toInt(),
    val fullScreenMode: Boolean = false,
    val keepScreenOn: Boolean = false,
    val screensaverEnabled: Boolean = false,
    val screensaverTimeoutMinutes: Int = 5,
    val dialog: Dialog? = null,
) {
    sealed interface Dialog {
        data object DarkMode : Dialog
        data object AppTheme : Dialog
        data object PaletteStyle : Dialog
        data object CustomColor : Dialog
        data object ScreensaverTimeout : Dialog
    }
}
