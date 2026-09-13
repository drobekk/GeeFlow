package app.geeflow.presentation.feature.device.dashboard.profileeditor

import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.ExitCondition
import app.geeflow.data.brew.model.PhaseRamp
import app.geeflow.data.brew.model.RampStyle
import kotlin.math.ceil

internal fun StepEditorViewState.toStep(): ProfileEditorViewState.Step? {
    val maximum = conditions.firstOrNull { it.metric == BrewMetric.PhaseTime }?.value
        ?.finiteNumber()?.takeIf { it > 0 && it <= Int.MAX_VALUE / MillisecondsPerSecond } ?: return null
    val value = if (type == StepType.Wait) 0f else target.finiteNumber() ?: return null
    val rampDuration = rampSeconds.finiteNumber()
    val usesRamp = type != StepType.Wait && rampStyle != RampStyle.Instant
    val thresholds = conditions.map { it.value.finiteNumber() }
    val validThresholds = thresholds.all { it != null && it >= 0 }
    val validRamp = !usesRamp || (rampDuration != null && rampDuration > 0 && rampDuration <= maximum)
    val validTarget = type == StepType.Wait || value in targetRange
    if (!validThresholds || !validRamp || !validTarget) return null
    val ramp = if (usesRamp) {
        PhaseRamp(
            style = rampStyle,
            durationMillis = (requireNotNull(rampDuration) * MillisecondsPerSecond).toLong(),
            start = rampStart,
        )
    } else {
        PhaseRamp()
    }
    val exits = conditions.zip(thresholds) { condition, threshold ->
        ExitCondition(
            metric = condition.metric,
            comparison = condition.comparison,
            threshold = requireNotNull(threshold),
        )
    }
    return source.copy(
        type = type,
        phaseName = name,
        value = value,
        timeSec = ceil(maximum).toInt(),
        minimumDurationMillis = 0,
        ramp = ramp,
        exitConditions = exits,
    )
}

private fun String.finiteNumber() = replace(',', '.').toFloatOrNull()?.takeIf { it.isFinite() }
private const val MillisecondsPerSecond = 1000f
