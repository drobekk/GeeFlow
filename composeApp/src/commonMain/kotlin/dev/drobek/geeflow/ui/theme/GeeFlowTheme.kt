package dev.drobek.geeflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.drobek.geeflow.platform.getThemeProvider

@Composable
fun GeeFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useSystemTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemThemeProvider = remember { getThemeProvider() }
    val systemTheme = systemThemeProvider.getSystemColorScheme()
    val colorScheme = when {
        useSystemTheme && systemTheme != null && !isPreview -> systemTheme
        darkTheme -> darkScheme
        else -> lightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = geeFlowTypography(),
        content = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
                content = content
            )
        }
    )
}
