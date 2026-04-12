package dev.drobek.geeflow.platform

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.window.core.layout.WindowSizeClass

actual fun getThemeProvider() = object : ThemeProvider {
    @Composable
    override fun getSystemColorScheme(): ColorScheme? {
        val context = LocalContext.current
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isSystemInDarkTheme()) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        } else {
            null
        }
    }
}

@Composable
actual fun calculateWindowSizeClass(): WindowSizeClass = currentWindowAdaptiveInfo(true).windowSizeClass
