package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.impl.DeviceControllerProvider
import dev.drobek.geeflow.domain.brew.model.BrewProfile
import org.koin.core.annotation.Factory

@Factory
class StartProfileBrewingUseCase(private val provider: DeviceControllerProvider) {
    suspend operator fun invoke(deviceId: String, profile: BrewProfile) =
        provider.getController(deviceId).startProfileBrewing(profile)
}
