package app.geeflow.platform

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo
import platform.UIKit.UIApplication

@Composable
internal actual fun platformContentInsets(): WindowInsets {
    // Re-evaluate UIKit visibility after rotation. Keep cutout protection in fullscreen.
    LocalWindowInfo.current.containerSize
    val statusBar = if (UIApplication.sharedApplication.statusBarHidden) {
        WindowInsets(0, 0, 0, 0)
    } else {
        WindowInsets.statusBars.only(WindowInsetsSides.Top)
    }
    // Preserve GeeFlow's home-indicator policy without symmetric landscape safe-area margins.
    return statusBar.union(WindowInsets.displayCutout)
}
