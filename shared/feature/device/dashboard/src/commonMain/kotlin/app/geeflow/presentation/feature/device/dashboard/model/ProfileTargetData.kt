package app.geeflow.presentation.feature.device.dashboard.model

import app.geeflow.data.brew.model.ProfileStep

private const val TickScale = 10

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
