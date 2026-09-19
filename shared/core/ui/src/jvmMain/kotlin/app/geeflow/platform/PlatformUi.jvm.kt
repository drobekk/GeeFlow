package app.geeflow.platform

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.window.core.layout.WindowSizeClass
import app.geeflow.ui.theme.ThemeMode
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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

@Composable
actual fun rememberFormattedTime(): String {
    var time by remember { mutableStateOf(getJvmFormattedTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            time = getJvmFormattedTime()
            delay(1.seconds)
        }
    }
    return time
}

@Composable
actual fun rememberFormattedDate(): String {
    var date by remember { mutableStateOf(getJvmFormattedDate()) }
    LaunchedEffect(Unit) {
        while (true) {
            date = getJvmFormattedDate()
            delay(1.minutes)
        }
    }
    return date
}

private fun getJvmFormattedTime(): String {
    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    return now.format(formatter)
}

private fun getJvmFormattedDate(): String {
    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())
    return now.format(formatter)
}
