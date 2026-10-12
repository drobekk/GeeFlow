package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import org.koin.core.annotation.Factory

@Factory
class ConnectDeviceUseCase(private val provider: DeviceControllerProvider) {
    suspend operator fun invoke(deviceId: Long) = provider.connect(deviceId)
}
