package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.devices.api.DeviceRepository
import org.koin.core.annotation.Factory

@Factory
class DeleteDeviceUseCase(
    private val deviceRepository: DeviceRepository
) {
    operator fun invoke(serialNumber: String) {
        deviceRepository.removeDeviceBySerialNumber(serialNumber)
    }
}
