package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.DeviceControllerProvider
import dev.drobek.geeflow.data.device.model.DeviceState
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Factory

@Factory
class ObserveDeviceStateUseCase(private val provider: DeviceControllerProvider) {
    operator fun invoke(deviceId: Long): StateFlow<DeviceState> =
        provider.getController(deviceId).deviceState
}
