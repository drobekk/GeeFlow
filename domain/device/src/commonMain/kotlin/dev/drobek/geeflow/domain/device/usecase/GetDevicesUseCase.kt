package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.DeviceRepository
import dev.drobek.geeflow.data.device.model.Device
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Factory

@Factory
class GetDevicesUseCase(
    private val deviceRepository: DeviceRepository,
) {
    operator fun invoke(): StateFlow<List<Device>> = deviceRepository.devices
}
