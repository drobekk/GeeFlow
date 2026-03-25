package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.impl.DeviceControllerProvider
import dev.drobek.geeflow.domain.device.model.MachineState.BoilerType
import org.koin.core.annotation.Factory

@Factory
class SetBoilerStateUseCase(private val provider: DeviceControllerProvider) {
    suspend operator fun invoke(deviceId: String, boilerType: BoilerType, enabled: Boolean) =
        provider.getController(deviceId).setBoilerState(boilerType, enabled)
}
