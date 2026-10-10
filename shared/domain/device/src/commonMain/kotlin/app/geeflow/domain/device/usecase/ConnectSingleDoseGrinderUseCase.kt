package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import org.koin.core.annotation.Factory

@Factory
class ConnectSingleDoseGrinderUseCase(private val provider: DeviceControllerProvider) {
    suspend operator fun invoke(deviceId: Long, name: String) = with(provider.getController(deviceId)) {
        requireConnected(deviceId)
        connectSingleDoseGrinder(name)
    }
}
