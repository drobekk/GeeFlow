package dev.drobek.geeflow.domain.brew.usecase

import dev.drobek.geeflow.data.brew.api.BrewProfileRepository
import dev.drobek.geeflow.data.device.api.DeviceController
import org.koin.core.annotation.Factory

@Factory
class BindProfileUseCase(
    private val brewProfileRepository: BrewProfileRepository,
    private val deviceController: DeviceController
) {
    @Throws(Exception::class)
    suspend operator fun invoke(deviceMac: String, profileId: Long) {
        val profile = brewProfileRepository.getBrewProfileById(profileId) ?: throw Exception("Profile not found")

        deviceController.bindProfile(profile)
        brewProfileRepository.bindProfile(deviceMac, profileId)
    }
}
