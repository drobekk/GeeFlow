package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import org.koin.core.annotation.Factory

@Factory
class StopFreeVariableBrewingUseCase(private val provider: DeviceControllerProvider) {
    suspend operator fun invoke(deviceId: Long) = with(provider.getController(deviceId)) {
        requireConnected(deviceId)
        stopFreeVariableBrewing()
    }
}
