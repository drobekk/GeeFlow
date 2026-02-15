package dev.drobek.geeflow.presentation.feature.device.adddevice

import dev.drobek.geeflow.domain.device.usecase.AddDeviceUseCase
import dev.drobek.geeflow.domain.device.usecase.ParseDeviceQrCodeUseCase
import dev.drobek.geeflow.platform.Platform
import dev.drobek.geeflow.presentation.feature.device.adddevice.AddDeviceEvent.BackClicked
import dev.drobek.geeflow.presentation.feature.device.adddevice.AddDeviceEvent.FormSubmitted
import dev.drobek.geeflow.presentation.feature.device.adddevice.AddDeviceEvent.QrCodeScanned
import dev.drobek.geeflow.presentation.feature.device.adddevice.AddDeviceEvent.ShowFormClicked
import dev.drobek.geeflow.presentation.feature.device.adddevice.AddDeviceEvent.ShowQrCodeScannerClicked
import dev.drobek.geeflow.presentation.feature.device.adddevice.AddDeviceViewModelEvent.ShowSnackbar
import dev.drobek.geeflow.viewmodel.BaseViewModel
import geeflow.composeapp.generated.resources.Res
import geeflow.composeapp.generated.resources.add_device_screen_qr_parsing_error
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class AddDeviceViewModel(
    platform: Platform,
    private val addDeviceUseCase: AddDeviceUseCase,
    private val parseDeviceQrCodeUseCase: ParseDeviceQrCodeUseCase
) : BaseViewModel<AddDeviceViewState, AddDeviceViewModelEvent>(
    AddDeviceViewState(
        isFormVisible = platform.type == Platform.Type.Desktop,
        isQrCodeScannerButtonVisible = platform.type != Platform.Type.Desktop
    )
) {

    fun handleEvent(event: AddDeviceEvent) = when (event) {
        is QrCodeScanned -> parseQrCode(event.data)
        is FormSubmitted -> addDevice(
            deviceId = event.deviceId,
            macAddress = event.bleMacAddress,
            name = event.name
        )

        is ShowFormClicked -> modify { copy(isFormVisible = true) }
        is ShowQrCodeScannerClicked -> modify { copy(isFormVisible = false) }
        is BackClicked -> emitEvent(Navigation.Back)
    }

    private fun parseQrCode(data: String) {
        val device = parseDeviceQrCodeUseCase(data)
        if (device != null) {
            addDeviceUseCase(
                serialNumber = device.serialNumber,
                macAddress = device.macAddress,
                name = device.name
            )
            modify { copy(qrCodeScanningEnabled = false) }
            emitEvent(Navigation.DevicesList)
        } else {
            launch {
                emitEvent(ShowSnackbar(getString(Res.string.add_device_screen_qr_parsing_error)))
            }
        }
    }

    private fun addDevice(deviceId: String, macAddress: String, name: String) {
        addDeviceUseCase(
            serialNumber = deviceId,
            macAddress = macAddress,
            name = name
        )
        emitEvent(Navigation.DevicesList)
    }
}
