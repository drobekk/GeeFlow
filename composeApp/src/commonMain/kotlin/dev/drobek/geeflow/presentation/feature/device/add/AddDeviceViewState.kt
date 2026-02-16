package dev.drobek.geeflow.presentation.feature.device.add

internal data class AddDeviceViewState(
    val isFormVisible: Boolean = false,
    val qrCodeScanningEnabled: Boolean = true,
    val isQrCodeScannerButtonVisible: Boolean = true
)
