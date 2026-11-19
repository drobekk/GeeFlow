package app.geeflow.data.brew

import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewHistoryEntry
import app.geeflow.data.brew.model.BrewScaleSample
import app.geeflow.data.brew.model.BrewShotMetadata

interface BrewHistoryRepository {
    /** Stores [entry] with its [dataPoints] and drops the user's oldest brews beyond the retention limit. */
    fun addBrew(entry: BrewHistoryEntry, dataPoints: Map<Float, BrewDataPoint>)

    /**
     * One page of recorded brews, newest first. [query] matches the profile name and the local
     * date/time of the brew; an empty query matches everything.
     */
    fun getBrews(userId: Long, query: String, limit: Int, offset: Int): List<BrewHistoryEntry>

    /** Device-specific history; legacy rows without a device are intentionally excluded. */
    fun getBrewsForDevice(userId: Long, deviceId: Long, query: String, limit: Int, offset: Int): List<BrewHistoryEntry> =
        getBrews(userId, query, limit, offset).filter { it.deviceId == deviceId }

    fun getBrewDataPoints(id: Long): Map<Float, BrewDataPoint>

    fun getBrewScaleSamples(id: Long): List<BrewScaleSample> = emptyList()

    /** Stores metadata before, during, or after the history row is inserted. */
    fun saveShotMetadata(metadata: BrewShotMetadata) = Unit

    /** Sets Grind explicitly, including clearing a previously saved value. */
    fun updateShotGrindSetting(
        deviceId: Long,
        startedAt: kotlin.time.Instant,
        grindSetting: String?,
        updatedAtEpochMillis: Long,
    ) = Unit

    /** Updates only the final scale value, preserving concurrent edits to the other shot metadata. */
    fun updateShotFinalWeight(deviceId: Long, startedAt: kotlin.time.Instant, finalWeightGrams: Float, updatedAtEpochMillis: Long) = Unit

    fun deleteBrew(id: Long)
}
