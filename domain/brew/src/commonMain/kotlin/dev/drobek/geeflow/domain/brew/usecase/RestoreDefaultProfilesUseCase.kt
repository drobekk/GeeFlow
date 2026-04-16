package dev.drobek.geeflow.domain.brew.usecase

import dev.drobek.geeflow.data.brew.BrewProfileRepository
import dev.drobek.geeflow.domain.user.usecase.GetSelectedUserUseCase
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class RestoreDefaultProfilesUseCase(
    private val getSelectedUser: GetSelectedUserUseCase,
    private val brewProfileRepository: BrewProfileRepository,
) {
    suspend operator fun invoke() {
        val userId = getSelectedUser().first()?.id ?: return
        brewProfileRepository.resetProfilesForUser(userId)
    }
}
