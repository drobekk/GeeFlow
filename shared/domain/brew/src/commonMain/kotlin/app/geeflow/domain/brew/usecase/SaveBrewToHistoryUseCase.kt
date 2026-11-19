package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewHistoryRepository
import app.geeflow.data.brew.model.BrewHistoryEntry
import app.geeflow.data.brew.model.BrewMode
import app.geeflow.data.brew.model.BrewSession
import app.geeflow.data.brew.model.FreeHandRecording
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.device.DeviceBrewingSettingsRepository
import app.geeflow.data.user.UserRepository
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory
import kotlin.time.Duration.Companion.milliseconds

/**
 * Records a finished [BrewSession]. Paddle long press brews are dropped when the user asked for
 * them to be skipped, which is the default - those are usually flushes rather than shots worth keeping.
 */
@Factory
class SaveBrewToHistoryUseCase(
    private val brewHistoryRepository: BrewHistoryRepository,
    private val deviceBrewingSettingsRepository: DeviceBrewingSettingsRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(
        deviceId: Long,
        session: BrewSession,
        profileId: Long?,
        profileName: String?,
        profileSteps: List<ProfileStep> = emptyList(),
        profileRecording: FreeHandRecording? = null,
        profileProgram: BrewProgram? = null,
        doseGrams: Float? = null,
        grindSetting: String? = null,
        extractionStartMillis: Long? = null,
        extractionStartEstimated: Boolean? = null,
    ) {
        // The execution coordinator records app-controlled profiles independently of navigation.
        if (session.executionTrace != null || session.dataPoints.isEmpty()) return
        val startedAt = session.startTime ?: return
        val userId = userRepository.selectedUser.first()?.id ?: return
        if (session.mode == BrewMode.Manual &&
            deviceBrewingSettingsRepository.observe(deviceId).first().treatManualAsFlush
        ) {
            return
        }

        brewHistoryRepository.addBrew(
            entry = BrewHistoryEntry(
                userId = userId,
                deviceId = deviceId,
                profileId = session.executionTrace?.profile?.id?.takeIf { it != 0L } ?: profileId,
                profileName = session.executionTrace?.profile?.name ?: profileName,
                executionTrace = session.executionTrace,
                mode = session.mode,
                startedAt = startedAt,
                durationSeconds = session.elapsedSeconds,
                profileSteps = profileSteps,
                profileRecording = profileRecording,
                scaleSamples = session.scaleSamples,
                profileProgram = profileProgram ?: session.executionTrace?.profile?.program,
                doseGrams = doseGrams,
                grindSetting = grindSetting,
                endedAt = session.endedAt ?: session.dataPoints.keys.maxOrNull()?.let { startedAt + (it * 1000).toLong().milliseconds },
                extractionStartMillis = extractionStartMillis,
                extractionStartEstimated = extractionStartEstimated,
                finalWeightGrams = session.scaleSamples.lastOrNull { it.decoded }?.weightGrams
                    ?: session.dataPoints.maxByOrNull { it.key }?.value?.weight?.takeIf { it > 0f },
            ),
            dataPoints = session.dataPoints,
        )
    }
}
