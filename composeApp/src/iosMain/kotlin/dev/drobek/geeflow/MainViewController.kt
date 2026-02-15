package dev.drobek.geeflow

import androidx.compose.ui.window.ComposeUIViewController
import dev.drobek.geeflow.app.App
import kotlin.system.exitProcess

fun MainViewController() = ComposeUIViewController { App { exitProcess(0) } }
