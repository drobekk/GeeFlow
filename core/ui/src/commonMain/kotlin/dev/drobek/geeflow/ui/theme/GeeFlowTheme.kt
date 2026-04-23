package dev.drobek.geeflow.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import dev.drobek.geeflow.ui.isWidthExpanded
import dev.drobek.geeflow.ui.theme.colorscheme.espressoColorScheme

@Composable
fun GeeFlowTheme(
    darkMode: Boolean,
    colorScheme: ColorScheme,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val spacing = if (isWidthExpanded()) expandedSpacing() else compactSpacing()
    val customColors = if (darkMode) darkCustomColors else lightCustomColors

    CompositionLocalProvider(
        LocalSpacing provides spacing,
        LocalColors provides customColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = geeFlowTypography(),
            content = {
                Surface(
                    modifier = modifier,
                    color = colorScheme.surface,
                    content = content,
                )
            },
        )
    }
}

@Composable
fun GeeFlowThemePreview(
    darkMode: Boolean,
    content: @Composable () -> Unit,
) {
    GeeFlowTheme(
        darkMode = darkMode,
        colorScheme = espressoColorScheme(darkMode),
        content = content,
    )
}

object GeeFlowTheme {
    val spacing: GeeFlowSpacing
        @Composable
        get() = LocalSpacing.current

    val colors: Colors
        @Composable
        get() = LocalColors.current
}
