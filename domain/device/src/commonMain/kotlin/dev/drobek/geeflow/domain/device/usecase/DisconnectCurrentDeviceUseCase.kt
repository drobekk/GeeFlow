package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.DeviceControllerProvider
import org.koin.core.annotation.Factory

@Factory
class DisconnectCurrentDeviceUseCase(private val provider: DeviceControllerProvider) {
    operator fun invoke() = provider.disconnectCurrent()
}
