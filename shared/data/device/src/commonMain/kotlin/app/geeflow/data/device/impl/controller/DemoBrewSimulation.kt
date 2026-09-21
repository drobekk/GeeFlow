package app.geeflow.data.device.impl.controller

import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.PhaseControl
import kotlin.math.exp
import kotlin.math.sin

/** Illustrative pump and puck response, not a calibrated model of a particular machine. */
internal class DemoBrewSimulation {
    private var elapsed = 0f
    private var pressure = 0f
    private var flow = 0f
    private var volume = 0f
    private var weight = 0f
    private var absorbed = 0f
    private var retained = 0f

    fun advance(control: PhaseControl, seconds: Float): BrewDataPoint {
        require(seconds.isFinite() && seconds > 0f && seconds <= MAX_INTERVAL_SECONDS)
        elapsed += seconds
        val saturation = 1f - exp(-volume / WETTING_VOLUME)
        val extraction = 1f - exp(-weight / EXTRACTION_WEIGHT)
        val resistance = DRY_RESISTANCE + WET_RESISTANCE * saturation - EXTRACTION_RESISTANCE * extraction
        val target = when (control) {
            is PhaseControl.Pressure -> control.bar
            is PhaseControl.Flow -> control.millilitresPerSecond * resistance
            PhaseControl.PumpPause -> 0f
        }.coerceIn(0f, MAX_PRESSURE)
        val ripple = 1f + RIPPLE_AMPLITUDE * sin(elapsed * RIPPLE_FREQUENCY)
        pressure = approach(pressure, (target * ripple).coerceAtMost(MAX_PRESSURE), seconds, PRESSURE_RESPONSE)
        flow = approach(flow, (pressure / resistance).coerceIn(0f, MAX_FLOW), seconds, FLOW_RESPONSE)
        val pumped = flow * seconds
        volume += pumped
        val absorbedNow = minOf((ABSORPTION_VOLUME - absorbed).coerceAtLeast(0f), pumped * ABSORPTION_FRACTION)
        absorbed += absorbedNow
        retained += pumped - absorbedNow
        val delivered = retained * (1f - exp(-seconds / CUP_DELAY))
        retained -= delivered
        weight += delivered
        return BrewDataPoint(
            pressure = pressure,
            flowRate = flow,
            volume = volume,
            weight = weight,
            weightRate = delivered / seconds,
        )
    }

    private fun approach(current: Float, target: Float, seconds: Float, response: Float): Float =
        current + (target - current) * (1f - exp(-seconds / response))

    private companion object {
        const val MAX_INTERVAL_SECONDS = 1f
        const val MAX_PRESSURE = 12f
        const val MAX_FLOW = 8f
        const val PRESSURE_RESPONSE = .45f
        const val FLOW_RESPONSE = .25f
        const val DRY_RESISTANCE = 1.2f
        const val WET_RESISTANCE = 2.2f
        const val EXTRACTION_RESISTANCE = .8f
        const val WETTING_VOLUME = 6f
        const val EXTRACTION_WEIGHT = 40f
        const val ABSORPTION_VOLUME = 6f
        const val ABSORPTION_FRACTION = 1f
        const val CUP_DELAY = .6f
        const val RIPPLE_AMPLITUDE = .008f
        const val RIPPLE_FREQUENCY = 3.7f
    }
}
