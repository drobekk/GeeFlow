package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.domain.device.ProfileExecutionCoordinator
import org.koin.core.annotation.Factory

@Factory
class StopFreeVariableBrewingUseCase(
    private val coordinator: ProfileExecutionCoordinator,
    private val provider: DeviceControllerProvider
) {
    suspend operator fun invoke(deviceId: Long) = with(provider.getController(deviceId)) {
        if (coordinator.stop(deviceId)) return@with
        requireConnected(deviceId)
        stopFreeVariableBrewing()
    }
}
