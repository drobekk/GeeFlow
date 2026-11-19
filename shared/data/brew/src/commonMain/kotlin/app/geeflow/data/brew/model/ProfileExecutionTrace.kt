package app.geeflow.data.brew.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ProfileExecutionTrace(
    val profile: BrewProfile,
    val startedAt: Instant? = null,
    val measurements: List<FreeHandSample> = emptyList(),
    val targets: List<EmittedTarget> = emptyList(),
    val transitions: List<PhaseTransition> = emptyList(),
    val endReason: String? = null,
    val stopMode: String? = null,
    val scaleSource: String? = null,
    val stopTargetGrams: Float? = null,
    val stopRequestedAt: Instant? = null,
    val stopConfirmedAt: Instant? = null,
    val finalScaleWeightGrams: Float? = null,
    val captures: List<CapturedMeasurement> = emptyList(),
    val decisions: List<ControlDecision> = emptyList(),
    val scaleSamples: List<BrewScaleSample> = emptyList(),
    val grindSetting: String? = null,
    val doseGrams: Float? = null,
    val extractionStartMillis: Long? = null,
    val extractionStartEstimated: Boolean? = null,
    val machineDiagnostics: List<ProfileMachineDiagnostic> = emptyList(),
)

@Serializable data class ProfileMachineDiagnostic(val recordedAt: Instant, val type: String, val fields: Map<String, String>)

@Serializable data class EmittedTarget(val elapsedMillis: Long, val phaseId: String?, val target: PhaseControl)

@Serializable enum class PhaseExitReason { MaximumDuration, ConditionMatched }

@Serializable data class PhaseTransition(
    val elapsedMillis: Long,
    val phaseId: String,
    val reason: PhaseExitReason,
    val condition: ExitCondition? = null,
)

@Serializable data class CapturedMeasurement(val elapsedMillis: Long, val name: String, val metric: BrewMetric, val value: Float, val sampledAtMillis: Long?, val fallbackUsed: Boolean)
@Serializable data class ControlDecision(val elapsedMillis: Long, val phaseId: String, val requested: PhaseControl, val limited: PhaseControl, val previousApplied: PhaseControl?)
