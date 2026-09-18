package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import org.koin.core.annotation.Factory

@Factory
class GetProfilingCapabilitiesUseCase(private val provider: DeviceControllerProvider) {
    operator fun invoke(deviceId: Long) = provider.getController(deviceId).profilingCapabilities
}
