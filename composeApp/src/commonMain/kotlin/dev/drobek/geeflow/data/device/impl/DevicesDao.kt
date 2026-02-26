package dev.drobek.geeflow.data.device.impl

import dev.drobek.geeflow.data.db.DatabaseProvider
import dev.drobek.geeflow.domain.device.model.Device
import org.koin.core.annotation.Singleton

@Singleton
class DevicesDao(databaseProvider: DatabaseProvider) {
    private val dbQuery = databaseProvider.database.deviceQueries

    fun getAllDevices() = dbQuery
        .selectAll(::mapToDevice)
        .executeAsList()

    fun getDeviceBySerialNumber(serialNumber: String) =
        dbQuery
            .selectBySerialNumber(serialNumber, ::mapToDevice)
            .executeAsOneOrNull()

    fun insertDevice(device: Device) {
        dbQuery.insertDevice(
            serialNumber = device.serialNumber,
            name = device.name,
            macAddress = device.macAddress,
            isLastUsed = device.isLastUsed
        )
    }

    fun updateLastUsed(serialNumber: String) {
        dbQuery.resetLastUsed()
        dbQuery.setLastUsed(serialNumber)
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
        macAddress: String,
        isLastUsed: Boolean
    ): Device = Device(
        serialNumber = serialNumber,
        name = name,
        macAddress = macAddress,
        isLastUsed = isLastUsed
    )
}
