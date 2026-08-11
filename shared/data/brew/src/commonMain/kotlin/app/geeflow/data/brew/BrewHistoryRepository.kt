package app.geeflow.data.brew

import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewHistoryEntry

interface BrewHistoryRepository {
    /** Stores [entry] with its [dataPoints] and drops the user's oldest brews beyond the retention limit. */
    fun addBrew(entry: BrewHistoryEntry, dataPoints: Map<Float, BrewDataPoint>)

    /**
     * One page of recorded brews, newest first. [query] matches the profile name and the local
     * date/time of the brew; an empty query matches everything.
     */
    fun getBrews(userId: Long, query: String, limit: Int, offset: Int): List<BrewHistoryEntry>

    fun getBrewDataPoints(id: Long): Map<Float, BrewDataPoint>

    fun deleteBrew(id: Long)
}
