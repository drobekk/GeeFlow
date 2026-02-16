package dev.drobek.geeflow.presentation.feature.device.add

internal sealed interface AddDeviceViewModelEvent {
    data class ShowSnackbar(val message: String) : AddDeviceViewModelEvent
}

internal sealed interface Navigation : AddDeviceViewModelEvent {
    object Back : Navigation
    data object DevicesList : Navigation
}
