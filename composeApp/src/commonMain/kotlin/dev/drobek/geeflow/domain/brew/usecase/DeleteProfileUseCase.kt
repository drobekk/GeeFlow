package dev.drobek.geeflow.domain.brew.usecase

import dev.drobek.geeflow.data.brew.api.BrewProfileRepository
import org.koin.core.annotation.Factory

@Factory
class DeleteProfileUseCase(
    private val brewProfileRepository: BrewProfileRepository
) {
    @Throws(Exception::class)
    operator fun invoke(profileId: Long) {
        val profile = brewProfileRepository.getBrewProfileById(profileId)
        if (profile?.boundDeviceMac != null) {
            throw Exception("Cannot delete bound profile")
        }
        brewProfileRepository.removeBrewProfile(profileId)
    }
}
