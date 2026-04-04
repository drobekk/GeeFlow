package dev.drobek.geeflow.presentation.feature.device.add

internal sealed interface AddDeviceViewModelEvent {
    data class ShowSnackbar(val message: String) : AddDeviceViewModelEvent
}
