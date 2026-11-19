package app.geeflow.data.brew.impl

import app.geeflow.data.brew.db.AppDatabase
import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewHistoryEntry
import app.geeflow.data.brew.model.BrewMode
import app.geeflow.data.brew.model.FreeHandRecording
import app.geeflow.data.brew.model.ProfileExecutionTrace
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.data.brew.model.BrewScaleSample
import app.geeflow.data.brew.model.BrewShotMetadata
import org.koin.core.annotation.Singleton
import kotlin.time.Instant

@Singleton
class BrewHistoryDao(private val database: AppDatabase) {
    private val dbQuery = database.brewHistoryQueries

    fun getPage(userId: Long, pattern: String, limit: Long, offset: Long): List<BrewHistoryEntry> =
        dbQuery.selectPage(userId, pattern, limit, offset, ::mapToEntry).executeAsList()

    fun getPageForDevice(userId: Long, deviceId: Long, pattern: String, limit: Long, offset: Long): List<BrewHistoryEntry> =
        dbQuery.selectPageForDevice(userId, deviceId, pattern, limit, offset, ::mapToEntry).executeAsList()

    fun getDataPoints(id: Long): Map<Float, BrewDataPoint> =
        dbQuery.selectDataPoints(id).executeAsOneOrNull().orEmpty()

    fun getScaleSamples(id: Long): List<BrewScaleSample> =
        dbQuery.selectScaleSamples(id).executeAsOneOrNull().orEmpty()

    fun insertBrew(entry: BrewHistoryEntry, dataPoints: Map<Float, BrewDataPoint>) {
        database.transaction {
            dbQuery.insertBrew(
                userId = entry.userId,
                deviceId = entry.deviceId,
                profileId = entry.profileId,
                profileName = entry.profileName,
                mode = entry.mode,
                startedAt = entry.startedAt.toEpochMilliseconds(),
                durationSeconds = entry.durationSeconds.toLong(),
                profileSteps = entry.profileSteps,
                profileRecording = entry.profileRecording,
                executionTrace = entry.executionTrace,
                scaleSamples = entry.scaleSamples,
                profileProgram = entry.profileProgram,
                dataPoints = dataPoints,
            )
            entry.deviceId?.let {
                mergeShotMetadata(
                    BrewShotMetadata(
                        deviceId = it,
                        startedAt = entry.startedAt,
                        doseGrams = entry.doseGrams,
                        grindSetting = entry.grindSetting,
                        endedAt = entry.endedAt,
                        extractionStartMillis = entry.extractionStartMillis,
                        extractionStartEstimated = entry.extractionStartEstimated,
                        finalWeightGrams = entry.finalWeightGrams,
                        recipeRatio = entry.recipeRatio,
                        recipeTargetGrams = entry.recipeTargetGrams,
                        updatedAtEpochMillis = entry.endedAt?.toEpochMilliseconds() ?: entry.startedAt.toEpochMilliseconds(),
                    ),
                )
            }
        }
    }

    fun saveShotMetadata(metadata: BrewShotMetadata) = database.transaction {
        val existing = findShotMetadata(metadata.deviceId, metadata.startedAt)
        writeShotMetadata(metadata.copy(
            // Grind has its own explicit update path, including clearing a value. A late
            // telemetry snapshot must not undo a history edit made while it was saving.
            grindSetting = if (existing != null) existing.grindSetting else metadata.grindSetting,
            recipeRatio = existing?.recipeRatio ?: metadata.recipeRatio,
            recipeTargetGrams = existing?.recipeTargetGrams ?: metadata.recipeTargetGrams,
        ))
    }

    fun updateShotGrindSetting(
        deviceId: Long,
        startedAt: Instant,
        grindSetting: String?,
        updatedAtEpochMillis: Long,
    ) = database.transaction {
        val existing = findShotMetadata(deviceId, startedAt)
        writeShotMetadata(
            existing?.copy(
                grindSetting = grindSetting,
                updatedAtEpochMillis = maxOf(existing.updatedAtEpochMillis, updatedAtEpochMillis),
            ) ?: BrewShotMetadata(
                deviceId = deviceId,
                startedAt = startedAt,
                grindSetting = grindSetting,
                updatedAtEpochMillis = updatedAtEpochMillis,
            ),
        )
    }

    fun updateShotFinalWeight(deviceId: Long, startedAt: Instant, finalWeightGrams: Float, updatedAtEpochMillis: Long) = database.transaction {
        mergeShotMetadata(
            BrewShotMetadata(
                deviceId = deviceId,
                startedAt = startedAt,
                finalWeightGrams = finalWeightGrams,
                updatedAtEpochMillis = updatedAtEpochMillis,
            ),
        )
    }

    private fun mergeShotMetadata(metadata: BrewShotMetadata) {
        val existing = findShotMetadata(metadata.deviceId, metadata.startedAt)
        val merged = if (existing == null) metadata else if (metadata.updatedAtEpochMillis >= existing.updatedAtEpochMillis) {
            metadata.copy(
                doseGrams = metadata.doseGrams ?: existing.doseGrams,
                grindSetting = metadata.grindSetting ?: existing.grindSetting,
                endedAt = metadata.endedAt ?: existing.endedAt,
                extractionStartMillis = metadata.extractionStartMillis ?: existing.extractionStartMillis,
                extractionStartEstimated = metadata.extractionStartEstimated ?: existing.extractionStartEstimated,
                finalWeightGrams = metadata.finalWeightGrams ?: existing.finalWeightGrams,
                recipeRatio = existing.recipeRatio ?: metadata.recipeRatio,
                recipeTargetGrams = existing.recipeTargetGrams ?: metadata.recipeTargetGrams,
                updatedAtEpochMillis = maxOf(metadata.updatedAtEpochMillis, existing.updatedAtEpochMillis),
            )
        } else {
            existing.copy(
                endedAt = existing.endedAt ?: metadata.endedAt,
                extractionStartMillis = existing.extractionStartMillis ?: metadata.extractionStartMillis,
                extractionStartEstimated = existing.extractionStartEstimated ?: metadata.extractionStartEstimated,
                finalWeightGrams = existing.finalWeightGrams ?: metadata.finalWeightGrams,
                recipeRatio = existing.recipeRatio ?: metadata.recipeRatio,
                recipeTargetGrams = existing.recipeTargetGrams ?: metadata.recipeTargetGrams,
            )
        }
        writeShotMetadata(merged)
    }

    private fun findShotMetadata(deviceId: Long, startedAt: Instant): BrewShotMetadata? =
        dbQuery.selectShotMetadata(deviceId, startedAt.toEpochMilliseconds()) { id, start, dose, grind, ended, split, estimated, finalWeight, ratio, target, updated ->
            BrewShotMetadata(
                deviceId = id,
                startedAt = Instant.fromEpochMilliseconds(start),
                doseGrams = dose?.toFloat(),
                grindSetting = grind,
                endedAt = ended?.let(Instant::fromEpochMilliseconds),
                extractionStartMillis = split,
                extractionStartEstimated = estimated?.let { it != 0L },
                finalWeightGrams = finalWeight?.toFloat(),
                recipeRatio = ratio?.toFloat(),
                recipeTargetGrams = target?.toFloat(),
                updatedAtEpochMillis = updated,
            )
        }.executeAsOneOrNull()

    private fun writeShotMetadata(metadata: BrewShotMetadata) {
        dbQuery.replaceShotMetadata(
            deviceId = metadata.deviceId,
            startedAt = metadata.startedAt.toEpochMilliseconds(),
            doseGrams = metadata.doseGrams?.toDouble(),
            grindSetting = metadata.grindSetting,
            endedAt = metadata.endedAt?.toEpochMilliseconds(),
            extractionStartMillis = metadata.extractionStartMillis,
            extractionStartEstimated = metadata.extractionStartEstimated?.let { if (it) 1L else 0L },
            finalWeightGrams = metadata.finalWeightGrams?.toDouble(),
            recipeRatio = metadata.recipeRatio?.toDouble(),
            recipeTargetGrams = metadata.recipeTargetGrams?.toDouble(),
            updatedAt = metadata.updatedAtEpochMillis,
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
        deviceId: Long?,
        profileId: Long?,
        profileName: String?,
        mode: BrewMode,
        startedAt: Long,
        durationSeconds: Long,
        profileSteps: List<ProfileStep>,
        profileRecording: FreeHandRecording?,
        executionTrace: ProfileExecutionTrace?,
        profileProgram: app.geeflow.data.brew.model.BrewProgram?,
        doseGrams: Double?,
        grindSetting: String?,
        endedAt: Long?,
        extractionStartMillis: Long?,
        extractionStartEstimated: Long?,
        finalWeightGrams: Double?,
        recipeRatio: Double?,
        recipeTargetGrams: Double?,
    ): BrewHistoryEntry = BrewHistoryEntry(
        id = id,
        userId = userId,
        deviceId = deviceId,
        profileId = profileId,
        profileName = profileName,
        mode = mode,
        startedAt = Instant.fromEpochMilliseconds(startedAt),
        durationSeconds = durationSeconds.toInt(),
        profileSteps = profileSteps,
        profileRecording = profileRecording,
        executionTrace = executionTrace,
        profileProgram = profileProgram,
        doseGrams = doseGrams?.toFloat(),
        grindSetting = grindSetting,
        endedAt = endedAt?.let(Instant::fromEpochMilliseconds),
        extractionStartMillis = extractionStartMillis,
        extractionStartEstimated = extractionStartEstimated?.let { it != 0L },
        finalWeightGrams = finalWeightGrams?.toFloat(),
        recipeRatio = recipeRatio?.toFloat(),
        recipeTargetGrams = recipeTargetGrams?.toFloat(),
    )
}
