package app.geeflow.presentation.feature.device.add

import app.geeflow.data.device.model.Device

internal data class AddDeviceViewState(
    val method: Method = Method.NearbyDevices(),
    val dialog: Dialog? = null,
) {
    sealed interface Method {
        val changeMethodButtonVisible: Boolean

        data class NearbyDevices(
            override val changeMethodButtonVisible: Boolean = true,
            val showMissingPermissionMessage: Boolean = false,
            val devices: List<DeviceItem> = emptyList(),
        ) : Method

        data class QrCodeScanner(
            override val changeMethodButtonVisible: Boolean = true,
            val scanningEnabled: Boolean = true,
        ) : Method
    }

    data class DeviceItem(
        val id: String, // BLE peripheral id (transient — only valid until persisted)
        val name: String,
    )

    sealed interface Dialog {
        data class ExperimentalWarning(val device: Device) : Dialog
    }
}
