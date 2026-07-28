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
        // dataPoints is keyed by absolute elapsed seconds at 0.1s resolution (see
        // ObserveBrewDataUseCase.TICKS_PER_SECOND), but ProfileStep.time is a duration
        // relative to the previous step (see WendougeeProfileCompiler, DemoDeviceController,
        // ProfileListViewModel) — collapse to one point per whole second, then convert
        // each entry's absolute second into the delta until the next one.
        val pointBySecond = session.dataPoints.entries
            .groupBy { it.key.toInt() }
            .mapValues { (_, entries) -> entries.maxBy { it.key }.value }
        val seconds = pointBySecond.keys.sorted()
        val steps = seconds.mapIndexed { index, second ->
            val point = pointBySecond.getValue(second)
            val duration = seconds.getOrNull(index + 1)?.minus(second) ?: DEFAULT_STEP_DURATION_SEC
            if (isFlow) {
                ProfileStep.Flow(duration, point.flowRate)
            } else {
                ProfileStep.Pressure(duration, point.pressure)
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

    private companion object {
        private const val DEFAULT_STEP_DURATION_SEC = 1
    }
}
