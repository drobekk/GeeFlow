package dev.drobek.geeflow.presentation.feature.device.list

import dev.drobek.geeflow.presentation.feature.device.list.DeviceListViewState.Device

sealed interface DeviceListEvent {
    data class DeviceClicked(val device: Device) : DeviceListEvent
    data class DeviceRemoveClicked(val device: Device) : DeviceListEvent
    data class DeviceSetAsDefaultClicked(val device: Device) : DeviceListEvent
    data object AddDeviceClicked : DeviceListEvent
    data object BackClicked : DeviceListEvent
}

