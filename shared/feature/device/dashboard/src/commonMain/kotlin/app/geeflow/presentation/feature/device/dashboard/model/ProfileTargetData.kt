package app.geeflow.presentation.feature.device.dashboard.model

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.FreeHandRecording
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.PhaseTransition
import app.geeflow.data.brew.model.ProfileExecutionTrace
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.data.brew.model.RampStart
import app.geeflow.data.brew.model.plannedDurationMillis

private const val TickScale = 10
private const val MillisecondsPerSecond = 1000f

private data class StepEvent(val time: Float, val pressure: Float, val flow: Float)

/**
 * Expands profile steps into the per-tick target curve charts render. [ProfileStep.time] is a
 * duration relative to the previous step, so times are accumulated while walking the list. A step
 * only drives the value it targets, and only for its own duration — the channel it does not control
 * reads zero rather than holding the value of an earlier step.
 */
internal fun List<ProfileStep>.toTargetData(): Map<Float, ChartData> {
    if (isEmpty()) return emptyMap()

    val events = mutableListOf<StepEvent>()
    var currentTime = 0f

    for (step in this) {
        val event = when (step) {
            is ProfileStep.Pressure -> StepEvent(currentTime, pressure = step.pressure, flow = 0f)
            is ProfileStep.Flow -> StepEvent(currentTime, pressure = 0f, flow = step.flow)
            is ProfileStep.Wait -> StepEvent(currentTime, pressure = 0f, flow = 0f)
        }
        events.add(event)
        currentTime += step.time.toFloat()
    }

    val totalTicks = (currentTime * TickScale).toInt()
    return buildMap(totalTicks + 1) {
        for (tick in 0..totalTicks) {
            val time = tick / TickScale.toFloat()
            val event = events.lastOrNull { it.time <= time } ?: events.first()
            put(
                key = time,
                value = ChartData(
                    pressure = event.pressure,
                    weight = 0f,
                    weightPerSecond = 0f,
                    volume = 0f,
                    volumePerSecond = event.flow,
                ),
            )
        }
    }
}

/** Raw measurements keep their actual timestamps in previews and history. */
internal fun FreeHandRecording.toTargetData(): Map<Float, ChartData> = BrewProgram.Recording(this).toTargetData()

internal fun BrewProfile.toTargetData(): Map<Float, ChartData> = program.toTargetData()

/** Conditional phases use their maximum duration. Measured ramp starts cannot be predicted. */
@Suppress("ComplexMethod")
internal fun BrewProgram.toTargetData(
    transitions: List<PhaseTransition> = emptyList(),
): Map<Float, ChartData> = when (this) {
    is BrewProgram.Recording -> recording.samples.associate { sample ->
        sample.elapsedMillis / MillisecondsPerSecond to ChartData(
            pressure = sample.data.pressure,
            volumePerSecond = sample.data.flowRate,
            volume = sample.data.volume,
            weight = sample.data.weight,
            weightPerSecond = sample.data.weightRate,
        )
    }

    is BrewProgram.Phases -> toPhase(transitions)
}

private fun BrewProgram.Phases.toPhase(transitions: List<PhaseTransition>): Map<Float, ChartData> = buildMap {
    var offset = 0L
    var previous: PhaseControl? = null
    for (phase in phases) {
        val target = when (val control = phase.control) {
            is PhaseControl.Pressure -> control.bar
            is PhaseControl.Flow -> control.millilitresPerSecond
            is PhaseControl.PumpPause -> 0f
        }
        val from = if (phase.ramp.start == RampStart.PreviousTarget) {
            when (phase.control) {
                is PhaseControl.Pressure if previous is PhaseControl.Pressure -> previous.bar
                is PhaseControl.Flow if previous is PhaseControl.Flow -> previous.millilitresPerSecond
                else -> 0f
            }
        } else {
            // Actual measurement is unavailable in a preview; show an estimated ramp from rest.
            0f
        }
        val duration = transitions.firstOrNull { it.phaseId == phase.id }
            ?.let { (it.elapsedMillis - offset).coerceAtLeast(0) }
            ?: phase.plannedDurationMillis()
        val times = (
            (0..duration step PreviewIntervalMillis).toList() +
                listOf(phase.ramp.durationMillis.coerceIn(0, duration), (duration - 1).coerceAtLeast(0), duration)
            )
            .distinct().sorted()
        for (time in times) {
            val value = from + (target - from) * phase.ramp.fraction(time)
            put(
                (offset + time) / MillisecondsPerSecond,
                ChartData(
                    pressure = if (phase.control is PhaseControl.Pressure) value else 0f,
                    volumePerSecond = if (phase.control is PhaseControl.Flow) value else 0f,
                    volume = 0f,
                    weight = 0f,
                    weightPerSecond = 0f,
                ),
            )
        }
        offset += duration
        previous = phase.control
    }
}

internal fun ProfileExecutionTrace.toTargetData(): Map<Float, ChartData> = buildMap {
    targets.forEachIndexed { index, sample ->
        val point = ChartData(
            pressure = (sample.target as? PhaseControl.Pressure)?.bar ?: 0f,
            volumePerSecond = (sample.target as? PhaseControl.Flow)?.millilitresPerSecond ?: 0f,
            volume = 0f,
            weight = 0f,
            weightPerSecond = 0f,
        )
        put(sample.elapsedMillis / MillisecondsPerSecond, point)
        // A sent target is held until the next command; it is not a linear interpolation.
        targets.getOrNull(index + 1)?.let { next ->
            if (next.elapsedMillis > sample.elapsedMillis) put((next.elapsedMillis - 1) / MillisecondsPerSecond, point)
        }
    }
    val end = transitions.lastOrNull()?.elapsedMillis
    val last = entries.lastOrNull()?.value
    if (end != null && last != null && end >= (targets.lastOrNull()?.elapsedMillis ?: 0)) {
        put(end / MillisecondsPerSecond, last)
    }
}

private const val PreviewIntervalMillis = 100L
