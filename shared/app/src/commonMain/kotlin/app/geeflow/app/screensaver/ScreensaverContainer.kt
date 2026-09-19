package app.geeflow.app.screensaver

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@Composable
internal fun ScreensaverContainer(
    enabled: Boolean,
    timeoutMinutes: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var isScreensaverVisible by remember { mutableStateOf(false) }
    var lastInteraction by remember { mutableStateOf(TimeSource.Monotonic.markNow()) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                lastInteraction = TimeSource.Monotonic.markNow()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(enabled, timeoutMinutes) {
        if (!enabled) {
            isScreensaverVisible = false
            return@LaunchedEffect
        }
        val timeout = timeoutMinutes.minutes
        while (true) {
            delay(1.seconds)
            if (!isScreensaverVisible && lastInteraction.elapsedNow() >= timeout) {
                isScreensaverVisible = true
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                            lastInteraction = TimeSource.Monotonic.markNow()
                        }
                    }
                },
        ) {
            content()
        }

        AnimatedVisibility(
            visible = isScreensaverVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Screensaver(
                onDismiss = {
                    lastInteraction = TimeSource.Monotonic.markNow()
                    isScreensaverVisible = false
                },
            )
        }
    }
}
