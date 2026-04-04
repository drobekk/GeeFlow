package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.impl.DeviceControllerProvider
import org.koin.core.annotation.Factory

@Factory
class SetManualBrewSettingsUseCase(private val provider: DeviceControllerProvider) {
    suspend operator fun invoke(deviceId: String, pressure: Float, timeSec: Float)  = with( provider.getController(deviceId)) {
        requireConnected(deviceId)
        setManualBrewPressure(pressure)
        setManualBrewTime(timeSec)
    }
}
