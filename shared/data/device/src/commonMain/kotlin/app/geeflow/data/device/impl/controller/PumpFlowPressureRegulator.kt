package app.geeflow.data.device.impl.controller

import kotlin.math.abs

/** Incremental feedback for pump flow in ml/s. Call once per fresh measurement, using monotonic time. */
internal class PumpFlowPressureRegulator(private val maximumPressure: Float) {
    private var pressure = 0f
    private var previousTime: Long? = null
    private var previousFlow: Float? = null
    private var previousTarget: Float? = null
    private var previousCorrection = 0f

    init {
        require(maximumPressure.isFinite() && maximumPressure > 0f)
    }

    fun reset(pressure: Float = 0f) {
        require(pressure.isFinite())
        this.pressure = pressure.coerceIn(0f, maximumPressure)
        previousTime = null
        previousFlow = null
        previousTarget = null
        previousCorrection = 0f
    }

    fun update(targetFlow: Float, measuredFlow: Float, elapsedMillis: Long): Float {
        require(targetFlow.isFinite() && targetFlow >= 0f)
        require(measuredFlow.isFinite() && measuredFlow >= 0f)
        val previous = previousTime
        require(elapsedMillis >= 0 && (previous == null || elapsedMillis >= previous))
        val seconds = if (previous == null) {
            0f
        } else {
            ((elapsedMillis - previous) / MILLISECONDS_PER_SECOND).coerceAtMost(MAXIMUM_INTERVAL_SECONDS)
        }
        previousTime = elapsedMillis
        if (targetFlow == 0f) {
            reset()
            return pressure
        }
        if (previous != null && elapsedMillis == previous) return pressure
        val error = targetFlow - measuredFlow
        val scale = correctionScale(targetFlow, measuredFlow, error, elapsedMillis - (previous ?: elapsedMillis))
        val beforeCorrection = pressure
        if (abs(error) > FLOW_DEADBAND) {
            val rate = (error * PRESSURE_GAIN).coerceIn(-MAXIMUM_FALL_RATE, MAXIMUM_RISE_RATE)
            pressure = (pressure + rate * seconds * scale).coerceIn(0f, maximumPressure)
        }
        previousCorrection = pressure - beforeCorrection
        previousFlow = measuredFlow
        previousTarget = targetFlow
        return pressure
    }

    private fun correctionScale(targetFlow: Float, measuredFlow: Float, error: Float, intervalMillis: Long): Float {
        val lastFlow = previousFlow ?: return 1f
        if (targetFlow != previousTarget || intervalMillis > MAXIMUM_TREND_INTERVAL_MS) return 1f
        val flowChange = measuredFlow - lastFlow
        val approachingTarget = error * flowChange > 0f && abs(flowChange) >= MINIMUM_FLOW_CHANGE
        val correctionTakingEffect = error * previousCorrection > 0f
        // Give the previous correction time to take effect instead of accumulating more pressure changes.
        return if (approachingTarget && correctionTakingEffect) APPROACHING_TARGET_SCALE else 1f
    }
}

private const val MILLISECONDS_PER_SECOND = 1000f
private const val MAXIMUM_INTERVAL_SECONDS = 0.25f
private const val FLOW_DEADBAND = 0.5f
private const val PRESSURE_GAIN = 1f
private const val MAXIMUM_RISE_RATE = 2f
private const val MAXIMUM_FALL_RATE = 2f
private const val MAXIMUM_TREND_INTERVAL_MS = 1000L
private const val MINIMUM_FLOW_CHANGE = 0.05f
private const val APPROACHING_TARGET_SCALE = 0.3f
