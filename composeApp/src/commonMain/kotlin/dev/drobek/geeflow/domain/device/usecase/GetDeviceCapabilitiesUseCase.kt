package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.api.DeviceController
import dev.drobek.geeflow.domain.device.model.DeviceCapability
import org.koin.core.annotation.Factory

@Factory
class GetDeviceCapabilitiesUseCase(
    private val deviceController: DeviceController
) {
    operator fun invoke(): Set<DeviceCapability> {
        return deviceController.capabilities
    }
}
