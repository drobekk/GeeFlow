package app.geeflow.presentation.feature.device.dashboard.model

import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.BrewPhase
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.ConditionOperator
import app.geeflow.data.brew.model.ExitCondition
import app.geeflow.data.brew.model.PhaseExitReason
import app.geeflow.data.brew.model.PhaseTransition
import app.geeflow.data.brew.model.ThresholdComparison
import app.geeflow.data.brew.model.plannedDurationMillis

internal fun BrewProgram?.chartBoundaries(transitions: List<PhaseTransition>): List<ChartPhaseBoundary> {
    val phases = (this as? BrewProgram.Phases)?.phases.orEmpty()
    if (phases.isEmpty()) {
        return transitions.mapIndexed { index, transition ->
            ChartPhaseBoundary(
                seconds = transition.elapsedMillis / 1000.0,
                stepNumber = index + 1,
                conditions = listOfNotNull(transition.condition),
                matchedConditions = setOfNotNull(transition.condition),
                actual = true,
            )
        }
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
                    matchedConditions = phase.matchedConditions(transition = transition, time = time),
                    actual = transition != null,
                ),
            )
        }
    }
}

internal fun BrewProgram?.nativeChartBoundaries(
    data: Map<Float, ChartData>,
    finishCondition: Condition?,
): List<ChartPhaseBoundary> {
    val goal = finishCondition.toExitCondition()
    val reachedAt = goal?.let { condition ->
        data.entries.sortedBy { it.key }.firstOrNull { (_, point) ->
            val value = if (condition.metric == BrewMetric.CupWeight) point.weight else point.volume
            value >= condition.threshold
        }?.key?.toDouble()
    }
    val elapsed = data.keys.maxOrNull()?.toDouble() ?: 0.0
    val original = nativePlannedBoundaries()
    val planned = original.mapIndexed { index, boundary ->
        if (goal != null && index == original.lastIndex) {
            boundary.copy(conditions = listOf(goal))
        } else {
            boundary
        }
    }
    if (planned.isEmpty()) return emptyList()
    val completed = planned.filter { it.seconds > 0.0 && (reachedAt == null || it.seconds < reachedAt) }
    val boundaries = completed.map { boundary ->
        val time = boundary.conditions.firstOrNull { it.metric == BrewMetric.PhaseTime }
        boundary.copy(
            matchedConditions = setOfNotNull(time.takeIf { boundary.seconds <= elapsed }),
            actual = time != null && boundary.seconds <= elapsed,
        )
    }
    if (reachedAt != null) {
        val current = planned.firstOrNull { it.seconds >= reachedAt } ?: planned.last()
        return boundaries.filter { it.stepNumber != current.stepNumber } + current.copy(
            seconds = reachedAt,
            conditions = listOf(goal),
            matchedConditions = setOf(goal),
            actual = true,
        )
    }
    return boundaries
}

private fun Condition?.toExitCondition(): ExitCondition? = when (this) {
    is Condition.Volume -> ExitCondition(
        metric = BrewMetric.PumpedVolume,
        comparison = ThresholdComparison.Above,
        threshold = target,
    )
    is Condition.Weight -> ExitCondition(
        metric = BrewMetric.CupWeight,
        comparison = ThresholdComparison.Above,
        threshold = target,
    )
    null -> null
}

private fun BrewProgram?.nativePlannedBoundaries(): List<ChartPhaseBoundary> {
    val boundaries = chartBoundaries(emptyList())
    if (boundaries.isNotEmpty()) return boundaries
    val recording = this as? BrewProgram.Recording ?: return emptyList()
    val duration = recording.recording.samples.lastOrNull()?.elapsedMillis ?: return emptyList()
    return listOf(ChartPhaseBoundary(seconds = duration / 1000.0))
}

private fun BrewPhase.matchedConditions(transition: PhaseTransition?, time: ExitCondition): Set<ExitCondition> = when {
    transition == null -> emptySet()
    transition.reason == PhaseExitReason.MaximumDuration -> setOf(time)
    transition.condition?.metric == BrewMetric.PhaseTime -> setOfNotNull(transition.condition)
    conditionOperator == ConditionOperator.And && transition.condition != null ->
        exitConditions.filterNot { it.metric == BrewMetric.PhaseTime }.toSet()
    else -> setOfNotNull(transition.condition)
}
