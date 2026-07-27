package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewProfileRepository
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewSession
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.ProfileMode
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.domain.user.usecase.GetSelectedUserUseCase
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class SaveFreeVariableProfileUseCase(
    private val brewProfileRepository: BrewProfileRepository,
    private val getSelectedUserUseCase: GetSelectedUserUseCase,
) {
    suspend operator fun invoke(session: BrewSession, name: String, isFlow: Boolean) {
        val userId = getSelectedUserUseCase().first()?.id ?: error("no user selected")
        val steps = session.dataPoints.entries
            .sortedBy { it.key }
            .map { (timeSec, point) ->
                if (isFlow) {
                    ProfileStep.Flow(timeSec.toInt(), point.flowRate)
                } else {
                    ProfileStep.Pressure(timeSec.toInt(), point.pressure)
                }
            }
        val profile = BrewProfile(
            userId = userId,
            name = name,
            description = "",
            mode = ProfileMode.FreeVariable,
            finishCondition = Condition.Volume(0f),
            steps = steps,
        )
        brewProfileRepository.addBrewProfile(profile)
    }
}
