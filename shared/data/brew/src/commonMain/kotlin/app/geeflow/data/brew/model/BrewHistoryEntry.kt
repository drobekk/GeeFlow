package app.geeflow.data.brew.model

import kotlin.time.Instant

/**
 * A recorded brew without its samples — `BrewHistoryRepository.getBrewDataPoints` loads those
 * separately so browsing the history does not read every stored curve.
 */
data class BrewHistoryEntry(
    val id: Long = 0,
    val userId: Long,
    /** Machine that produced the brew; null only for records created before device scoping. */
    val deviceId: Long? = null,
    val profileId: Long? = null,
    val profileName: String? = null,
    val mode: BrewMode,
    val startedAt: Instant,
    val durationSeconds: Int,
    /** The profile's steps as they were when this brew ran; empty for manual and freehand brews. */
    val profileSteps: List<ProfileStep> = emptyList(),
    val executionTrace: ProfileExecutionTrace? = null,
    val profileRecording: FreeHandRecording? = null,
    val scaleSamples: List<BrewScaleSample> = emptyList(),
    val profileProgram: BrewProgram? = executionTrace?.profile?.program,
    val doseGrams: Float? = null,
    val grindSetting: String? = null,
    val endedAt: Instant? = null,
    val extractionStartMillis: Long? = null,
    val extractionStartEstimated: Boolean? = null,
    val finalWeightGrams: Float? = null,
    val recipeRatio: Float? = null,
    val recipeTargetGrams: Float? = null,
)
