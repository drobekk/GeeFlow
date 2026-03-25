package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.impl.DeviceControllerProvider
import org.koin.core.annotation.Factory

@Factory
class SetBrewTemperatureUseCase(private val provider: DeviceControllerProvider) {
    suspend operator fun invoke(deviceId: String, temp: Int) =
        provider.getController(deviceId).setBrewTemperature(temp)
}
