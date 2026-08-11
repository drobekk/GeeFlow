package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewProfileRepository
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.domain.user.usecase.GetSelectedUserUseCase
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class DuplicateProfileUseCase(
    private val brewProfileRepository: BrewProfileRepository,
    private val getSelectedUserUseCase: GetSelectedUserUseCase,
) {
    suspend operator fun invoke(profileId: Long, name: String) {
        val userId = getSelectedUserUseCase().first()?.id ?: error("no user selected")
        val source = brewProfileRepository.getBrewProfileById(profileId) ?: return
        brewProfileRepository.addBrewProfile(
            source.copy(
                id = BrewProfile.NEW_ID,
                userId = userId,
                name = name,
                autoLinkOpen = false,
                position = brewProfileRepository.getBrewProfilesForUser(userId).size,
            ),
        )
    }
}
