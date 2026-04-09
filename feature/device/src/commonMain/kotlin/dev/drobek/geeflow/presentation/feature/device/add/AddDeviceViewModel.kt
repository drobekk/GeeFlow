package dev.drobek.geeflow.presentation.feature.device.add

import dev.drobek.geeflow.core.presentation.BaseViewModel
import dev.drobek.geeflow.core.presentation.Platform
import dev.drobek.geeflow.core.presentation.launch
import dev.drobek.geeflow.data.device.NearbyDevicesController
import dev.drobek.geeflow.data.device.model.Device
import dev.drobek.geeflow.data.device.model.DeviceConnection
import dev.drobek.geeflow.data.device.model.SupportedDevice
import dev.drobek.geeflow.domain.device.usecase.AddDeviceUseCase
import dev.drobek.geeflow.domain.device.usecase.ParseDeviceQrCodeUseCase
import dev.drobek.geeflow.navigation.NavEvent
import dev.drobek.geeflow.navigation.destination.DeviceDashboard
import dev.drobek.geeflow.platform.permissions.DeniedException
import dev.drobek.geeflow.platform.permissions.PermissionBluetoothConnect
import dev.drobek.geeflow.platform.permissions.PermissionBluetoothScan
import dev.drobek.geeflow.platform.permissions.PermissionsController
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceEvent.AddDemoDeviceClicked
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
import geeflow.feature.device.generated.resources.Res
import geeflow.feature.device.generated.resources.add_device_screen_qr_parsing_error
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class AddDeviceViewModel(
    platform: Platform,
    @InjectedParam val permissionsController: PermissionsController,
    private val addDeviceUseCase: AddDeviceUseCase,
    private val parseDeviceQrCodeUseCase: ParseDeviceQrCodeUseCase,
    private val nearbyDevicesController: NearbyDevicesController
) : BaseViewModel<AddDeviceViewState, AddDeviceViewModelEvent>(AddDeviceViewState(platform.getMethod())) {

    init {
        if (viewState.value.method is NearbyDevices) startScanning()
        launch { nearbyDevicesController.discoveredDevices.collect(::onDevicesFound) }
    }

    fun handleEvent(event: AddDeviceEvent) = when (event) {
        is BackClicked -> navigate(NavEvent.Back)
        is ShowNearbyDevicesClicked -> {
            startScanning()
            modify { copy(method = NearbyDevices()) }
        }

        is ShowQrCodeScannerClicked -> {
            modify { copy(method = QrCodeScanner()) }
        }

        is QrCodeScanned -> parseQrCode(event.data)
        is NearbyDeviceClicked -> onDeviceClicked(event.id)
        is Resumed -> (viewState.value.method as? NearbyDevices)?.let { startScanning() }
        is OpenSystemSettingsClicked -> permissionsController.openAppSettings()
        is AddDemoDeviceClicked -> {
            val demoMac = "DE:MO:00:00:00:00"
            addDeviceAndNavigate(
                Device(
                    id = 0L,
                    name = "Demo",
                    connection = DeviceConnection.Ble(
                        peripheralId = demoMac,
                        macAddress = demoMac,
                    ),
                    manufacturer = SupportedDevice.GeeFlowDemo.manufacturer,
                    model = SupportedDevice.GeeFlowDemo.model,
                    version = SupportedDevice.GeeFlowDemo.version,
                )
            )
        }
    }

    private fun startScanning() {
        withBluetoothPermissions {
            nearbyDevicesController.startScanning()
            showMissingPermissionMessage(false)
        }
    }

    private fun onDeviceClicked(id: String) {
        val device = nearbyDevicesController.discoveredDevices.value
            .find { (it.connection as? DeviceConnection.Ble)?.peripheralId == id }
            ?: return
        addDeviceAndNavigate(device)
    }

    private fun onDevicesFound(devices: Set<Device>) {
        (viewState.value.method as? NearbyDevices)?.let {
            val items = devices.mapNotNull { device ->
                val ble = device.connection as? DeviceConnection.Ble ?: return@mapNotNull null
                DeviceItem(id = ble.peripheralId, name = device.name)
            }
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
            addDeviceAndNavigate(device)
        } else {
            launch {
                emitEvent(ShowSnackbar(getString(Res.string.add_device_screen_qr_parsing_error)))
            }
        }
    }

    private fun addDeviceAndNavigate(device: Device) {
        val deviceId = addDeviceUseCase(device)
        navigate(NavEvent.ClearBackStack)
        navigate(NavEvent.To(DeviceDashboard(deviceId)))
    }

    override fun onCleared() {
        super.onCleared()
        nearbyDevicesController.stopScanning()
    }
}

private fun Platform.getMethod(): AddDeviceViewState.Method = when (this.type) {
    Platform.Type.Desktop -> NearbyDevices(changeMethodButtonVisible = false)
    Platform.Type.Android -> QrCodeScanner(changeMethodButtonVisible = true)
    Platform.Type.IOS -> QrCodeScanner(changeMethodButtonVisible = true)
}
