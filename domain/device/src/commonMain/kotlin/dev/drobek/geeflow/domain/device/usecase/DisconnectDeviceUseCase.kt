package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.DeviceControllerProvider
import org.koin.core.annotation.Factory

@Factory
class DisconnectDeviceUseCase(private val provider: DeviceControllerProvider) {
    operator fun invoke(deviceId: Long) = with(provider.getController(deviceId)) {
        requireConnected(deviceId)
        disconnect()
    }
}
