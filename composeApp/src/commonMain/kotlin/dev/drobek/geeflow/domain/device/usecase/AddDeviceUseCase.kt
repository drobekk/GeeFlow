package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.devices.api.DeviceRepository
import dev.drobek.geeflow.domain.device.model.Device
import org.koin.core.annotation.Factory

@Factory
class AddDeviceUseCase(
    private val deviceRepository: DeviceRepository
) {
    operator fun invoke(
        serialNumber: String,
        macAddress: String,
        name: String
    ) {
        deviceRepository.addDevice(
            Device(
                serialNumber = serialNumber,
                name = name,
                macAddress = macAddress
            )
        )
    }
}
