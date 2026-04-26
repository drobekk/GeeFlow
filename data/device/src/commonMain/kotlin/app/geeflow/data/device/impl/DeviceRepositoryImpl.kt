package app.geeflow.data.device.impl

import app.geeflow.data.device.DeviceRepository
import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Singleton

@Singleton
class DeviceRepositoryImpl(
    private val devicesDao: DevicesDao,
) : DeviceRepository {

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    override val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    init {
        refresh()
    }

    override fun addDevice(device: Device): Long {
        val id = devicesDao.insertDevice(device)
        refresh()
        return id
    }

    override fun getDeviceById(id: Long): Device? = devicesDao.getDeviceById(id)

    override fun getDeviceByBleMacAddress(macAddress: String): Device? = devicesDao.getDeviceByBleMac(macAddress)

    override fun removeDeviceById(id: Long) {
        devicesDao.deleteDevice(id)
        refresh()
    }

    override fun setLastUsedDevice(id: Long) {
        devicesDao.updateLastUsed(id)
        refresh()
    }

    override fun updateConnection(id: Long, connection: DeviceConnection) {
        devicesDao.updateConnection(id, connection)
        refresh()
    }

    override fun bindProfile(deviceId: Long, profileId: Long) {
        devicesDao.bindProfile(deviceId, profileId)
        refresh()
    }

    override fun unbindProfile(deviceId: Long) {
        devicesDao.unbindProfile(deviceId)
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
