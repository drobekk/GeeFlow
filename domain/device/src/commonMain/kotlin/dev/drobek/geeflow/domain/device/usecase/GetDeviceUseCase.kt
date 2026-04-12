package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.DeviceRepository
import dev.drobek.geeflow.data.device.model.Device
import org.koin.core.annotation.Factory

@Factory
class GetDeviceUseCase(
    private val deviceRepository: DeviceRepository,
) {
    operator fun invoke(id: Long): Device? = deviceRepository.getDeviceById(id)
}
