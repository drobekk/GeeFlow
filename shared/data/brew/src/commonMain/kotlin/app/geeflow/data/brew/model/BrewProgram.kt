package app.geeflow.data.brew.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Portable recipe. All durations are milliseconds; pressure is bar, flow ml/s, volume ml, mass g. */
@Serializable
sealed interface BrewProgram {
    val version: Int

    @Serializable
    @SerialName("phases")
    data class Phases(
        val phases: List<BrewPhase>, override val version: Int = 1,
        val execution: ExecutionPolicy = ExecutionPolicy(),
        val recipe: RecipeDefinition? = null,
    ) : BrewProgram

    @Serializable
    @SerialName("recording")
    data class Recording(
        val recording: FreeHandRecording, override val version: Int = 1,
        val execution: ExecutionPolicy = ExecutionPolicy(),
        val originalRecording: FreeHandRecording? = null,
        val replayPolicy: ReplayPolicy = ReplayPolicy.Legacy,
    ) : BrewProgram
}

@Serializable
data class BrewPhase(
    val id: String,
    val name: String = "",
    val control: PhaseControl,
    val maximumDurationMillis: Long,
    val minimumDurationMillis: Long = 0,
    val ramp: PhaseRamp = PhaseRamp(),
    /** PhaseTime thresholds use seconds. */
    val exitConditions: List<ExitCondition> = emptyList(),
    val conditionOperator: ConditionOperator = ConditionOperator.Or,
    val adaptiveFlowProbe: AdaptiveFlowProbe? = null,
    val targetSource: TargetSource? = null,
    val entryCaptures: List<CaptureAction> = emptyList(),
    val exitCaptures: List<CaptureAction> = emptyList(),
    val secondaryLimit: SecondaryLimit? = null,
    val temperatureC: Float? = null,
    val stabilityDurationMillis: Long = 0,

)

/** Probe a rising flow target, then hold the fastest measured flow below a pressure ceiling. */
@Serializable
data class AdaptiveFlowProbe(
    val samplingDurationMillis: Long,
    val pressureCeilingBar: Float,
    val flowBackoffMillilitresPerSecond: Float = 0.1f,
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

@Serializable enum class BrewMetric {
    PumpPressure,
    GroupPressure,
    BoilerPressure,
    PumpFlow,
    PumpedVolume,
    CupWeight,
    PhaseTime
}

@Serializable enum class ThresholdComparison { Above, Below, StrictlyAbove, StrictlyBelow }

@Serializable
data class ExitCondition(
    val metric: BrewMetric,
    val comparison: ThresholdComparison,
    val threshold: Float,
    val scope: MeasurementScope = MeasurementScope.Total,
)

@Serializable enum class MeasurementScope { Total, Step }

@Serializable enum class RampStyle { Instant, Linear, EaseIn, EaseOut, EaseInOut }

@Serializable enum class RampStart { PreviousTarget, CurrentMeasurement }

@Serializable
data class PhaseRamp(
    val style: RampStyle = RampStyle.Instant,
    val durationMillis: Long = 0,
    val start: RampStart = RampStart.PreviousTarget,
    val source: TargetSource? = null,
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
    require(version in 1..2) { "Unsupported program version: $version" }
    val policy = when (this) { is BrewProgram.Phases -> execution; is BrewProgram.Recording -> execution }
    require(version == 2 || policy.maximumTotalDurationMillis == 40000L)
    require(policy.maximumTotalDurationMillis in 1..40000) { "Live total deadline may not exceed 40 seconds" }
    when (this) {
        is BrewProgram.Recording -> {
            recording.playbackPoints()
            originalRecording?.playbackPoints()
            require(version == 2 || (originalRecording == null && replayPolicy == ReplayPolicy.Legacy))
        }
        is BrewProgram.Phases -> {
            require(phases.isNotEmpty()) { "The program needs a phase" }
            require(phases.map { it.id }.distinct().size == phases.size) { "Duplicate phase IDs" }
            require(version == 2 || recipe == null)
            recipe?.let { definition ->
                require(definition.templateId.isNotBlank() && definition.source.sha256.matches(Regex("[a-fA-F0-9]{64}")))
                require(definition.source.format.isNotBlank())
                listOf(definition.settings.doseG, definition.settings.ratio, definition.settings.targetYieldG).forEach { value -> require(value == null || (value.isFinite() && value > 0)) }
                require(definition.settings.effectiveYieldG == null || definition.settings.effectiveYieldG?.isFinite() == true)
                BrewProgram.Phases(definition.templatePhases, version = 2).validate()
            }
            val captures = mutableSetOf<String>()
            phases.forEach { phase ->
                require(phase.stabilityDurationMillis in 0..phase.maximumDurationMillis)
                require(phase.temperatureC == null || (phase.temperatureC.isFinite() && phase.temperatureC in 0f..130f))
                require(version == 2 || (phase.targetSource == null && phase.ramp.source == null && phase.entryCaptures.isEmpty() && phase.exitCaptures.isEmpty() && phase.secondaryLimit == null && phase.temperatureC == null && phase.stabilityDurationMillis == 0L))
                fun validateCaptures(actions: List<CaptureAction>) {
                    actions.forEach { action ->
                        require(action.name.isNotBlank() && action.metric != BrewMetric.PhaseTime)
                        require(action.windowMillis in 0..10000 && action.maximumAgeMillis in 1..10000)
                        require(action.minimum.isFinite() && action.maximum.isFinite() && action.minimum >= 0 && action.minimum <= action.maximum)
                        require(action.fallback == null || (action.fallback.isFinite() && action.fallback in action.minimum..action.maximum))
                        require(captures.add(action.name)) { "Duplicate capture: ${action.name}" }
                    }
                }
                validateCaptures(phase.entryCaptures)
                phase.targetSource?.validateSource(captures)
                phase.ramp.source?.validateSource(captures)
                phase.secondaryLimit?.let { limit ->
                    require((phase.control is PhaseControl.Flow && limit.metric == BrewMetric.PumpPressure) ||
                        (phase.control is PhaseControl.Pressure && limit.metric == BrewMetric.PumpFlow))
                    require(limit.minimum == null || (limit.minimum.isFinite() && limit.minimum >= 0 && limit.minimum < limit.ceiling))
                    require(limit.ceiling.isFinite() && limit.ceiling > 0 && limit.hysteresis.isFinite() && limit.hysteresis >= 0 && limit.hysteresis < limit.ceiling)
                    require(limit.reductionPerSecond.isFinite() && limit.reductionPerSecond > 0 && limit.recoveryPerSecond.isFinite() && limit.recoveryPerSecond >= 0)
                    require(limit.overshootAllowance.isFinite() && limit.overshootAllowance >= 0 && limit.tripDurationMillis in 1..10000)
                }
                validateCaptures(phase.exitCaptures)
                require(phase.maximumDurationMillis > 0 && phase.minimumDurationMillis in 0..phase.maximumDurationMillis)
                require(phase.ramp.durationMillis in 0..phase.maximumDurationMillis)
                require(phase.control != PhaseControl.PumpPause || phase.ramp.style == RampStyle.Instant)
                phase.adaptiveFlowProbe?.let { probe ->
                    require(phase.control is PhaseControl.Flow)
                    require(probe.samplingDurationMillis in 1..phase.maximumDurationMillis)
                    require(phase.ramp.style != RampStyle.Instant && phase.ramp.durationMillis >= probe.samplingDurationMillis)
                    require(probe.pressureCeilingBar.isFinite() && probe.pressureCeilingBar > 0f)
                    require(
                        probe.flowBackoffMillilitresPerSecond.isFinite() &&
                            probe.flowBackoffMillilitresPerSecond >= 0f,
                    )
                }
                val value = when (val control = phase.control) {
                    is PhaseControl.Pressure -> control.bar
                    is PhaseControl.Flow -> control.millilitresPerSecond
                    PhaseControl.PumpPause -> 0f
                }
                require(value.isFinite() && value >= 0f) { "Invalid phase target" }
                require(
                    phase.exitConditions.all {
                        it.threshold.isFinite() && it.threshold >= 0f && it.hasValidScope()
                    },
                )
            }
        }
    }
}

private const val RAMP_MIDPOINT = .5f

private fun ExitCondition.hasValidScope(): Boolean = scope == MeasurementScope.Total ||
    metric == BrewMetric.PumpedVolume || metric == BrewMetric.CupWeight

/** A single time condition has a known deadline even when a longer safety limit is configured. */
fun BrewPhase.plannedDurationMillis(): Long {
    val time = exitConditions.singleOrNull()?.takeIf {
        it.metric == BrewMetric.PhaseTime &&
            it.comparison in setOf(ThresholdComparison.Above, ThresholdComparison.StrictlyAbove)
    } ?: return maximumDurationMillis
    return minOf(
        maximumDurationMillis,
        maxOf(
            minimumDurationMillis,
            (time.threshold * MILLISECONDS_PER_SECOND).toLong()
        )
    )
}

/** Time remains a deadline; AND combines the measurement conditions at the current instant. */
fun BrewPhase.conditionsMet(matches: (ExitCondition) -> Boolean): Boolean = when (conditionOperator) {
    ConditionOperator.Or -> exitConditions.any(matches)
    ConditionOperator.And -> {
        val measurements = exitConditions.filterNot { it.metric == BrewMetric.PhaseTime }
        exitConditions.any { it.metric == BrewMetric.PhaseTime && matches(it) } ||
            (measurements.isNotEmpty() && measurements.all(matches))
    }
}

private const val MILLISECONDS_PER_SECOND = 1000L
