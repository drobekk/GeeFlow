package app.geeflow.data.device

import app.geeflow.data.device.model.Device
import dev.bluefalcon.core.BluetoothPeripheral
import kotlinx.coroutines.flow.StateFlow

interface NearbyDevicesController {
    val discoveredDevices: StateFlow<Set<DiscoveredBleDevice>>
    fun startScanning()
    fun stopScanning()
}

/**
 * A recognised nearby device together with the live Blue Falcon peripheral handle the
 * scan found. The peripheral is useful at add-time: it's the same object the controller
 * needs to call [dev.bluefalcon.core.BlueFalcon.connect] with, so handing it off
 * together with the [Device] saves a `retrievePeripheral(...)` round-trip.
 */
data class DiscoveredBleDevice(
    val device: Device,
    val peripheral: BluetoothPeripheral,
)
