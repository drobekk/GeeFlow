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
)

@Serializable data class EmittedTarget(val elapsedMillis: Long, val phaseId: String?, val target: PhaseControl)

@Serializable enum class PhaseExitReason { MaximumDuration, ConditionMatched }

@Serializable data class PhaseTransition(
    val elapsedMillis: Long,
    val phaseId: String,
    val reason: PhaseExitReason,
    val condition: ExitCondition? = null,
)
