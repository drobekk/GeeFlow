package app.geeflow.data.device.impl.controller

import kotlin.math.abs

/** Incremental feedback for pump flow in ml/s. Call once per fresh measurement, using monotonic time. */
internal class PumpFlowPressureRegulator(private val maximumPressure: Float) {
    private var pressure = 0f
    private var previousTime: Long? = null

    init {
        require(maximumPressure.isFinite() && maximumPressure > 0f)
    }

    fun reset(pressure: Float = 0f) {
        require(pressure.isFinite())
        this.pressure = pressure.coerceIn(0f, maximumPressure)
        previousTime = null
    }

    fun update(targetFlow: Float, measuredFlow: Float, elapsedMillis: Long): Float {
        require(targetFlow.isFinite() && targetFlow >= 0f)
        require(measuredFlow.isFinite() && measuredFlow >= 0f)
        val previous = previousTime
        require(elapsedMillis >= 0 && (previous == null || elapsedMillis >= previous))
        val seconds = if (previous == null) 0f else {
            ((elapsedMillis - previous) / MILLISECONDS_PER_SECOND).coerceAtMost(MAXIMUM_INTERVAL_SECONDS)
        }
        previousTime = elapsedMillis
        if (targetFlow == 0f) {
            pressure = 0f
            return pressure
        }
        val error = targetFlow - measuredFlow
        if (abs(error) > FLOW_DEADBAND) {
            val rate = (error * PRESSURE_GAIN).coerceIn(-MAXIMUM_FALL_RATE, MAXIMUM_RISE_RATE)
            pressure = (pressure + rate * seconds).coerceIn(0f, maximumPressure)
        }
        return pressure
    }
}

private const val MILLISECONDS_PER_SECOND = 1000f
private const val MAXIMUM_INTERVAL_SECONDS = 1f
private const val FLOW_DEADBAND = 0.5f
private const val PRESSURE_GAIN = 0.25f
private const val MAXIMUM_RISE_RATE = 0.5f
private const val MAXIMUM_FALL_RATE = 1f
