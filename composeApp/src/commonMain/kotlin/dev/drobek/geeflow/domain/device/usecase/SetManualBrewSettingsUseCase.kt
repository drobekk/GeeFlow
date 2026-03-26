package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.impl.DeviceControllerProvider
import org.koin.core.annotation.Factory

@Factory
class SetManualBrewSettingsUseCase(private val provider: DeviceControllerProvider) {
    suspend operator fun invoke(deviceId: String, pressure: Float, timeSec: Float) {
        val controller = provider.getController(deviceId)
        controller.setManualBrewPressure(pressure)
        controller.setManualBrewTime(timeSec)
    }
}
