package dev.drobek.geeflow.data.device.impl

import dev.drobek.geeflow.data.device.api.DeviceRepository
import dev.drobek.geeflow.domain.device.model.Device
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Singleton

@Singleton
class DeviceRepositoryImpl(
    private val devicesDao: DevicesDao
) : DeviceRepository {

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    override val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    init {
        refresh()
    }

    override fun addDevice(device: Device) {
        devicesDao.insertDevice(device)
        refresh()
    }

    override fun getDeviceBySerialNumber(serialNumber: String): Device? {
        return devicesDao.getDeviceBySerialNumber(serialNumber)
    }

    override fun removeDeviceBySerialNumber(serialNumber: String) {
        devicesDao.deleteDevice(serialNumber)
        refresh()
    }

    override fun setLastUsedDevice(serialNumber: String) {
        devicesDao.updateLastUsed(serialNumber)
        refresh()
    }

    override fun clearAll() {
        devicesDao.clearAll()
        refresh()
    }

    private fun refresh() {
        _devices.value = devicesDao.getAllDevices()
    }
}
