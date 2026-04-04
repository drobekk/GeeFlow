package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.impl.DeviceControllerProvider
import dev.drobek.geeflow.domain.brew.model.BrewProfile
import dev.drobek.geeflow.domain.brew.model.Condition
import dev.drobek.geeflow.domain.exception.ScaleNotConnectedException
import org.koin.core.annotation.Factory

@Factory
class StartProfileBrewingUseCase(private val provider: DeviceControllerProvider) {
    suspend operator fun invoke(deviceId: String, profile: BrewProfile) = with(provider.getController(deviceId)) {
        requireConnected(deviceId)
        if (profile.finishCondition is Condition.Weight && deviceState.value.smartScale?.isConnected != true) {
            throw ScaleNotConnectedException(deviceId)
        }
        startProfileBrewing(profile)
    }
}
