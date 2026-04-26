package app.geeflow.platform

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass

actual fun getThemeProvider() = object : ThemeProvider {}

@Composable
actual fun calculateWindowSizeClass(): WindowSizeClass = currentWindowAdaptiveInfo(true).windowSizeClass

@Composable
actual fun KeepScreenOnEffect(enabled: Boolean) = Unit

@Composable
actual fun FullScreenEffect(enabled: Boolean) = Unit

@Composable
actual fun ThemeModeEffect(darkTheme: Boolean) = Unit

@Composable
actual fun rememberLanguageSettingsLauncher(): (() -> Unit)? = null

actual val isFullScreenSupported: Boolean = false

actual val isKeepScreenOnSupported: Boolean = false
