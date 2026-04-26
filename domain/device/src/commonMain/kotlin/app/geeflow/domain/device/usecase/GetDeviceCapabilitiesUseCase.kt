package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceCapability
import org.koin.core.annotation.Factory

@Factory
class GetDeviceCapabilitiesUseCase(private val provider: DeviceControllerProvider) {
    operator fun invoke(deviceId: Long): Set<DeviceCapability> =
        provider.getController(deviceId).capabilities
}
