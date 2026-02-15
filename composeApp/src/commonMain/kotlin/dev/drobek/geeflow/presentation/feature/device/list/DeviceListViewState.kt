package dev.drobek.geeflow.presentation.feature.device.list

data class DeviceListViewState(
    val devices: List<Device> = emptyList()
) {
    data class Device(
        val id: String,
        val name: String
    )
}
