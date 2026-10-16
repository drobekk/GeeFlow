package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceCapability
import app.geeflow.data.device.model.DeviceState.BrewStatus
import app.geeflow.domain.device.ProfileExecutionCoordinator
import app.geeflow.domain.exception.MachineBusyException
import org.koin.core.annotation.Factory

@Factory
class StartHotWaterUseCase(
    private val coordinator: ProfileExecutionCoordinator,
    private val provider: DeviceControllerProvider,
) {
    suspend operator fun invoke(deviceId: Long) = coordinator.withUnownedControl(deviceId) {
        with(provider.getController(deviceId)) {
            requireReady(deviceId)
            check(DeviceCapability.HotWaterDispensing in capabilities)
            if (deviceState.value.brewStatus != BrewStatus.Idle) throw MachineBusyException(deviceId)
            hotWater.start()
        }
    }
}
