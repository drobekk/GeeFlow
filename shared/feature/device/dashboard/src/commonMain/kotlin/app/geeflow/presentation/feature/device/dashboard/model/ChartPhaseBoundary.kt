package app.geeflow.presentation.feature.device.dashboard.model

import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.ExitCondition
import app.geeflow.data.brew.model.PhaseExitReason
import app.geeflow.data.brew.model.PhaseTransition
import app.geeflow.data.brew.model.ThresholdComparison
import app.geeflow.data.brew.model.plannedDurationMillis

internal data class ChartPhaseBoundary(
    val seconds: Double,
    val stepNumber: Int? = null,
    val conditions: List<ExitCondition> = emptyList(),
    val matchedCondition: ExitCondition? = null,
    val actual: Boolean = false,
)

internal fun BrewProgram?.chartBoundaries(transitions: List<PhaseTransition>): List<ChartPhaseBoundary> {
    val phases = (this as? BrewProgram.Phases)?.phases.orEmpty()
    if (phases.isEmpty()) return transitions.mapIndexed { index, transition ->
        ChartPhaseBoundary(
            seconds = transition.elapsedMillis / 1000.0,
            stepNumber = index + 1,
            conditions = listOfNotNull(transition.condition),
            matchedCondition = transition.condition,
            actual = true,
        )
    }
    var elapsedMillis = 0L
    return buildList {
        add(ChartPhaseBoundary(seconds = 0.0, stepNumber = null))
        phases.forEachIndexed { index, phase ->
            val transition = transitions.firstOrNull { it.phaseId == phase.id }
            elapsedMillis = transition?.elapsedMillis ?: (elapsedMillis + phase.plannedDurationMillis())
            val time = phase.exitConditions.firstOrNull { it.metric == BrewMetric.PhaseTime }
                ?: ExitCondition(
                    metric = BrewMetric.PhaseTime,
                    comparison = ThresholdComparison.Above,
                    threshold = phase.maximumDurationMillis / 1000f,
                )
            val conditions = listOf(time) + phase.exitConditions.filter { it.metric != BrewMetric.PhaseTime }
            add(
                ChartPhaseBoundary(
                    seconds = elapsedMillis / 1000.0,
                    stepNumber = index + 1,
                    conditions = conditions,
                    matchedCondition = if (transition?.reason == PhaseExitReason.MaximumDuration) time else transition?.condition,
                    actual = transition != null,
                ),
            )
        }
    }
}
