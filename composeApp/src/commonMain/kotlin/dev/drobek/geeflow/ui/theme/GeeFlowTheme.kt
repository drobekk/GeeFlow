package dev.drobek.geeflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.drobek.geeflow.platform.getThemeProvider
import dev.drobek.geeflow.ui.isWidthExpanded

@Composable
fun GeeFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useSystemTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemThemeProvider = remember { getThemeProvider() }
    val systemTheme = systemThemeProvider.getSystemColorScheme()
    val spacing = if (isWidthExpanded()) expandedSpacing() else compactSpacing()
    val colorScheme = when {
        useSystemTheme && systemTheme != null && !isPreview -> systemTheme
        darkTheme -> darkScheme
        else -> lightScheme
    }
    val customColors = if (darkTheme) darkCustomColors else lightCustomColors

    CompositionLocalProvider(
        LocalSpacing provides spacing,
        LocalColors provides customColors
    ) {
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
}

object GeeFlowTheme {
    val spacing: Spacing
        @Composable
        get() = LocalSpacing.current

    val colors: Colors
        @Composable
        get() = LocalColors.current
}
