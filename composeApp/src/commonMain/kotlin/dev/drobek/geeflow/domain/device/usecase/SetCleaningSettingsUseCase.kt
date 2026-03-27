package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.impl.DeviceControllerProvider
import org.koin.core.annotation.Factory

@Factory
class SetCleaningSettingsUseCase(private val provider: DeviceControllerProvider) {
    suspend operator fun invoke(deviceId: String, timeSec: Float, restSec: Float, count: Int) =
        provider.getController(deviceId).setCleaningSettings(timeSec, restSec, count)
}
