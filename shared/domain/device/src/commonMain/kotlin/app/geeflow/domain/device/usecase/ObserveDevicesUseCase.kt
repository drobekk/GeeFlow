package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceRepository
import app.geeflow.data.device.model.Device
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Factory

@Factory
class ObserveDevicesUseCase(
    private val deviceRepository: DeviceRepository,
) {
    operator fun invoke(): StateFlow<List<Device>> = deviceRepository.devices
}
