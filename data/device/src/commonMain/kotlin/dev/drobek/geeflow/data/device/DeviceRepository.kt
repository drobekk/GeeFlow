package dev.drobek.geeflow.data.device

import dev.drobek.geeflow.data.device.model.Device
import dev.drobek.geeflow.data.device.model.DeviceConnection
import kotlinx.coroutines.flow.StateFlow

interface DeviceRepository {
    val devices: StateFlow<List<Device>>

    fun addDevice(device: Device): Long
    fun getDeviceById(id: Long): Device?
    fun getDeviceByBleMacAddress(macAddress: String): Device?
    fun removeDeviceById(id: Long)
    fun setLastUsedDevice(id: Long)
    fun updateConnection(id: Long, connection: DeviceConnection)
    fun bindProfile(deviceId: Long, profileId: Long)
    fun unbindProfile(deviceId: Long)
    fun clearAll()
}
