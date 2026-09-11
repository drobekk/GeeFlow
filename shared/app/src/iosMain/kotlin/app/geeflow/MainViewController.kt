package app.geeflow

import androidx.compose.ui.window.ComposeUIViewController
import app.geeflow.app.App
import platform.posix.exit

@Suppress("FunctionNaming")
fun MainViewController() = ComposeUIViewController {
    App { exit(0) }
}
