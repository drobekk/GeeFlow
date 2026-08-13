package app.geeflow.app.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import app.geeflow.data.user.model.AppPaletteStyle
import app.geeflow.data.user.model.AppTheme
import app.geeflow.domain.user.model.AppearanceSettings
import app.geeflow.platform.getThemeProvider
import app.geeflow.ui.theme.ThemeMode
import app.geeflow.ui.theme.colorscheme.EmeraldSeed
import app.geeflow.ui.theme.colorscheme.EspressoSeed
import app.geeflow.ui.theme.colorscheme.RoseSeed
import app.geeflow.ui.theme.colorscheme.SapphireSeed
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import app.geeflow.data.user.model.ThemeMode as UserThemeMode

@Composable
internal fun rememberAppColorScheme(
    appearance: AppearanceSettings,
    darkMode: Boolean,
): ColorScheme {
    val systemThemeProvider = remember { getThemeProvider() }
    val systemTheme = systemThemeProvider.getSystemColorScheme(darkMode)

    val seedColor = when (appearance.appTheme) {
        AppTheme.ESPRESSO -> EspressoSeed
        AppTheme.SAPPHIRE -> SapphireSeed
        AppTheme.EMERALD -> EmeraldSeed
        AppTheme.ROSE -> RoseSeed
        AppTheme.CUSTOM -> Color(appearance.customSeedColor)
        AppTheme.SYSTEM -> if (systemTheme != null) return systemTheme else EspressoSeed
    }

    return rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = darkMode,
        style = appearance.paletteStyle.toPaletteStyle(),
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
    )
}

internal fun UserThemeMode.toUiThemeMode() = when (this) {
    UserThemeMode.SYSTEM -> ThemeMode.System
    UserThemeMode.LIGHT -> ThemeMode.Light
    UserThemeMode.DARK -> ThemeMode.Dark
}

private fun AppPaletteStyle.toPaletteStyle() = when (this) {
    AppPaletteStyle.TONAL_SPOT -> PaletteStyle.TonalSpot
    AppPaletteStyle.NEUTRAL -> PaletteStyle.Neutral
    AppPaletteStyle.VIBRANT -> PaletteStyle.Vibrant
    AppPaletteStyle.EXPRESSIVE -> PaletteStyle.Expressive
}
