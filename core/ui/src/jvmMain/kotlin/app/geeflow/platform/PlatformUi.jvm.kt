package app.geeflow.platform

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass
import app.geeflow.ui.theme.ThemeMode

actual fun getThemeProvider() = object : ThemeProvider {}

@Composable
actual fun calculateWindowSizeClass(): WindowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass

@Composable
actual fun KeepScreenOnEffect(enabled: Boolean) = Unit

@Composable
actual fun FullScreenEffect(enabled: Boolean) = Unit

@Composable
actual fun ThemeModeEffect(themeMode: ThemeMode) = Unit

@Composable
actual fun rememberLanguageSettingsLauncher(): (() -> Unit)? = null

actual val isFullScreenSupported: Boolean = false

actual val isKeepScreenOnSupported: Boolean = false
