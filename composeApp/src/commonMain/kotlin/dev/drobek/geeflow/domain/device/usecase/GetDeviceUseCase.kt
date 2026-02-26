package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.api.DeviceRepository
import dev.drobek.geeflow.domain.device.model.Device
import org.koin.core.annotation.Factory

@Factory
class GetDeviceUseCase(
    private val deviceRepository: DeviceRepository
) {
    operator fun invoke(serialNumber: String): Device? = deviceRepository.getDeviceBySerialNumber(serialNumber)
}
