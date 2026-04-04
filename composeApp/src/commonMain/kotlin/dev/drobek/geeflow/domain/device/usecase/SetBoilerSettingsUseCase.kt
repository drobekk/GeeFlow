package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.impl.DeviceControllerProvider
import dev.drobek.geeflow.domain.device.model.DeviceState.BoilerType
import org.koin.core.annotation.Factory

@Factory
class SetBoilerSettingsUseCase(private val provider: DeviceControllerProvider) {
    suspend operator fun invoke(
        deviceId: String,
        boilerType: BoilerType,
        enabled: Boolean,
        temp: Int
    ) = with(provider.getController(deviceId)) {
        requireConnected(deviceId)
        setBoilerState(boilerType, enabled)
        when (boilerType) {
            BoilerType.Brew -> setBrewTemperature(temp)
            BoilerType.Steam -> setSteamTemperature(temp)
        }
    }
}
