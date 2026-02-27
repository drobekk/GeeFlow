package dev.drobek.geeflow.presentation.feature.device.add

import dev.drobek.geeflow.data.device.api.NearbyDevicesController
import dev.drobek.geeflow.domain.device.model.Device
import dev.drobek.geeflow.domain.device.usecase.AddDeviceUseCase
import dev.drobek.geeflow.domain.device.usecase.ParseDeviceQrCodeUseCase
import dev.drobek.geeflow.platform.permissions.DeniedException
import dev.drobek.geeflow.platform.permissions.PermissionBluetoothConnect
import dev.drobek.geeflow.platform.permissions.PermissionBluetoothScan
import dev.drobek.geeflow.platform.permissions.PermissionsController
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceEvent.BackClicked
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceEvent.NearbyDeviceClicked
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceEvent.OpenSystemSettingsClicked
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceEvent.QrCodeScanned
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceEvent.Resumed
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceEvent.ShowNearbyDevicesClicked
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceEvent.ShowQrCodeScannerClicked
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceViewModelEvent.ShowSnackbar
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceViewState.DeviceItem
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceViewState.Method.NearbyDevices
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceViewState.Method.QrCodeScanner
import dev.drobek.geeflow.presentation.feature.device.add.Navigation.Back
import dev.drobek.geeflow.presentation.feature.device.add.Navigation.DevicesList
import dev.drobek.geeflow.viewmodel.BaseViewModel
import geeflow.composeapp.generated.resources.Res
import geeflow.composeapp.generated.resources.add_device_screen_qr_parsing_error
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class AddDeviceViewModel(
    private val addDeviceUseCase: AddDeviceUseCase,
    private val parseDeviceQrCodeUseCase: ParseDeviceQrCodeUseCase,
    private val nearbyDevicesController: NearbyDevicesController,
    private val permissionsController: PermissionsController
) : BaseViewModel<AddDeviceViewState, AddDeviceViewModelEvent>(AddDeviceViewState()) {

    init {
        startScanning()
        launch { nearbyDevicesController.discoveredDevices.collect(::onDevicesFound) }
    }

    fun handleEvent(event: AddDeviceEvent) = when (event) {
        is BackClicked -> emitEvent(Back)
        is ShowNearbyDevicesClicked -> {
            startScanning()
            modify { copy(method = NearbyDevices()) }
        }

        is ShowQrCodeScannerClicked -> {
            nearbyDevicesController.stopScanning()
            modify { copy(method = QrCodeScanner()) }
        }

        is QrCodeScanned -> parseQrCode(event.data)
        is NearbyDeviceClicked -> {
            val device = nearbyDevicesController.discoveredDevices.value.find { it.macAddress == event.id }
            device?.let {
                addDeviceUseCase(macAddress = it.macAddress, name = it.name)
                emitEvent(DevicesList)
            }
        }

        is Resumed -> startScanning()
        is OpenSystemSettingsClicked -> permissionsController.openAppSettings()
    }

    private fun startScanning() {
        withBluetoothPermissions {
            nearbyDevicesController.startScanning()
            showMissingPermissionMessage(false)
        }
    }

    private fun onDevicesFound(devices: Set<Device>) {
        (viewState.value.method as? NearbyDevices)?.let {
            val items = devices.map { device -> DeviceItem(id = device.macAddress, name = device.name) }
            modify { copy(method = it.copy(devices = items)) }
        }
    }

    private fun withBluetoothPermissions(block: suspend () -> Unit) = launch {
        try {
            permissionsController.providePermission(PermissionBluetoothScan)
            permissionsController.providePermission(PermissionBluetoothConnect)
            block()
        } catch (_: DeniedException) {
            showMissingPermissionMessage(true)
        }
    }

    private fun showMissingPermissionMessage(show: Boolean) {
        (viewState.value.method as? NearbyDevices)?.let {
            modify { copy(method = it.copy(showMissingPermissionMessage = show)) }
        }
    }

    private fun parseQrCode(data: String) {
        val device = parseDeviceQrCodeUseCase(data)
        if (device != null) {
            addDeviceUseCase(macAddress = device.macAddress, name = device.name)
            emitEvent(DevicesList)
        } else {
            launch {
                emitEvent(ShowSnackbar(getString(Res.string.add_device_screen_qr_parsing_error)))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        nearbyDevicesController.stopScanning()
    }
}
