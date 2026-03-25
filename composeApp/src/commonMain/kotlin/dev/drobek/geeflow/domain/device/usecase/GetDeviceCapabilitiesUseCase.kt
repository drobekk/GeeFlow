package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.impl.DeviceControllerProvider
import dev.drobek.geeflow.domain.device.model.DeviceCapability
import org.koin.core.annotation.Factory

@Factory
class GetDeviceCapabilitiesUseCase(private val provider: DeviceControllerProvider) {
    operator fun invoke(deviceId: String): Set<DeviceCapability> =
        provider.getController(deviceId).capabilities
}
