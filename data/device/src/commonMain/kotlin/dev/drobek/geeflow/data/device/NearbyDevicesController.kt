package dev.drobek.geeflow.data.device

import dev.drobek.geeflow.data.device.model.Device
import kotlinx.coroutines.flow.StateFlow

interface NearbyDevicesController {
    val discoveredDevices: StateFlow<Set<Device>>
    fun startScanning()
    fun stopScanning()
}
