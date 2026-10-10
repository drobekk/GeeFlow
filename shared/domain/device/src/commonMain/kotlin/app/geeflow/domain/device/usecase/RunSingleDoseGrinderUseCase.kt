package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.domain.exception.GrinderNotConnectedException
import org.koin.core.annotation.Factory

@Factory
class RunSingleDoseGrinderUseCase(private val provider: DeviceControllerProvider) {
    suspend operator fun invoke(deviceId: Long, grindingSize: Int, grindingSpeed: Int) =
        with(provider.getController(deviceId)) {
            requireConnected(deviceId)
            if (deviceState.value.singleDoseGrinder?.isConnected != true) {
                throw GrinderNotConnectedException(deviceId)
            }
            runSingleDoseGrinder(grindingSize, grindingSpeed)
        }
}
