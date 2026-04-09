package dev.drobek.geeflow.domain.brew.usecase

import dev.drobek.geeflow.data.brew.BrewProfileRepository
import dev.drobek.geeflow.data.device.DeviceRepository
import dev.drobek.geeflow.data.device.DeviceControllerProvider
import dev.drobek.geeflow.domain.device.usecase.requireConnected
import org.koin.core.annotation.Factory

@Factory
class BindProfileUseCase(
    private val brewProfileRepository: BrewProfileRepository,
    private val deviceRepository: DeviceRepository,
    private val provider: DeviceControllerProvider
) {
    @Throws(Exception::class)
    suspend operator fun invoke(deviceId: Long, profileId: Long) = with(provider.getController(deviceId)) {
        requireConnected(deviceId)
        val profile = brewProfileRepository.getBrewProfileById(profileId) ?: throw Exception("Profile not found")
        bindProfile(profile)
        deviceRepository.bindProfile(deviceId, profileId)
    }
}
