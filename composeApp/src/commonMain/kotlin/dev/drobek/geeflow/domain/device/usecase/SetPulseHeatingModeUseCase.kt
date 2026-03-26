package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.impl.DeviceControllerProvider
import dev.drobek.geeflow.domain.device.model.MachineState.HeatingMode
import org.koin.core.annotation.Factory

@Factory
class SetPulseHeatingModeUseCase(private val provider: DeviceControllerProvider) {
    suspend operator fun invoke(deviceId: String, enabled: Boolean) =
        provider.getController(deviceId).setHeatingMode(
            if (enabled) HeatingMode.Pulse else HeatingMode.FullSpeed
        )
}
