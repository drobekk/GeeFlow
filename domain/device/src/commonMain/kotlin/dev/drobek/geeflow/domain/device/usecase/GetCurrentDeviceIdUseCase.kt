package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.DeviceControllerProvider
import org.koin.core.annotation.Factory

@Factory
class GetCurrentDeviceIdUseCase(private val provider: DeviceControllerProvider) {
    operator fun invoke(): Long? = provider.currentDeviceId
}
