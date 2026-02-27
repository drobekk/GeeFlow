package dev.drobek.geeflow.presentation.feature.device.add

internal data class AddDeviceViewState(
    val method: Method = Method.NearbyDevices()
) {
    sealed interface Method {
        val changeMethodButtonVisible: Boolean

        data class NearbyDevices(
            override val changeMethodButtonVisible: Boolean = true,
            val showMissingPermissionMessage: Boolean = false,
            val devices: List<DeviceItem> = emptyList()
        ) : Method

        data class QrCodeScanner(
            override val changeMethodButtonVisible: Boolean = true,
            val scanningEnabled: Boolean = true
        ) : Method
    }

    data class DeviceItem(
        val id: String, // Mac address
        val name: String
    )
}
