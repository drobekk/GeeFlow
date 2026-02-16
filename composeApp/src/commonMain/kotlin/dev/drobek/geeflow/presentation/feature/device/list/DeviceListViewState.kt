package dev.drobek.geeflow.presentation.feature.device.list

data class DeviceListViewState(
    val devices: List<Device> = emptyList(),
    val user: User? = null
) {
    data class Device(
        val id: String,
        val name: String,
        val favourite: Boolean
    )

    data class User(
        val id: Long,
        val name: String
    )
}
