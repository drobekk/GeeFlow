package app.geeflow.app.autoflush

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.core.presentation.toUserMessage
import app.geeflow.domain.device.AutoFlushCoordinator
import app.geeflow.ui.GeeFlowInsets
import org.koin.compose.koinInject

@Composable
internal fun AutoFlushHost() {
    val coordinator = koinInject<AutoFlushCoordinator>()
    val countdown by coordinator.countdown.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle, coordinator) {
        coordinator.setForeground(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        val observer = LifecycleEventObserver { _, _ ->
            coordinator.setForeground(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            coordinator.setForeground(false)
        }
    }
    LaunchedEffect(coordinator) {
        coordinator.errors.collect { error ->
            snackbarState.currentSnackbarData?.dismiss()
            snackbarState.showSnackbar(error.toUserMessage())
        }
    }

    countdown?.let {
        AutoFlushDialog(countdown = it, onCancel = coordinator::cancel)
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        SnackbarHost(
            hostState = snackbarState,
            modifier = Modifier.windowInsetsPadding(GeeFlowInsets.bottom).padding(16.dp),
        )
    }
}
