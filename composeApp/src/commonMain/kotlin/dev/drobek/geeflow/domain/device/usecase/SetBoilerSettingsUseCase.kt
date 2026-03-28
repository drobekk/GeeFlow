package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.impl.DeviceControllerProvider
import dev.drobek.geeflow.domain.device.model.DeviceState.BoilerType
import org.koin.core.annotation.Factory

@Factory
class SetBoilerSettingsUseCase(private val provider: DeviceControllerProvider) {
    suspend operator fun invoke(deviceId: String, boilerType: BoilerType, enabled: Boolean, temp: Int) {
        val controller = provider.getController(deviceId)
        controller.setBoilerState(boilerType, enabled)
        when (boilerType) {
            BoilerType.Brew -> controller.setBrewTemperature(temp)
            BoilerType.Steam -> controller.setSteamTemperature(temp)
        }
    }
}
