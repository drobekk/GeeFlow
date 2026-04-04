package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.impl.DeviceControllerProvider
import dev.drobek.geeflow.domain.device.model.DeviceState
import org.koin.core.annotation.Factory

@Factory
class StopBrewingUseCase(private val provider: DeviceControllerProvider) {
    suspend operator fun invoke(deviceId: String) = with(provider.getController(deviceId)) {
        requireConnected(deviceId)
        if (deviceState.value.brewStatus == DeviceState.BrewStatus.Profile) {
            stopProfileBrewing()
        } else {
            stopManualBrewing()
        }
    }
}
