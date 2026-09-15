package app.geeflow.presentation.feature.device.add

internal sealed interface AddDeviceEvent {
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
