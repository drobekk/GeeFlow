package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewProfileRepository
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.validate
import app.geeflow.domain.user.usecase.GetSelectedUserUseCase
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

/**
 * Creates a profile when [id] is [BrewProfile.NEW_ID], otherwise updates the existing one, keeping the
 * fields the editor does not expose (mode, auto link, position) untouched.
 */
@Factory
class SaveBrewProfileUseCase(
    private val brewProfileRepository: BrewProfileRepository,
    private val getSelectedUserUseCase: GetSelectedUserUseCase,
) {
    suspend operator fun invoke(
        id: Long,
        name: String,
        description: String,
        finishCondition: Condition?,
        program: BrewProgram,
    ) {
        program.validate()
        val userId = getSelectedUserUseCase().first()?.id ?: error("no user selected")
        val existing = brewProfileRepository.getBrewProfileById(id)
        val profile = existing?.copy(
            name = name,
            description = description,
            finishCondition = finishCondition,
            program = program,
        ) ?: BrewProfile(
            userId = userId,
            name = name,
            description = description,
            finishCondition = finishCondition,
            program = program,
            position = brewProfileRepository.getBrewProfilesForUser(userId).size,
        )
        brewProfileRepository.addBrewProfile(profile)
    }
}
