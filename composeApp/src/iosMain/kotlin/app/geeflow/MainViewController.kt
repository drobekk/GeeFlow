package app.geeflow

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.window.ComposeUIViewController
import app.geeflow.app.App
import platform.UIKit.UIApplication
import platform.posix.exit

@Suppress("FunctionNaming")
fun MainViewController() = ComposeUIViewController {
    val statusBarInsetsToConsume = if (isStatusBarVisible()) {
        WindowInsets()
    } else {
        WindowInsets.statusBars
    }

    Box(
        Modifier
            .fillMaxSize()
            .consumeWindowInsets(WindowInsets.navigationBars)
            .consumeWindowInsets(statusBarInsetsToConsume),
    ) {
        App { exit(0) }
    }
}

@Composable
private fun isStatusBarVisible(): Boolean {
    // Force recomposition when window size changes, which can indicate a change in status bar visibility
    LocalWindowInfo.current.containerSize
    val topCutout = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding().value > 0f
    val isSystemHidden = UIApplication.sharedApplication.statusBarHidden

    return !isSystemHidden || topCutout
}
