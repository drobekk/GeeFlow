package dev.drobek.geeflow.presentation.feature.device.list

sealed interface DeviceLitViewModelEvent

sealed interface Navigation : DeviceLitViewModelEvent {
    data object Back : Navigation
    data object AddDevice : Navigation
    data class DeviceDetails(val id: String) : Navigation
}
