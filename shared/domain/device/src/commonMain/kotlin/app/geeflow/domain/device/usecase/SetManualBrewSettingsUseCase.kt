package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import org.koin.core.annotation.Factory

@Factory
class SetManualBrewSettingsUseCase(private val provider: DeviceControllerProvider) {
    suspend operator fun invoke(
        deviceId: Long,
        pressure: Float,
        timeSec: Float,
    ) = with(provider.getController(deviceId)) {
        requireConnected(deviceId)
        settings.setManualBrewPressure(pressure)
        settings.setManualBrewTime(timeSec)
    }
}
