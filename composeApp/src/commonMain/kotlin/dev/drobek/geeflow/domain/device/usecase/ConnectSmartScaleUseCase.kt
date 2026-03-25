package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.impl.DeviceControllerProvider
import org.koin.core.annotation.Factory

@Factory
class ConnectSmartScaleUseCase(private val provider: DeviceControllerProvider) {
    suspend operator fun invoke(deviceId: String, name: String) =
        provider.getController(deviceId).connectSmartScale(name)
}
