package app.geeflow.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass
import app.geeflow.ui.theme.ThemeMode

expect fun getThemeProvider(): ThemeProvider

@Composable
expect fun calculateWindowSizeClass(): WindowSizeClass

@Composable
expect fun KeepScreenOnEffect(enabled: Boolean)

@Composable
expect fun FullScreenEffect(enabled: Boolean)

@Composable
expect fun ThemeModeEffect(themeMode: ThemeMode)

@Composable
expect fun rememberLanguageSettingsLauncher(): (() -> Unit)?

expect val isFullScreenSupported: Boolean

expect val isKeepScreenOnSupported: Boolean

interface ThemeProvider {
    @Composable
    fun getSystemColorScheme(darkTheme: Boolean): ColorScheme? = null
}
