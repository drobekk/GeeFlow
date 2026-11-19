package app.geeflow.data.brew.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Data-only sources. No expressions or executable recipe scripts. */
@Serializable enum class TargetSourceKind { Constant, MeasurementAtEntry, CapturedValue, PreviousGoal, PreviousApplied }
@Serializable data class TargetSource(
    val kind: TargetSourceKind,
    val value: Float? = null,
    val metric: BrewMetric? = null,
    val capture: String? = null,
)
@Serializable enum class CaptureFilter { Latest, Mean }
@Serializable data class CaptureAction(
    val name: String,
    val metric: BrewMetric,
    val windowMillis: Long = 500,
    val maximumAgeMillis: Long = 500,
    val filter: CaptureFilter = CaptureFilter.Mean,
    val minimum: Float = 0f,
    val maximum: Float = 12f,
    val fallback: Float? = null,
)
/** Reactive app limit; physical overshoot is possible. Live use requires a verified device strategy. */
@Serializable data class SecondaryLimit(
    val metric: BrewMetric,
    val ceiling: Float,
    val hysteresis: Float = 0.2f,
    val reductionPerSecond: Float = 1f,
    val recoveryPerSecond: Float = 0.25f,
    val overshootAllowance: Float = 1f,
    val tripDurationMillis: Long = 1000,
    /** Optional lower edge. Inside the band hold the adjusted target; below it recover toward the requested target. */
    val minimum: Float? = null,
)
@Serializable data class ExecutionPolicy(val maximumTotalDurationMillis: Long = 40000)
@Serializable enum class ReplayPolicy { Legacy, Hold, Linear }
@Serializable data class BrewSettings(val doseG: Float? = null, val ratio: Float? = null, val targetYieldG: Float? = null) {
    val effectiveYieldG: Float? get() = targetYieldG ?: doseG?.let { dose -> ratio?.let { dose * it } }
}
@Serializable data class RecipeSource(
    val format: String,
    val title: String,
    val author: String = "",
    val url: String = "",
    val sha256: String,
    val original: JsonObject,
)
@Serializable data class RecipeDefinition(
    val templateId: String,
    val source: RecipeSource,
    val templatePhases: List<BrewPhase>,
    val settings: BrewSettings = BrewSettings(),
    val diagnostics: List<String> = emptyList(),
    val adaptation: String? = null,
    val unresolvedSemantics: List<String> = emptyList(),
)

fun TargetSource.validateSource(available: Set<String>) {
    when (kind) {
        TargetSourceKind.Constant -> require(value != null && value.isFinite() && value >= 0f)
        TargetSourceKind.MeasurementAtEntry -> require(metric != null && metric != BrewMetric.PhaseTime)
        TargetSourceKind.CapturedValue -> require(capture in available) { "Capture must be defined before use: $capture" }
        TargetSourceKind.PreviousGoal, TargetSourceKind.PreviousApplied -> Unit
    }
}

/** Metadata edits in the simpler tablet editor must not reconstruct and downgrade v2 programs. */
fun BrewProgram.withFinishConfiguration(condition: Condition?): BrewProgram = when (this) {
    is BrewProgram.Phases -> copy(
        recipe = recipe?.copy(
            settings = when (condition) {
                is Condition.Weight -> recipe.settings.copy(targetYieldG = condition.target)
                else -> recipe.settings.copy(targetYieldG = null)
            },
        ),
    )
    is BrewProgram.Recording -> this
}
