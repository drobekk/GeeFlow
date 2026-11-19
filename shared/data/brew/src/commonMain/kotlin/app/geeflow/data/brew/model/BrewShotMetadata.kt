package app.geeflow.data.brew.model

import kotlin.time.Instant

/** Shot annotations keyed by device and start time so edits survive asynchronous history saves. */
data class BrewShotMetadata(
    val deviceId: Long,
    val startedAt: Instant,
    val doseGrams: Float? = null,
    val grindSetting: String? = null,
    val endedAt: Instant? = null,
    val extractionStartMillis: Long? = null,
    val extractionStartEstimated: Boolean? = null,
    val finalWeightGrams: Float? = null,
    /** Values shown in the recipe setup when this shot was started, separate from stop conditions. */
    val recipeRatio: Float? = null,
    val recipeTargetGrams: Float? = null,
    val updatedAtEpochMillis: Long,
)
