package dev.drobek.geeflow.data.device.api

import dev.drobek.geeflow.domain.device.model.Device
import kotlinx.coroutines.flow.StateFlow

interface DeviceRepository {
    val devices: StateFlow<List<Device>>

    fun addDevice(device: Device)
    fun getDeviceByMacAddress(macAddress: String): Device?
    fun removeDeviceByMacAddress(macAddress: String)
    fun setLastUsedDevice(macAddress: String)
    fun clearAll()
}
