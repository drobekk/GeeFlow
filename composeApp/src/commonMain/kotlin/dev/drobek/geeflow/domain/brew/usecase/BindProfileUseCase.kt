package dev.drobek.geeflow.domain.brew.usecase

import dev.drobek.geeflow.data.brew.api.BrewProfileRepository
import dev.drobek.geeflow.data.device.impl.DeviceControllerProvider
import org.koin.core.annotation.Factory

@Factory
class BindProfileUseCase(
    private val brewProfileRepository: BrewProfileRepository,
    private val provider: DeviceControllerProvider
) {
    @Throws(Exception::class)
    suspend operator fun invoke(deviceId: String, profileId: Long) {
        val profile = brewProfileRepository.getBrewProfileById(profileId) ?: throw Exception("Profile not found")
        provider.getController(deviceId).bindProfile(profile)
        brewProfileRepository.bindProfile(deviceId, profileId)
    }
}
