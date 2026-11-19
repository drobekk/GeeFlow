package app.geeflow.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Shared machine workspace palette; retains the user's light or dark mode. */
@Composable
fun ClearScreenTheme(enabled: Boolean = true, content: @Composable () -> Unit) {
    val original = MaterialTheme.colorScheme
    val scheme = remember(original, enabled) { if (enabled) original.clearColorScheme() else original }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalContentColor provides scheme.onSurface, content = content)
    }
}

@Suppress("MagicNumber") // Named palette fields use ARGB color literals.
fun ColorScheme.clearColorScheme(): androidx.compose.material3.ColorScheme {
    val dark = background.luminance() < 0.5f
    return if (dark) {
        copy(
            background = Color(0xFF15191E),
            surface = Color(0xFF1B2026),
            surfaceContainerLow = Color(0xFF20262D),
            surfaceContainer = Color(0xFF252C33),
            surfaceContainerHigh = Color(0xFF2D353D),
            surfaceContainerHighest = Color(0xFF37414B),
            onSurface = Color(0xFFF0F3F6),
            onSurfaceVariant = Color(0xFFC2CBD4),
            outlineVariant = Color(0xFF596570),
            primary = Color(0xFF8DBBFF),
            onPrimary = Color(0xFF10243D),
            primaryContainer = Color(0xFF2A333B),
            onPrimaryContainer = Color(0xFFE0E6EB),
            secondaryContainer = Color(0xFF2A333B),
            onSecondaryContainer = Color(0xFFE0E6EB),
            tertiary = Color(0xFF7ED5A2),
            onTertiary = Color(0xFF0D2A1A),
        )
    } else {
        copy(
            background = Color(0xFFF3F5F7),
            surface = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFFFFFFF),
            surfaceContainer = Color(0xFFF8FAFC),
            surfaceContainerHigh = Color(0xFFE9EEF3),
            surfaceContainerHighest = Color(0xFFE0E7ED),
            onSurface = Color(0xFF17212B),
            onSurfaceVariant = Color(0xFF475666),
            outlineVariant = Color(0xFFB8C3CE),
            error = Color(0xFFB42318),
            onError = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFEAF0F5),
            onSecondaryContainer = Color(0xFF245C9A),
            primaryContainer = Color(0xFFEAF0F5),
            onPrimaryContainer = Color(0xFF17212B),
            primary = Color(0xFF245C9A),
            onPrimary = Color(0xFFFFFFFF),
            tertiary = Color(0xFF26734D),
            onTertiary = Color(0xFFFFFFFF),
        )
    }
}
