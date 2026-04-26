package app.geeflow

import androidx.compose.ui.window.ComposeUIViewController
import app.geeflow.app.App
import kotlin.system.exitProcess

@Suppress("FunctionNaming")
fun MainViewController() = ComposeUIViewController { App { exitProcess(0) } }
