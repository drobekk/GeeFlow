package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewHistoryRepository
import app.geeflow.data.brew.model.BrewShotMetadata
import org.koin.core.annotation.Factory
import kotlin.time.Clock
import kotlin.time.Instant

@Factory
class SaveBrewShotMetadataUseCase(private val history: BrewHistoryRepository) {
    fun updateGrindSetting(deviceId: Long, startedAt: Instant, grindSetting: String?) {
        history.updateShotGrindSetting(
            deviceId = deviceId,
            startedAt = startedAt,
            grindSetting = grindSetting,
            updatedAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
        )
    }

    fun updateFinalWeight(deviceId: Long, startedAt: Instant, finalWeightGrams: Float) {
        history.updateShotFinalWeight(
            deviceId = deviceId,
            startedAt = startedAt,
            finalWeightGrams = finalWeightGrams,
            updatedAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
        )
    }

    operator fun invoke(
        deviceId: Long,
        startedAt: Instant,
        doseGrams: Float?,
        grindSetting: String?,
        endedAt: Instant? = null,
        extractionStartMillis: Long? = null,
        extractionStartEstimated: Boolean? = null,
        finalWeightGrams: Float? = null,
        recipeRatio: Float? = null,
        recipeTargetGrams: Float? = null,
    ) {
        history.saveShotMetadata(
            BrewShotMetadata(
                deviceId = deviceId,
                startedAt = startedAt,
                doseGrams = doseGrams,
                grindSetting = grindSetting,
                endedAt = endedAt,
                extractionStartMillis = extractionStartMillis,
                extractionStartEstimated = extractionStartEstimated,
                finalWeightGrams = finalWeightGrams,
                recipeRatio = recipeRatio,
                recipeTargetGrams = recipeTargetGrams,
                updatedAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }
}
