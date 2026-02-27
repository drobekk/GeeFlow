package dev.drobek.geeflow.data.device.impl

import co.touchlab.kermit.Logger
import dev.bluefalcon.AdvertisementDataRetrievalKeys
import dev.bluefalcon.BlueFalcon
import dev.bluefalcon.BlueFalconDelegate
import dev.bluefalcon.BluetoothCharacteristic
import dev.bluefalcon.BluetoothPeripheral
import dev.drobek.geeflow.data.device.api.NearbyDevicesController
import dev.drobek.geeflow.domain.device.model.Device
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Singleton

@Singleton
class BluetoothNearbyDeviceController(
    private val blueFalcon: BlueFalcon
) : NearbyDevicesController, BlueFalconDelegate {

    private val _discoveredDevices = MutableStateFlow<Set<Device>>(emptySet())
    override val discoveredDevices: StateFlow<Set<Device>> = _discoveredDevices.asStateFlow()

    companion object {
        private val hexPattern = Regex("([a-fA-F0-9]{12})$")
    }

    init {
        blueFalcon.delegates.add(this)
    }

    override fun startScanning() {
        Logger.d { "Start scanning for nearby Bluetooth devices" }

        _discoveredDevices.value = emptySet()
        blueFalcon.scan()
    }

    override fun stopScanning() {
        Logger.d { "Stop scanning" }
        blueFalcon.stopScanning()
    }

    override fun didDiscoverDevice(
        bluetoothPeripheral: BluetoothPeripheral,
        advertisementData: Map<AdvertisementDataRetrievalKeys, Any>
    ) {
        val deviceName = advertisementData[AdvertisementDataRetrievalKeys.LocalName] as? String ?: bluetoothPeripheral.name ?: ""
        val match = hexPattern.find(deviceName)
        if (match != null) {
            val macInName = match.value
            val prefix = deviceName.substring(0, match.range.first)
            val formattedName = prefix
                .trimEnd('_', '-', ' ')
                .replace('_', ' ')
                .trim()
                .ifEmpty { "Unknown Device" }

            val formattedMac = macInName
                .chunked(2)
                .joinToString(":")

            val newDevice = Device(
                macAddress = formattedMac,
                name = formattedName
            )

            _discoveredDevices.update { it + newDevice }
            Logger.i { "Discovered device: $deviceName -> $formattedName ($formattedMac)" }
        }
    }

    override fun didConnect(bluetoothPeripheral: BluetoothPeripheral) {}
    override fun didDisconnect(bluetoothPeripheral: BluetoothPeripheral) {}
    override fun didDiscoverServices(bluetoothPeripheral: BluetoothPeripheral) {}
    override fun didDiscoverCharacteristics(bluetoothPeripheral: BluetoothPeripheral) {}
    override fun didUpdateNotificationStateFor(p: BluetoothPeripheral, c: BluetoothCharacteristic) {}
    override fun didCharacteristcValueChanged(p: BluetoothPeripheral, c: BluetoothCharacteristic) {}
}
