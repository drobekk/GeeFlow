package app.geeflow.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.geeflow.domain.device.ProfileExecutionCoordinator
import app.geeflow.platform.KeepScreenOnEffect
import org.koin.compose.koinInject

@Composable
internal fun ProfileExecutionEffect(keepScreenOn: Boolean) {
    val coordinator = koinInject<ProfileExecutionCoordinator>()
    val run by coordinator.state.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, coordinator) {
        coordinator.setForeground(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) coordinator.setForeground(false)
            if (event == Lifecycle.Event.ON_START) coordinator.setForeground(true)
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            coordinator.setForeground(false)
        }
    }
    KeepScreenOnEffect(keepScreenOn || run.active || run.stopPending)
}
