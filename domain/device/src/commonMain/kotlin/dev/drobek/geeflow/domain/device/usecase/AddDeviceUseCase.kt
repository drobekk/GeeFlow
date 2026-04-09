package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.DeviceRepository
import dev.drobek.geeflow.data.device.model.Device
import dev.drobek.geeflow.data.device.model.DeviceConnection
import org.koin.core.annotation.Factory

@Factory
class AddDeviceUseCase(
    private val deviceRepository: DeviceRepository
) {
    operator fun invoke(device: Device): Long {
        val ble = device.connection as? DeviceConnection.Ble
        if (ble != null) {
            val existing = deviceRepository.getDeviceByBleMacAddress(ble.macAddress)
            if (existing != null) return existing.id
        }
        return deviceRepository.addDevice(device)
    }
}
