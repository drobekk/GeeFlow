package app.geeflow.data.brew.impl

import app.geeflow.data.brew.BrewHistoryRepository
import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewHistoryEntry
import org.koin.core.annotation.Singleton

@Singleton
class BrewHistoryRepositoryImpl(
    private val brewHistoryDao: BrewHistoryDao,
) : BrewHistoryRepository {

    override fun addBrew(entry: BrewHistoryEntry, dataPoints: Map<Float, BrewDataPoint>) {
        brewHistoryDao.insertBrew(entry, dataPoints)
        brewHistoryDao.trimHistory(entry.userId, MAX_STORED_BREWS)
    }

    override fun getBrews(userId: Long, query: String, limit: Int, offset: Int): List<BrewHistoryEntry> =
        brewHistoryDao.getPage(
            userId = userId,
            pattern = "%${query.escapeLikeWildcards()}%",
            limit = limit.toLong(),
            offset = offset.toLong(),
        )

    override fun getBrewDataPoints(id: Long): Map<Float, BrewDataPoint> = brewHistoryDao.getDataPoints(id)

    override fun deleteBrew(id: Long) {
        brewHistoryDao.deleteBrew(id)
    }

    private fun String.escapeLikeWildcards() = replace("%", "").replace("_", "")
}

private const val MAX_STORED_BREWS = 200L
