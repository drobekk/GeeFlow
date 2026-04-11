package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.DeviceControllerProvider
import dev.drobek.geeflow.data.device.model.DeviceState.HeatingMode
import org.koin.core.annotation.Factory

@Factory
class SetPulseHeatingModeUseCase(private val provider: DeviceControllerProvider) {
    suspend operator fun invoke(deviceId: Long, enabled: Boolean) = with(provider.getController(deviceId)) {
        requireConnected(deviceId)
        setHeatingMode(if (enabled) HeatingMode.Pulse else HeatingMode.FullSpeed)
    }
}
