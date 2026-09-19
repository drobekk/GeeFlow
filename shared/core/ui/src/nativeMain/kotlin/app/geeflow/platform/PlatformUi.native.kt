package app.geeflow.platform

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.window.core.layout.WindowSizeClass
import app.geeflow.ui.theme.ThemeMode
import kotlinx.coroutines.delay
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIStatusBarAnimation
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.setStatusBarHidden
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

actual fun getThemeProvider() = object : ThemeProvider {}

@Composable
actual fun calculateWindowSizeClass(): WindowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass

@Composable
actual fun KeepScreenOnEffect(enabled: Boolean) {
    DisposableEffect(enabled) {
        val previous = UIApplication.sharedApplication.idleTimerDisabled
        UIApplication.sharedApplication.idleTimerDisabled = enabled
        onDispose { UIApplication.sharedApplication.idleTimerDisabled = previous }
    }
}

@Composable
actual fun FullScreenEffect(enabled: Boolean) {
    UIApplication.sharedApplication.setStatusBarHidden(
        hidden = enabled,
        withAnimation = UIStatusBarAnimation.UIStatusBarAnimationSlide,
    )
}

@Composable
actual fun ThemeModeEffect(themeMode: ThemeMode) {
    UIApplication.sharedApplication.keyWindow?.overrideUserInterfaceStyle = when (themeMode) {
        ThemeMode.System -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
        ThemeMode.Light -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
        ThemeMode.Dark -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
    }
}

@Composable
actual fun rememberLanguageSettingsLauncher(): (() -> Unit)? = remember {
    {
        NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let { url ->
            UIApplication.sharedApplication.openURL(
                url = url,
                options = emptyMap<Any?, Any?>(),
                completionHandler = null,
            )
        }
    }
}

actual val isFullScreenSupported: Boolean = true

actual val isKeepScreenOnSupported: Boolean = true

@Composable
actual fun rememberFormattedTime(): String {
    var time by remember { mutableStateOf(getNativeFormattedTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            time = getNativeFormattedTime()
            delay(1.seconds)
        }
    }
    return time
}

@Composable
actual fun rememberFormattedDate(): String {
    var date by remember { mutableStateOf(getNativeFormattedDate()) }
    LaunchedEffect(Unit) {
        while (true) {
            date = getNativeFormattedDate()
            delay(1.minutes)
        }
    }
    return date
}

private fun getNativeFormattedTime(): String {
    val formatter = NSDateFormatter().apply {
        timeStyle = NSDateFormatterShortStyle
        dateStyle = NSDateFormatterNoStyle
    }
    return formatter.stringFromDate(NSDate())
}

private fun getNativeFormattedDate(): String {
    val formatter = NSDateFormatter().apply {
        timeStyle = NSDateFormatterNoStyle
        setLocalizedDateFormatFromTemplate("EEEE, d MMMM")
    }
    return formatter.stringFromDate(NSDate())
}
