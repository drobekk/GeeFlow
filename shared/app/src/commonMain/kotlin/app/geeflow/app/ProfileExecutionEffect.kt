package app.geeflow.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.geeflow.domain.device.ProfileExecutionCoordinator
import app.geeflow.platform.KeepScreenOnEffect
import geeflow.shared.core.ui.generated.resources.Res
import geeflow.shared.core.ui.generated.resources.profile_manual_stop_required
import org.jetbrains.compose.resources.stringResource
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
    KeepScreenOnEffect(keepScreenOn || run.active)
    if (run.manualStopRequired) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.padding(24.dp)) {
                Text(
                    stringResource(Res.string.profile_manual_stop_required),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
