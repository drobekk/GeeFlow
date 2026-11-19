package app.geeflow.presentation.feature.device.add

import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.SupportedDevice

internal data class AddDeviceViewState(
    val method: Method = Method.NearbyDevices(),
    val dialog: Dialog? = null,
    val manual: ManualEntry = ManualEntry(),
    val qrCodeAvailable: Boolean = true,
) {
    sealed interface Method {
        val changeMethodButtonVisible: Boolean

        data class NearbyDevices(
            override val changeMethodButtonVisible: Boolean = true,
            val showMissingPermissionMessage: Boolean = false,
            val devices: List<DeviceItem> = emptyList(),
        ) : Method

        data object Manual : Method {
            override val changeMethodButtonVisible = true
        }

        data class QrCodeScanner(
            override val changeMethodButtonVisible: Boolean = true,
            val scanningEnabled: Boolean = true,
        ) : Method
    }

    data class DeviceItem(
        val id: String, // BLE peripheral id (transient — only valid until persisted)
        val name: String,
    )

    data class ManualEntry(
        val serialNumber: String = "",
        val modelId: String = SupportedDevice.LitaBA.id,
        val bluetoothAddress: String = "",
        val searchStarted: Boolean = false,
        val searching: Boolean = false,
        val error: ManualError? = null,
        val devices: List<ManualDeviceItem> = emptyList(),
    )

    data class ManualDeviceItem(
        val device: Device,
        val serialMatches: Boolean,
        val modelCompatible: Boolean,
    )

    enum class ManualError { SerialRequired, InvalidSerial, InvalidAddress, PermissionDenied, ScanFailed }

    sealed interface Dialog {
        data object ManualModelSelection : Dialog
        data class ManualConfirmation(val device: Device, val serialMatches: Boolean) : Dialog
        data class ModelSelection(val device: Device) : Dialog
        data class ExperimentalWarning(val device: Device) : Dialog
    }
}
