package app.geeflow.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import app.geeflow.ui.isWidthExpanded
import app.geeflow.ui.theme.colorscheme.EspressoSeed
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

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
        colorScheme = rememberDynamicColorScheme(
            seedColor = EspressoSeed,
            isDark = darkMode,
            style = PaletteStyle.TonalSpot,
        ),
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
