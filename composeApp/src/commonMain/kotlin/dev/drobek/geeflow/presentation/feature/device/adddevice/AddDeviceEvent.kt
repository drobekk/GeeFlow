package dev.drobek.geeflow.presentation.feature.device.adddevice

internal sealed interface AddDeviceEvent {
    data object BackClicked: AddDeviceEvent
    data object ShowFormClicked : AddDeviceEvent
    data object ShowQrCodeScannerClicked: AddDeviceEvent
    data class QrCodeScanned(val data: String) : AddDeviceEvent
    data class FormSubmitted(val deviceId: String, val bleMacAddress: String, val name: String) : AddDeviceEvent
}
