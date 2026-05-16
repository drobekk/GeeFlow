package app.geeflow.data.device.impl

import app.geeflow.data.device.DiscoveredBleDevice
import app.geeflow.data.device.NearbyDevicesController
import app.geeflow.data.device.impl.discovery.BleAdvertisement
import app.geeflow.data.device.impl.discovery.BleDeviceDiscoverer
import co.touchlab.kermit.Logger
import dev.bluefalcon.core.BlueFalcon
import dev.bluefalcon.core.BluetoothPeripheral
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Singleton

@Singleton
class BluetoothNearbyDeviceController(
    private val blueFalcon: BlueFalcon,
    private val discoverers: List<BleDeviceDiscoverer>,
    private val scope: CoroutineScope,
) : NearbyDevicesController {

    private val _discoveredDevices = MutableStateFlow<Set<DiscoveredBleDevice>>(emptySet())
    override val discoveredDevices: StateFlow<Set<DiscoveredBleDevice>> =
        _discoveredDevices.asStateFlow()

    private var scanJob: Job? = null

    override fun startScanning() {
        Logger.d { "Start scanning for nearby Bluetooth devices" }
        scanJob?.cancel()
        _discoveredDevices.value = emptySet()
        blueFalcon.clearPeripherals()
        scanJob = scope.launch {
            launch { blueFalcon.scan() }
            blueFalcon.peripherals.collect { peripherals ->
                _discoveredDevices.value = peripherals
                    .mapNotNull { peripheral -> recognise(peripheral) }
                    .toSet()
            }
        }
    }

    override fun stopScanning() {
        Logger.d { "Stop scanning" }
        scanJob?.cancel()
        scanJob = null
        scope.launch { blueFalcon.stopScanning() }
    }

    private fun recognise(peripheral: BluetoothPeripheral): DiscoveredBleDevice? {
        val advertisement = BleAdvertisement(
            peripheralId = peripheral.uuid,
            name = peripheral.name,
        )
        val device = discoverers.firstNotNullOfOrNull { it.tryRecognise(advertisement) }
            ?: return null
        return DiscoveredBleDevice(device = device, peripheral = peripheral)
    }
}
