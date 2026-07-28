@file:Suppress("TooManyFunctions", "LongParameterList")

package app.geeflow.data.device.impl

import app.geeflow.data.device.db.AppDatabase
import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import org.koin.core.annotation.Singleton

@Singleton
class DevicesDao(database: AppDatabase) {
    private val dbQuery = database.deviceQueries

    fun getAllDevices(): List<Device> = dbQuery
        .selectAll(::mapToDevice)
        .executeAsList()

    fun getDeviceById(id: Long): Device? = dbQuery
        .selectById(id, ::mapToDevice)
        .executeAsOneOrNull()

    fun getDeviceByBleMac(macAddress: String): Device? = dbQuery
        .selectByBleMac(macAddress, ::mapToDevice)
        .executeAsOneOrNull()

    fun insertDevice(device: Device): Long {
        val isLastUsed = if (device.isLastUsed) 1L else 0L
        val (connectionType, blePeripheralId, bleMacAddress) = device.connection.toColumns()
        var id = 0L
        dbQuery.transaction {
            dbQuery.insertDevice(
                name = device.name,
                connectionType = connectionType,
                blePeripheralId = blePeripheralId,
                bleMacAddress = bleMacAddress,
                isLastUsed = isLastUsed,
                manufacturer = device.manufacturer,
                model = device.model,
                version = device.version,
            )
            id = dbQuery.lastInsertRowId().executeAsOne()
        }
        return id
    }

    fun updateConnection(id: Long, connection: DeviceConnection) {
        when (connection) {
            is DeviceConnection.Ble -> dbQuery.updateBlePeripheralId(connection.peripheralId, id)
        }
    }

    fun updateLastUsed(id: Long) {
        dbQuery.transaction {
            dbQuery.resetLastUsed()
            dbQuery.setLastUsed(id)
        }
    }

    fun bindProfile(deviceId: Long, profileId: Long) {
        dbQuery.bindProfile(profileId, deviceId)
    }

    fun unbindProfile(deviceId: Long) {
        dbQuery.unbindProfile(deviceId)
    }

    fun deleteDevice(id: Long) {
        dbQuery.deleteById(id)
    }

    fun clearAll() {
        dbQuery.clearAll()
    }

    private fun DeviceConnection.toColumns(): Triple<String, String?, String?> = when (this) {
        is DeviceConnection.Ble -> Triple(CONNECTION_TYPE_BLE, peripheralId, macAddress)
    }

    private fun mapToDevice(
        id: Long,
        name: String,
        connectionType: String,
        blePeripheralId: String?,
        bleMacAddress: String?,
        isLastUsed: Long,
        manufacturer: String,
        model: String,
        version: String,
        boundProfileId: Long?,
    ): Device = Device(
        id = id,
        name = name,
        connection = connectionFromColumns(connectionType, blePeripheralId, bleMacAddress),
        isLastUsed = isLastUsed != 0L,
        manufacturer = manufacturer,
        model = model,
        version = version,
        boundProfileId = boundProfileId,
    )

    private fun connectionFromColumns(
        connectionType: String,
        blePeripheralId: String?,
        bleMacAddress: String?,
    ): DeviceConnection = when (connectionType) {
        CONNECTION_TYPE_BLE -> DeviceConnection.Ble(
            peripheralId = blePeripheralId.orEmpty(),
            macAddress = bleMacAddress.orEmpty(),
        )

        else -> error("Unknown connectionType '$connectionType' in devices row")
    }

    private companion object {
        private const val CONNECTION_TYPE_BLE = "ble"
    }
}
