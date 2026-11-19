package app.geeflow.data.device.impl

import app.geeflow.data.device.DeviceRepository
import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.data.device.model.SupportedDevice
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
        ensureLiangsLita()
        refresh()
    }

    /** Keep the printed serial separate from the BLE address used by the transport. */
    private fun ensureLiangsLita() {
        val defaultDevice = Device(
            name = LIANGS_LITA_NAME,
            connection = DeviceConnection.Ble(
                peripheralId = LIANGS_LITA_BLE_ADDRESS,
                macAddress = LIANGS_LITA_BLE_ADDRESS,
            ),
            manufacturer = SupportedDevice.LitaBA.manufacturer,
            model = SupportedDevice.LitaBA.model,
            modelId = SupportedDevice.LitaBA.id,
            modelSource = "bundled-default",
            serialNumber = LIANGS_LITA_SERIAL_NUMBER,
        )
        val saved = devicesDao.getDeviceByBleMac(LIANGS_LITA_BLE_ADDRESS)
        when {
            saved == null -> devicesDao.insertDevice(defaultDevice)
            saved.serialNumber == null -> devicesDao.updateDefaultIdentity(saved.id, defaultDevice)
        }
    }

    override fun addDevice(device: Device): Long {
        val id = devicesDao.insertDevice(device)
        refresh()
        return id
    }

    override fun getDeviceById(id: Long): Device? = devicesDao.getDeviceById(id)

    override fun getDeviceByBleMacAddress(macAddress: String): Device? = devicesDao.getDeviceByBleMac(macAddress)

    override fun updateModel(id: Long, model: SupportedDevice) {
        devicesDao.updateModel(id, model)
        refresh()
    }

    override fun renameDevice(id: Long, name: String) {
        devicesDao.updateName(id, name)
        refresh()
    }

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

    private companion object {
        const val LIANGS_LITA_NAME = "Liang's LITA"
        const val LIANGS_LITA_SERIAL_NUMBER = "AB08118958"
        const val LIANGS_LITA_BLE_ADDRESS = "10:20:BA:04:43:36"
    }
}
