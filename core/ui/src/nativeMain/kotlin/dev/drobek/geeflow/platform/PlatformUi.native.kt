package dev.drobek.geeflow.platform

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.window.core.layout.WindowSizeClass
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIStatusBarAnimation
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.setStatusBarHidden

actual fun getThemeProvider() = object : ThemeProvider {}

@Composable
actual fun calculateWindowSizeClass(): WindowSizeClass = currentWindowAdaptiveInfo(true).windowSizeClass

@Composable
actual fun KeepScreenOnEffect(enabled: Boolean) = Unit

@Composable
actual fun FullScreenEffect(enabled: Boolean) {
    UIApplication.sharedApplication.setStatusBarHidden(
        hidden = enabled,
        withAnimation = UIStatusBarAnimation.UIStatusBarAnimationSlide,
    )
}

@Composable
actual fun ThemeModeEffect(darkTheme: Boolean) {
    UIApplication.sharedApplication.keyWindow?.overrideUserInterfaceStyle = if (darkTheme) {
        UIUserInterfaceStyle.UIUserInterfaceStyleDark
    } else {
        UIUserInterfaceStyle.UIUserInterfaceStyleLight
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
