package dev.drobek.geeflow.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass

expect fun getThemeProvider(): ThemeProvider

@Composable
expect fun calculateWindowSizeClass(): WindowSizeClass

interface ThemeProvider {
    @Composable
    fun getSystemColorScheme(): ColorScheme? = null
}
