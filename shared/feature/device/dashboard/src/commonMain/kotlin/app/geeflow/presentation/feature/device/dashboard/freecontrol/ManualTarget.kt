package app.geeflow.presentation.feature.device.dashboard.freecontrol

import kotlin.math.roundToInt

/** Round every step so repeated taps cannot accumulate a register-sized floating point error. */
internal fun steppedManualTarget(value: Float, step: Float, range: ClosedFloatingPointRange<Float>): Float =
    (((value + step) * TargetPrecision).roundToInt() / TargetPrecision).coerceIn(range)

private const val TargetPrecision = 10f

/** Relative pickup: touching the pad never changes the target. Upward motion increases it. */
internal fun draggedManualTarget(
    value: Float,
    deltaY: Float,
    travel: Float,
    range: ClosedFloatingPointRange<Float>,
    fine: Boolean,
): Float {
    if (travel <= 0f) return value.coerceIn(range)
    val gain = if (fine) FineGain else 1f
    return (value - deltaY / travel * (range.endInclusive - range.start) * gain).coerceIn(range)
}

private const val FineGain = 0.25f
