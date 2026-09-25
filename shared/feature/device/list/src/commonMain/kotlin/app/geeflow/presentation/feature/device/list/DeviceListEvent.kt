package app.geeflow.presentation.feature.device.list

import app.geeflow.presentation.feature.device.list.DeviceListViewState.Device

sealed interface DeviceListEvent {
    data class DeviceClicked(val device: Device) : DeviceListEvent
    data class DeviceRemoveClicked(val device: Device) : DeviceListEvent
    data class DeviceRenameClicked(val device: Device) : DeviceListEvent
    data class DeviceRenameConfirmed(val device: Device, val name: String) : DeviceListEvent
    data class DeviceSetAsDefaultClicked(val device: Device) : DeviceListEvent
    data object DialogDismissed : DeviceListEvent
    data object AddDeviceClicked : DeviceListEvent
    data object BackClicked : DeviceListEvent
}
