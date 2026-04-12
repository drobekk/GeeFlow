package dev.drobek.geeflow.domain.brew.usecase

import dev.drobek.geeflow.data.brew.BrewProfileRepository
import dev.drobek.geeflow.data.brew.model.BrewProfile
import dev.drobek.geeflow.domain.user.usecase.GetSelectedUserUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.koin.core.annotation.Factory

@Factory
class ObserveUserProfilesUseCase(
    private val getSelectedUserUseCase: GetSelectedUserUseCase,
    private val brewProfileRepository: BrewProfileRepository,
) {
    operator fun invoke(): Flow<List<BrewProfile>> = combine(
        getSelectedUserUseCase(),
        brewProfileRepository.brewProfiles,
    ) { user, _ ->
        user?.id?.let { brewProfileRepository.getBrewProfilesForUser(it) }.orEmpty()
    }
}
