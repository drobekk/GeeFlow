package dev.drobek.geeflow.domain.brew.usecase

import dev.drobek.geeflow.data.brew.api.BrewProfileRepository
import dev.drobek.geeflow.data.device.api.DeviceController
import dev.drobek.geeflow.domain.user.usecase.GetSelectedUserUseCase
import kotlinx.coroutines.flow.firstOrNull
import org.koin.core.annotation.Factory

@Factory
class BindProfileUseCase(
    private val getSelectedUserUseCase: GetSelectedUserUseCase,
    private val brewProfileRepository: BrewProfileRepository,
    private val deviceController: DeviceController
) {
    @Throws(Exception::class)
    suspend operator fun invoke(profileId: Long) {
        val user = getSelectedUserUseCase().firstOrNull() ?: throw Exception("No user selected")
        val profile = brewProfileRepository.getBrewProfileById(profileId) ?: throw Exception("Profile not found")

        deviceController.bindProfile(profile)
        brewProfileRepository.bindProfile(user.id, profileId)
    }
}
