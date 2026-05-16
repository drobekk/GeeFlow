package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceRepository
import app.geeflow.data.device.model.DeviceConnection
import org.koin.core.annotation.Factory

@Factory
class UpdateDeviceConnectionUseCase(
    private val deviceRepository: DeviceRepository,
) {
    operator fun invoke(deviceId: Long, connection: DeviceConnection) {
        deviceRepository.updateConnection(deviceId, connection)
    }
}
