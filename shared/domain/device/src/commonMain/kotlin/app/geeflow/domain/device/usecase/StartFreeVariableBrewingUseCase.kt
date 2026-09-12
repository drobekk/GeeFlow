package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.domain.device.ProfileExecutionCoordinator
import org.koin.core.annotation.Factory

@Factory
class StartFreeVariableBrewingUseCase(
    private val coordinator: ProfileExecutionCoordinator,
    private val provider: DeviceControllerProvider
) {
    suspend operator fun invoke(deviceId: Long, isFlow: Boolean) = coordinator.withUnownedControl(
        deviceId
    ) {
        with(provider.getController(deviceId)) {
            requireConnected(deviceId)
            startFreeVariableBrewing(isFlow)
        }
    }
}
