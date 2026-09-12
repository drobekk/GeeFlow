package app.geeflow.data.brew.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Portable recipe. All durations are milliseconds; pressure is bar, flow ml/s, volume ml, mass g. */
@Serializable
sealed interface BrewProgram {
    val version: Int

    @Serializable
    @SerialName("phases")
    data class Phases(val phases: List<BrewPhase>, override val version: Int = 1) : BrewProgram

    @Serializable
    @SerialName("recording")
    data class Recording(val recording: FreeHandRecording, override val version: Int = 1) : BrewProgram
}

@Serializable
data class BrewPhase(
    val id: String,
    val name: String = "",
    val control: PhaseControl,
    val maximumDurationMillis: Long,
    val minimumDurationMillis: Long = 0,
    val ramp: PhaseRamp = PhaseRamp(),
    /** OR: the first satisfied condition exits the phase. Volume is relative to phase entry. */
    val exitConditions: List<ExitCondition> = emptyList(),
)

@Serializable
sealed interface PhaseControl {
    @Serializable
    @SerialName("pressure")
    data class Pressure(val bar: Float, val location: PressureLocation = PressureLocation.Pump) : PhaseControl

    @Serializable
    @SerialName("flow")
    data class Flow(val millilitresPerSecond: Float) : PhaseControl

    @Serializable
    @SerialName("pump_pause")
    data object PumpPause : PhaseControl
}

@Serializable enum class PressureLocation { Pump, Group, Boiler }

@Serializable enum class BrewMetric { PumpPressure, GroupPressure, BoilerPressure, PumpFlow, PumpedVolume, CupWeight }

@Serializable enum class ThresholdComparison { Above, Below }

@Serializable
data class ExitCondition(val metric: BrewMetric, val comparison: ThresholdComparison, val threshold: Float)

@Serializable enum class RampStyle { Instant, Linear, EaseIn, EaseOut, EaseInOut }

@Serializable enum class RampStart { PreviousTarget, CurrentMeasurement }

@Serializable
data class PhaseRamp(
    val style: RampStyle = RampStyle.Instant,
    val durationMillis: Long = 0,
    val start: RampStart = RampStart.PreviousTarget,
) {
    fun fraction(elapsedMillis: Long): Float {
        if (style == RampStyle.Instant || durationMillis == 0L) return 1f
        val t = (elapsedMillis.toFloat() / durationMillis).coerceIn(0f, 1f)
        return when (style) {
            RampStyle.Instant -> 1f
            RampStyle.Linear -> t
            RampStyle.EaseIn -> t * t
            RampStyle.EaseOut -> 1f - (1f - t) * (1f - t)
            RampStyle.EaseInOut -> if (t < RAMP_MIDPOINT) 2f * t * t else 1f - 2f * (1f - t) * (1f - t)
        }
    }
}

fun List<ProfileStep>.toProgram(): BrewProgram.Phases = BrewProgram.Phases(
    mapIndexed { index, step ->
        BrewPhase(
            id = index.toString(),
            control = when (step) {
                is ProfileStep.Pressure -> PhaseControl.Pressure(step.pressure)
                is ProfileStep.Flow -> PhaseControl.Flow(step.flow)
                is ProfileStep.Wait -> PhaseControl.PumpPause
            },
            maximumDurationMillis = step.time * 1000L,
        )
    }
)

fun BrewProgram.validate() {
    require(version == 1) { "Unsupported program version: $version" }
    when (this) {
        is BrewProgram.Recording -> recording.playbackPoints()
        is BrewProgram.Phases -> {
            require(phases.isNotEmpty()) { "The program needs a phase" }
            require(phases.map { it.id }.distinct().size == phases.size) { "Duplicate phase IDs" }
            phases.forEach { phase ->
                require(phase.maximumDurationMillis > 0 && phase.minimumDurationMillis in 0..phase.maximumDurationMillis)
                require(phase.ramp.durationMillis in 0..phase.maximumDurationMillis)
                require(phase.control != PhaseControl.PumpPause || phase.ramp.style == RampStyle.Instant)
                val value = when (val control = phase.control) {
                    is PhaseControl.Pressure -> control.bar
                    is PhaseControl.Flow -> control.millilitresPerSecond
                    PhaseControl.PumpPause -> 0f
                }
                require(value.isFinite() && value >= 0f) { "Invalid phase target" }
                require(phase.exitConditions.all { it.threshold.isFinite() && it.threshold >= 0f })
            }
        }
    }
}

private const val RAMP_MIDPOINT = .5f
