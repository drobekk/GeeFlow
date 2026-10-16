package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Factory

@Factory
class ObserveDeviceCapabilitiesUseCase(private val provider: DeviceControllerProvider) {
    operator fun invoke(deviceId: Long) = provider.observeController(deviceId).map { it.capabilities }
}
