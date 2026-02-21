package dev.drobek.geeflow.presentation.feature.device.dashboard

sealed interface DeviceLitViewModelEvent

sealed interface Navigation : DeviceLitViewModelEvent {
    data object Back : Navigation
    data object DeviceList : Navigation
}
