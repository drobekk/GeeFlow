package dev.drobek.geeflow.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

expect fun getThemeProvider() : ThemeProvider

interface ThemeProvider {
    @Composable
    fun getSystemColorScheme(): ColorScheme? = null
}
