package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import org.koin.core.annotation.Factory

@Factory
class StartFreeVariableBrewingUseCase(private val provider: DeviceControllerProvider) {
    suspend operator fun invoke(deviceId: Long, isFlow: Boolean) = with(provider.getController(deviceId)) {
        requireConnected(deviceId)
        startFreeVariableBrewing(isFlow)
    }
}
