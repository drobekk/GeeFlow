package app.geeflow.domain.device

import app.geeflow.data.brew.model.FreeHandControlMode

enum class LiveTargetEncodingVariant(val id: String, val label: String) {
    ScaledCanonical("scaled_canonical", "×10, pressure first"),
    RawCanonical("raw_canonical", "Raw, pressure first"),
    ScaledSwapped("scaled_swapped", "×10, words swapped"),
    RawSwapped("raw_swapped", "Raw, words swapped"),
}

enum class LiveModeEncodingVariant(val id: String, val label: String) {
    PressureZeroFlowOne("pressure_0_flow_1", "Pressure=0, flow=1"),
    PressureOneFlowZero("pressure_1_flow_0", "Pressure=1, flow=0");

    fun valueFor(mode: FreeHandControlMode): Int = when (this) {
        PressureZeroFlowOne -> if (mode == FreeHandControlMode.Pressure) 0 else 1
        PressureOneFlowZero -> if (mode == FreeHandControlMode.Pressure) 1 else 0
    }
}

data class LiveCommandEncodingProbeStep(
    val index: Int,
    val variant: LiveTargetEncodingVariant,
    val modeEncoding: LiveModeEncodingVariant,
    val mode: FreeHandControlMode,
    val targetValue: Float,
    val targetUnit: String,
    val words: List<Int>,
    val holdMillis: Long,
)

/** Sixteen low setpoints cover all target-word and mode-value combinations. */
object LiveCommandEncodingProbeScript {
    const val PRESSURE_TARGET_BAR = 2f
    const val FLOW_TARGET_ML_PER_SECOND = 2f
    // Multiple telemetry intervals per combination, with a bounded full-matrix runtime.
    const val HOLD_MILLIS = 4_000L

    val steps: List<LiveCommandEncodingProbeStep> = LiveModeEncodingVariant.entries.flatMap { modeEncoding ->
        LiveTargetEncodingVariant.entries.flatMap { variant ->
            listOf(
                step(variant, modeEncoding, FreeHandControlMode.Pressure, PRESSURE_TARGET_BAR, "bar"),
                step(variant, modeEncoding, FreeHandControlMode.Flow, FLOW_TARGET_ML_PER_SECOND, "ml/s"),
            )
        }
    }.mapIndexed { index, step -> step.copy(index = index + 1) }

    private fun step(
        variant: LiveTargetEncodingVariant,
        modeEncoding: LiveModeEncodingVariant,
        mode: FreeHandControlMode,
        value: Float,
        unit: String,
    ) = LiveCommandEncodingProbeStep(
        index = 0,
        variant = variant,
        modeEncoding = modeEncoding,
        mode = mode,
        targetValue = value,
        targetUnit = unit,
        words = encode(variant, mode, value),
        holdMillis = HOLD_MILLIS,
    )

    private fun encode(
        variant: LiveTargetEncodingVariant,
        mode: FreeHandControlMode,
        value: Float,
    ): List<Int> {
        val scaled = when (variant) {
            LiveTargetEncodingVariant.ScaledCanonical,
            LiveTargetEncodingVariant.ScaledSwapped -> (value * 10).toInt()
            LiveTargetEncodingVariant.RawCanonical,
            LiveTargetEncodingVariant.RawSwapped -> value.toInt()
        }
        val canonical = if (mode == FreeHandControlMode.Pressure) listOf(scaled, 0) else listOf(0, scaled)
        return when (variant) {
            LiveTargetEncodingVariant.ScaledCanonical,
            LiveTargetEncodingVariant.RawCanonical -> canonical
            LiveTargetEncodingVariant.ScaledSwapped,
            LiveTargetEncodingVariant.RawSwapped -> canonical.reversed()
        }
    }
}
