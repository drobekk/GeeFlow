package app.geeflow.presentation.feature.device.add

internal sealed interface AddDeviceEvent {
    data class ModelSelected(val modelId: String) : AddDeviceEvent
    sealed interface ManualEvent : AddDeviceEvent
    data object ShowManualEntryClicked : ManualEvent
    data class SerialNumberChanged(val value: String) : ManualEvent
    data class BluetoothAddressChanged(val value: String) : ManualEvent
    data object ManualModelClicked : ManualEvent
    data object SearchManualDeviceClicked : ManualEvent
    data class ManualDeviceClicked(val id: String) : ManualEvent
    data object ConfirmManualDeviceClicked : ManualEvent
    data object BackClicked : AddDeviceEvent
    data object ShowNearbyDevicesClicked : AddDeviceEvent
    data object ShowQrCodeScannerClicked : AddDeviceEvent
    data class QrCodeScanned(val data: String) : AddDeviceEvent
    data class NearbyDeviceClicked(val id: String) : AddDeviceEvent
    data object Resumed : AddDeviceEvent
    data object OpenSystemSettingsClicked : AddDeviceEvent
    data object AddDemoDeviceClicked : AddDeviceEvent
    data object CancelWarningClicked : AddDeviceEvent
    data object AcceptWarningClicked : AddDeviceEvent
}
