package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.DeviceControllerProvider
import dev.drobek.geeflow.data.device.DeviceRepository
import org.koin.core.annotation.Factory

@Factory
class DeleteDeviceUseCase(
    private val deviceRepository: DeviceRepository,
    private val controllerProvider: DeviceControllerProvider,
) {
    operator fun invoke(id: Long) {
        if (controllerProvider.currentDeviceId == id) {
            controllerProvider.disconnectCurrent()
        }
        deviceRepository.removeDeviceById(id)
    }
}
