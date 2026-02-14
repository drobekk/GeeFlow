package dev.drobek.geeflow.data.devices.api

import dev.drobek.geeflow.domain.device.model.Device
import kotlinx.coroutines.flow.StateFlow

interface DeviceRepository {
    val devices: StateFlow<List<Device>>

    fun addDevice(device: Device)
    fun removeDeviceBySerialNumber(serialNumber: String)
    fun setLastUsedDevice(serialNumber: String)
    fun clearAll()
}
