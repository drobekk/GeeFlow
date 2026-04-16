package dev.drobek.geeflow

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.drobek.geeflow.app.App
import dev.drobek.geeflow.ui.icons.AppLogo
import dev.drobek.geeflow.ui.icons.GeeFlowIcon
import geeflow.core.ui.generated.resources.Res
import geeflow.core.ui.generated.resources.app_name
import org.jetbrains.compose.resources.stringResource

fun main() = application {
    var isFullscreen by remember { mutableStateOf(false) }
    val state = rememberWindowState(placement = WindowPlacement.Floating, size = DpSize(1000.dp, 650.dp))

    key(isFullscreen) {
        Window(
            onCloseRequest = ::exitApplication,
            state = state,
            title = stringResource(Res.string.app_name),
            icon = rememberVectorPainter(GeeFlowIcon.AppLogo),
            undecorated = isFullscreen,
            onKeyEvent = { event ->
                handleFullscreenKey(event) {
                    isFullscreen = !isFullscreen
                    state.placement = if (isFullscreen) {
                        WindowPlacement.Fullscreen
                    } else {
                        WindowPlacement.Floating
                    }
                }
            },
        ) {
            App { exitApplication() }
        }
    }
}

private fun handleFullscreenKey(
    event: KeyEvent,
    onToggle: () -> Unit,
): Boolean = if (event.isAltPressed && event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
    onToggle()
    true
} else {
    false
}
