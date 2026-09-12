package app.geeflow.platform

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.window.core.layout.WindowSizeClass
import app.geeflow.ui.theme.ThemeMode
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIStatusBarAnimation
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.setStatusBarHidden

actual fun getThemeProvider() = object : ThemeProvider {}

@Composable
actual fun calculateWindowSizeClass(): WindowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass

@Composable
actual fun KeepScreenOnEffect(enabled: Boolean) {
    androidx.compose.runtime.DisposableEffect(enabled) {
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
