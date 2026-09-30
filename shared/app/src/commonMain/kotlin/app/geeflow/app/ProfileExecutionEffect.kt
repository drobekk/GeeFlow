package app.geeflow.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceState.BrewStatus
import app.geeflow.data.device.model.DeviceState.ConnectionStatus
import app.geeflow.domain.device.AutoFlushCoordinator
import app.geeflow.domain.device.ProfileExecutionCoordinator
import app.geeflow.platform.KeepScreenOnEffect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

@Composable
@OptIn(ExperimentalCoroutinesApi::class)
internal fun ProfileExecutionEffect(keepScreenOn: Boolean) {
    val coordinator = koinInject<ProfileExecutionCoordinator>()
    val autoFlush = koinInject<AutoFlushCoordinator>()
    val provider = koinInject<DeviceControllerProvider>()
    val run by coordinator.state.collectAsState()
    val countdown by autoFlush.countdown.collectAsState()
    val machineActive by remember(provider) {
        provider.currentDeviceId.flatMapLatest { id ->
            if (id == null) {
                flowOf(false)
            } else {
                provider.getController(id).deviceState.map { state ->
                    state.connectionStatus == ConnectionStatus.Connected && state.brewStatus != BrewStatus.Idle
                }
            }
        }
    }.collectAsState(initial = false)

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

    KeepScreenOnEffect(keepScreenOn || run.active || run.stopPending || machineActive || countdown != null)
}
