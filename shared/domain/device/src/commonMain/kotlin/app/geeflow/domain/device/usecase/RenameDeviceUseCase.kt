package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceRepository
import org.koin.core.annotation.Factory

@Factory
class RenameDeviceUseCase(private val deviceRepository: DeviceRepository) {
    operator fun invoke(deviceId: Long, name: String) {
        deviceRepository.renameDevice(deviceId, name)
    }
}
