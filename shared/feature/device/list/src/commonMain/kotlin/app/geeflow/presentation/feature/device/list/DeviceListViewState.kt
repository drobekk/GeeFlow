package app.geeflow.presentation.feature.device.list

data class DeviceListViewState(
    val devices: List<Device> = emptyList(),
    val user: User? = null,
) {
    data class Device(
        val id: Long,
        val name: String,
        val macAddress: String,
        val favourite: Boolean,
    )

    data class User(
        val id: Long,
        val name: String,
    )
}
