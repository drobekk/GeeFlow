package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.DeviceControllerProvider
import dev.drobek.geeflow.data.device.model.DeviceConstraints
import org.koin.core.annotation.Factory

@Factory
class GetDeviceConstraintsUseCase(private val provider: DeviceControllerProvider) {
    operator fun invoke(deviceId: Long): DeviceConstraints =
        provider.getController(deviceId).constraints
}
