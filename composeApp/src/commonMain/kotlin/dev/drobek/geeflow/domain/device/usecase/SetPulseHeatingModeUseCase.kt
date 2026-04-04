package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.impl.DeviceControllerProvider
import dev.drobek.geeflow.domain.device.model.DeviceState.HeatingMode
import org.koin.core.annotation.Factory

@Factory
class SetPulseHeatingModeUseCase(private val provider: DeviceControllerProvider) {
    suspend operator fun invoke(deviceId: String, enabled: Boolean) = with( provider.getController(deviceId)) {
        requireConnected(deviceId)
        setHeatingMode(if (enabled) HeatingMode.Pulse else HeatingMode.FullSpeed)
    }
}
