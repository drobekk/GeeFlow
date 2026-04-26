package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import org.koin.core.annotation.Factory

@Factory
class SetSmartScaleConnectivityUseCase(private val provider: DeviceControllerProvider) {
    suspend operator fun invoke(deviceId: Long, enabled: Boolean) = with(provider.getController(deviceId)) {
        requireConnected(deviceId)
        setSmartScaleConnectivity(enabled)
    }
}
