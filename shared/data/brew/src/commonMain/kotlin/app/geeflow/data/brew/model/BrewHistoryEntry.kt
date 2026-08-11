package app.geeflow.data.brew.model

import kotlin.time.Instant

/**
 * A recorded brew without its samples — `BrewHistoryRepository.getBrewDataPoints` loads those
 * separately so browsing the history does not read every stored curve.
 */
data class BrewHistoryEntry(
    val id: Long = 0,
    val userId: Long,
    val profileId: Long? = null,
    val profileName: String? = null,
    val mode: BrewMode,
    val startedAt: Instant,
    val durationSeconds: Int,
    /** The profile's steps as they were when this brew ran; empty for manual and freehand brews. */
    val profileSteps: List<ProfileStep> = emptyList(),
)
