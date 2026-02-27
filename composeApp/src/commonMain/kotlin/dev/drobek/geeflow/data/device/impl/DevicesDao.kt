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

    fun getDeviceByMacAddress(macAddress: String) =
        dbQuery
            .selectByMacAddress(macAddress, ::mapToDevice)
            .executeAsOneOrNull()

    fun insertDevice(device: Device) {
        dbQuery.insertDevice(
            macAddress = device.macAddress,
            name = device.name,
            isLastUsed = device.isLastUsed
        )
    }

    fun updateLastUsed(macAddress: String) {
        dbQuery.resetLastUsed()
        dbQuery.setLastUsed(macAddress)
    }

    fun deleteDevice(macAddress: String) {
        dbQuery.deleteByMacAddress(macAddress)
    }

    fun clearAll() {
        dbQuery.clearAll()
    }

    private fun mapToDevice(
        macAddress: String,
        name: String,
        isLastUsed: Boolean
    ): Device = Device(
        macAddress = macAddress,
        name = name,
        isLastUsed = isLastUsed
    )
}
