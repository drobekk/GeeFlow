package dev.drobek.geeflow.platform

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass

actual fun getThemeProvider() = object : ThemeProvider {}

@Composable
actual fun calculateWindowSizeClass(): WindowSizeClass {
    return currentWindowAdaptiveInfo(true).windowSizeClass
}
