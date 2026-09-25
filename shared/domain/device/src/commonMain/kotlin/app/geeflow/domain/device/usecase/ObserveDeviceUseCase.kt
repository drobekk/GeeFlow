package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceRepository
import app.geeflow.data.device.model.Device
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Factory

@Factory
class ObserveDeviceUseCase(
    private val deviceRepository: DeviceRepository,
) {
    operator fun invoke(id: Long): Flow<Device?> =
        deviceRepository.devices.map { devices -> devices.firstOrNull { it.id == id } }
}
