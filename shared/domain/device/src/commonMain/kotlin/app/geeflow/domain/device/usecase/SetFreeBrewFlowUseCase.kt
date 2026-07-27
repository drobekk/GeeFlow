package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import org.koin.core.annotation.Factory

@Factory
class SetFreeBrewFlowUseCase(private val provider: DeviceControllerProvider) {
    suspend operator fun invoke(deviceId: Long, flow: Float) = with(provider.getController(deviceId)) {
        requireConnected(deviceId)
        setFreeBrewFlowTarget(flow)
    }
}
