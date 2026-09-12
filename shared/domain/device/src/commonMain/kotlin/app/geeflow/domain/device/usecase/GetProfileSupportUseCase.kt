package app.geeflow.domain.device.usecase

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.assessProfile
import org.koin.core.annotation.Factory

@Factory
class GetProfileSupportUseCase(private val provider: DeviceControllerProvider) {
    operator fun invoke(deviceId: Long, profile: BrewProfile) = provider.getController(deviceId).assessProfile(profile)
}
