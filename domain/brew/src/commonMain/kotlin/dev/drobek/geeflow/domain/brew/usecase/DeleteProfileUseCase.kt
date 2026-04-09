package dev.drobek.geeflow.domain.brew.usecase

import dev.drobek.geeflow.data.brew.BrewProfileRepository
import dev.drobek.geeflow.data.device.DeviceRepository
import org.koin.core.annotation.Factory

@Factory
class DeleteProfileUseCase(
    private val brewProfileRepository: BrewProfileRepository,
    private val deviceRepository: DeviceRepository
) {
    @Throws(Exception::class)
    operator fun invoke(profileId: Long) {
        val isBound = deviceRepository.devices.value.any { it.boundProfileId == profileId }
        if (isBound) throw Exception("Cannot delete bound profile")
        brewProfileRepository.removeBrewProfile(profileId)
    }
}
