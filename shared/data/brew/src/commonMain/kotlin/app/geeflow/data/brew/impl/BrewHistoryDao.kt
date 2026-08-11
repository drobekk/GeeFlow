package app.geeflow.data.brew.impl

import app.geeflow.data.brew.db.AppDatabase
import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewHistoryEntry
import app.geeflow.data.brew.model.BrewMode
import app.geeflow.data.brew.model.ProfileStep
import org.koin.core.annotation.Singleton
import kotlin.time.Instant

@Singleton
class BrewHistoryDao(database: AppDatabase) {
    private val dbQuery = database.brewHistoryQueries

    fun getPage(userId: Long, pattern: String, limit: Long, offset: Long): List<BrewHistoryEntry> =
        dbQuery.selectPage(userId, pattern, limit, offset, ::mapToEntry).executeAsList()

    fun getDataPoints(id: Long): Map<Float, BrewDataPoint> =
        dbQuery.selectDataPoints(id).executeAsOneOrNull().orEmpty()

    fun insertBrew(entry: BrewHistoryEntry, dataPoints: Map<Float, BrewDataPoint>) {
        dbQuery.insertBrew(
            userId = entry.userId,
            profileId = entry.profileId,
            profileName = entry.profileName,
            mode = entry.mode,
            startedAt = entry.startedAt.toEpochMilliseconds(),
            durationSeconds = entry.durationSeconds.toLong(),
            profileSteps = entry.profileSteps,
            dataPoints = dataPoints,
        )
    }

    fun trimHistory(userId: Long, keep: Long) {
        dbQuery.trimHistory(userId, keep)
    }

    fun deleteBrew(id: Long) {
        dbQuery.deleteById(id)
    }

    @Suppress("LongParameterList")
    private fun mapToEntry(
        id: Long,
        userId: Long,
        profileId: Long?,
        profileName: String?,
        mode: BrewMode,
        startedAt: Long,
        durationSeconds: Long,
        profileSteps: List<ProfileStep>,
    ): BrewHistoryEntry = BrewHistoryEntry(
        id = id,
        userId = userId,
        profileId = profileId,
        profileName = profileName,
        mode = mode,
        startedAt = Instant.fromEpochMilliseconds(startedAt),
        durationSeconds = durationSeconds.toInt(),
        profileSteps = profileSteps,
    )
}
