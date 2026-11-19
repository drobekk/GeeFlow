package app.geeflow.data.device.impl

import app.geeflow.data.device.DiscoveredBleDevice
import app.geeflow.data.device.NearbyDevicesController
import app.geeflow.data.device.ble.BleScanCoordinator
import app.geeflow.data.device.impl.discovery.BleAdvertisement
import app.geeflow.data.device.impl.discovery.BleDeviceDiscoverer
import app.geeflow.data.device.impl.discovery.WendougeeBleDeviceDiscoverer
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
    private val scanCoordinator: BleScanCoordinator,
) : NearbyDevicesController {

    private val _discoveredDevices = MutableStateFlow<Set<DiscoveredBleDevice>>(emptySet())
    override val discoveredDevices: StateFlow<Set<DiscoveredBleDevice>> =
        _discoveredDevices.asStateFlow()

    private var scanJob: Job? = null
    private var manualSerial: String? = null

    override fun startScanning() = startScan(null)

    override fun startManualScanning(serialNumber: String) = startScan(serialNumber)

    private fun startScan(serialNumber: String?) {
        manualSerial = serialNumber
        Logger.d { "Start scanning for nearby Bluetooth devices" }
        scanJob?.cancel()
        _discoveredDevices.value = emptySet()
        scanJob = scope.launch {
            scanCoordinator.scanning {
                blueFalcon.peripherals.collect { peripherals ->
                    _discoveredDevices.value = peripherals
                        .mapNotNull { peripheral -> recognise(peripheral) }
                        .toSet()
                }
            }
        }
    }

    override fun stopScanning() {
        Logger.d { "Stop scanning" }
        scanJob?.cancel()
        scanJob = null
        manualSerial = null
    }

    private fun recognise(peripheral: BluetoothPeripheral): DiscoveredBleDevice? {
        val advertisement = BleAdvertisement(
            peripheralId = peripheral.uuid,
            name = peripheral.name,
        )
        val device = manualSerial?.let { serial ->
            discoverers.filterIsInstance<WendougeeBleDeviceDiscoverer>().firstNotNullOfOrNull {
                it.tryRecogniseSerial(advertisement, serial)
            }
        } ?: discoverers.firstNotNullOfOrNull { it.tryRecognise(advertisement) }
            ?: return null
        return DiscoveredBleDevice(device = device, peripheral = peripheral)
    }
}
