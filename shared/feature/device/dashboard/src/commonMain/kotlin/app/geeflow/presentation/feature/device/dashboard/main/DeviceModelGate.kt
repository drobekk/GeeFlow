package app.geeflow.presentation.feature.device.dashboard.main

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.data.device.model.SupportedDevice
import app.geeflow.data.device.model.supportedDevice
import app.geeflow.domain.device.usecase.ObserveDeviceUseCase
import app.geeflow.domain.device.usecase.SetDeviceModelUseCase
import app.geeflow.ui.components.DeviceModelPicker
import app.geeflow.ui.components.GeeFlowDialog
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import org.koin.core.parameter.parametersOf

internal data class DeviceModelState(val loaded: Boolean = false, val model: SupportedDevice? = null)

@KoinViewModel
internal class DeviceModelViewModel(
    @InjectedParam private val deviceId: Long,
    observe: ObserveDeviceUseCase,
    private val selectModel: SetDeviceModelUseCase,
) : BaseViewModel<DeviceModelState, Unit>(DeviceModelState()) {
    init {
        launch { observe(deviceId).collect { modify { copy(loaded = true, model = it?.supportedDevice) } } }
    }
    fun select(id: String) = launch { selectModel(deviceId, id) }
}

@Composable
internal fun DeviceModelGate(deviceId: Long, onClose: () -> Unit, content: @Composable (String) -> Unit) {
    val viewModel = koinViewModel<DeviceModelViewModel>(key = "model-$deviceId") { parametersOf(deviceId) }
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val model = state.model
    when {
        !state.loaded -> CircularProgressIndicator()
        model != null -> content(model.id)
        else -> GeeFlowDialog(onDismissRequest = onClose) {
            DeviceModelPicker(
                models = SupportedDevice
                    .all()
                    .filter { it != SupportedDevice.GeeFlowDemo }
                    .map { it.id to "${it.manufacturer} ${it.model}" },
                onSelect = viewModel::select,
                onClose = onClose,
            )
        }
    }
}
