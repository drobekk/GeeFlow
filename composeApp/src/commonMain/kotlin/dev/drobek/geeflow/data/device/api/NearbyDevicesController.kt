package dev.drobek.geeflow.data.device.api

import dev.drobek.geeflow.domain.device.model.Device
import kotlinx.coroutines.flow.StateFlow

interface NearbyDevicesController {
    val discoveredDevices: StateFlow<Set<Device>>
    fun startScanning()
    fun stopScanning()
}
