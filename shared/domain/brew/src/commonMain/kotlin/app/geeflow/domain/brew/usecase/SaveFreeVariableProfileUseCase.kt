package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewProfileRepository
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.BrewSession
import app.geeflow.data.brew.model.Condition
import app.geeflow.domain.user.usecase.GetSelectedUserUseCase
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class SaveFreeVariableProfileUseCase(
    private val brewProfileRepository: BrewProfileRepository,
    private val getSelectedUserUseCase: GetSelectedUserUseCase,
) {
    suspend operator fun invoke(session: BrewSession, name: String) {
        val userId = getSelectedUserUseCase().first()?.id ?: error("no user selected")
        val recording = requireNotNull(session.recording) { "No free hand recording available" }
        recording.playbackPoints() // Validate the transport capacity before persisting.
        val target = recording.samples.last().data.volume
        require(target > 0f) { "The recording has no measured volume" }
        val profile = BrewProfile(
            userId = userId,
            name = name,
            description = "",

            finishCondition = Condition.Volume(target),
            program = BrewProgram.Recording(recording),
        )
        brewProfileRepository.addBrewProfile(profile)
    }
}
