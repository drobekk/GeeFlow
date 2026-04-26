package app.geeflow.domain.device.usecase

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.domain.exception.ScaleNotConnectedException
import org.koin.core.annotation.Factory

@Factory
class StartProfileBrewingUseCase(private val provider: DeviceControllerProvider) {
    suspend operator fun invoke(deviceId: Long, profile: BrewProfile) = with(provider.getController(deviceId)) {
        requireConnected(deviceId)
        if (profile.finishCondition is Condition.Weight && deviceState.value.smartScale?.isConnected != true) {
            throw ScaleNotConnectedException(deviceId)
        }
        startProfileBrewing(profile)
    }
}
