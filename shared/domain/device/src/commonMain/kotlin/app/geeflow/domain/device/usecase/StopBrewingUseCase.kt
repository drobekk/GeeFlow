package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceState
import app.geeflow.domain.device.ProfileExecutionCoordinator
import org.koin.core.annotation.Factory

@Factory
class StopBrewingUseCase(
    private val provider: DeviceControllerProvider,
    private val coordinator: ProfileExecutionCoordinator
) {
    suspend operator fun invoke(deviceId: Long) = with(provider.getController(deviceId)) {
        if (coordinator.stop(deviceId)) return@with
        requireConnected(deviceId)
        when (deviceState.value.brewStatus) {
            DeviceState.BrewStatus.Profile -> stopProfileBrewing()
            DeviceState.BrewStatus.FreeVariable -> stopFreeVariableBrewing()
            else -> stopManualBrewing()
        }
    }
}
