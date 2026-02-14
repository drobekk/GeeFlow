package dev.drobek.geeflow.data.devices.impl

import dev.drobek.geeflow.data.db.DatabaseProvider
import dev.drobek.geeflow.domain.device.model.Device
import org.koin.core.annotation.Singleton

@Singleton
class DevicesDao(databaseProvider: DatabaseProvider) {
    private val dbQuery = databaseProvider.database.deviceQueries

    fun getAllDevices() = dbQuery.selectAll(::mapToDevice).executeAsList()

    fun getDeviceBySerialNumber(serialNumber: String) =
        dbQuery.selectBySerialNumber(serialNumber, ::mapToDevice).executeAsOneOrNull()

    fun insertDevice(device: Device) {
        dbQuery.insertDevice(
            serialNumber = device.serialNumber,
            name = device.name,
            isLastUsed = device.isLastUsed
        )
    }

    fun updateLastUsed(serialNumber: String) {
        dbQuery.updateLastUsed(serialNumber)
    }

    fun deleteDevice(serialNumber: String) {
        dbQuery.deleteBySerialNumber(serialNumber)
    }

    fun clearAll() {
        dbQuery.clearAll()
    }

    private fun mapToDevice(
        serialNumber: String,
        name: String,
        isLastUsed: Boolean
    ): Device = Device(serialNumber, name, isLastUsed)
}
