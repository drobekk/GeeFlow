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
    suspend operator fun invoke(profileId: Long): Boolean {
        val user = getSelectedUserUseCase().firstOrNull() ?: return false
        val profile = brewProfileRepository.getBrewProfileById(profileId) ?: return false
        
        return try {
            deviceController.bindProfile(profile)
            brewProfileRepository.bindProfile(user.id, profileId)
            true
        } catch (e: Exception) {
            false
        }
    }
}
