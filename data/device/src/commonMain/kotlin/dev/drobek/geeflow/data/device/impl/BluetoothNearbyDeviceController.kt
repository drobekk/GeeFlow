package dev.drobek.geeflow.data.device.impl

import co.touchlab.kermit.Logger
import dev.drobek.geeflow.data.device.NearbyDevicesController
import dev.drobek.geeflow.data.device.ble.BleClient
import dev.drobek.geeflow.data.device.impl.discovery.BleAdvertisement
import dev.drobek.geeflow.data.device.impl.discovery.BleDeviceDiscoverer
import dev.drobek.geeflow.data.device.model.Device
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Singleton

@Singleton
class BluetoothNearbyDeviceController(
    private val bleClient: BleClient,
    private val discoverers: List<BleDeviceDiscoverer>,
    private val scope: CoroutineScope,
) : NearbyDevicesController {

    private val _discoveredDevices = MutableStateFlow<Set<Device>>(emptySet())
    override val discoveredDevices: StateFlow<Set<Device>> = _discoveredDevices.asStateFlow()

    private var collectorJob: Job? = null

    override fun startScanning() {
        Logger.d { "Start scanning for nearby Bluetooth devices" }
        _discoveredDevices.value = emptySet()
        bleClient.startScan()
        collectorJob?.cancel()
        collectorJob = scope.launch {
            bleClient.discoveredDevices.collect { bleSet ->
                _discoveredDevices.value = bleSet
                    .mapNotNull { ble ->
                        discoverers.firstNotNullOfOrNull {
                            it.tryRecognise(BleAdvertisement(ble.peripheralId, ble.name))
                        }
                    }
                    .toSet()
            }
        }
    }

    override fun stopScanning() {
        Logger.d { "Stop scanning" }
        collectorJob?.cancel()
        bleClient.stopScan()
    }
}
