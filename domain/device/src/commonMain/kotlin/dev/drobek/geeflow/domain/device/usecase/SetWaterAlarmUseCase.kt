package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.DeviceControllerProvider
import org.koin.core.annotation.Factory

@Factory
class SetWaterAlarmUseCase(private val provider: DeviceControllerProvider) {
    suspend operator fun invoke(deviceId: Long, enabled: Boolean) = with(provider.getController(deviceId)) {
        requireConnected(deviceId)
        setWaterAlarm(enabled)
    }
}
