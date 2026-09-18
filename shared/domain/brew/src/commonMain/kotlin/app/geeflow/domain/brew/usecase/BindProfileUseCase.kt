package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewProfileRepository
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.DeviceRepository
import app.geeflow.data.device.assessProfile
import app.geeflow.domain.device.usecase.requireConnected
import app.geeflow.domain.exception.ProfileBindingNotAllowedException
import org.koin.core.annotation.Factory

@Factory
class BindProfileUseCase(
    private val brewProfileRepository: BrewProfileRepository,
    private val deviceRepository: DeviceRepository,
    private val provider: DeviceControllerProvider,
) {
    @Throws(IllegalStateException::class)
    suspend operator fun invoke(deviceId: Long, profileId: Long) {
        invoke(deviceId, checkNotNull(brewProfileRepository.getBrewProfileById(profileId)))
    }

    /**
     * Binds [profile] as given rather than as stored, so an edit can be pushed to the machine before
     * it is persisted and a failure here can abort the save.
     */
    @Throws(IllegalStateException::class, ProfileBindingNotAllowedException::class)
    suspend operator fun invoke(deviceId: Long, profile: BrewProfile) = with(provider.getController(deviceId)) {
        if (!assessProfile(profile).bindingAllowed) {
            throw ProfileBindingNotAllowedException()
        }
        requireConnected(deviceId)
        bindProfile(profile)
        deviceRepository.bindProfile(deviceId, profile.id)
    }
}
