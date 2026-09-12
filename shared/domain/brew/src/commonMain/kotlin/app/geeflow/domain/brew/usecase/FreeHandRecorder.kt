package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.brew.model.FreeHandRecording
import app.geeflow.data.brew.model.FreeHandSample
import app.geeflow.data.device.model.DeviceState
import kotlin.time.Instant

/** Captures telemetry, not interpolated chart ticks, and freezes before the post-brew tail. */
internal class FreeHandRecorder(private val startedAt: Instant, private val mode: FreeHandControlMode) {
    private val samples = mutableListOf<FreeHandSample>()
    private var firstMeasurement: Instant? = null
    private var lastMeasurement: Instant? = null
    private var stopped = false

    fun capture(state: DeviceState) {
        if (state.freeHandStopRequested || state.brewStatus != DeviceState.BrewStatus.FreeVariable) stopped = true
        if (stopped) return
        val time = state.telemetryTime ?: return
        if (time < startedAt || lastMeasurement?.let { time <= it } == true) return
        val origin = firstMeasurement ?: time.also { firstMeasurement = it }
        samples.add(
            FreeHandSample(
                elapsedMillis = (time - origin).inWholeMilliseconds,
                data = BrewDataPoint(
                    pressure = state.pressure ?: 0f,
                    weight = state.weight ?: 0f,
                    volume = state.volume ?: 0f,
                    flowRate = state.flowRate ?: 0f,
                    weightRate = state.weightRate ?: 0f,
                ),
            ),
        )
        lastMeasurement = time
    }

    fun snapshot(): FreeHandRecording? = samples.takeIf { it.isNotEmpty() }?.let {
        FreeHandRecording(mode, it.toList())
    }
}
