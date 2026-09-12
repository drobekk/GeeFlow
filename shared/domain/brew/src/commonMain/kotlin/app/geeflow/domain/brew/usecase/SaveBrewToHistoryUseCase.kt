package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewHistoryRepository
import app.geeflow.data.brew.model.BrewHistoryEntry
import app.geeflow.data.brew.model.BrewMode
import app.geeflow.data.brew.model.BrewSession
import app.geeflow.data.brew.model.FreeHandRecording
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

/**
 * Records a finished [BrewSession]. Paddle long press brews are dropped when the user asked for
 * them to be skipped, which is the default - those are usually flushes rather than shots worth keeping.
 */
@Factory
class SaveBrewToHistoryUseCase(
    private val brewHistoryRepository: BrewHistoryRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(
        session: BrewSession,
        profileId: Long?,
        profileName: String?,
        profileSteps: List<ProfileStep> = emptyList(),
        profileRecording: FreeHandRecording? = null,
    ) {
        // The execution coordinator records app-controlled profiles independently of navigation.
        if (session.executionTrace != null) return
        val startedAt = session.startTime ?: return
        if (session.dataPoints.isEmpty()) return
        val userId = userRepository.selectedUser.first()?.id ?: return
        if (session.mode == BrewMode.Manual && userSettingsRepository.skipManualBrewHistory(userId).first()) return

        brewHistoryRepository.addBrew(
            entry = BrewHistoryEntry(
                userId = userId,
                profileId = session.executionTrace?.profile?.id?.takeIf { it != 0L } ?: profileId,
                profileName = session.executionTrace?.profile?.name ?: profileName,
                executionTrace = session.executionTrace,
                mode = session.mode,
                startedAt = startedAt,
                durationSeconds = session.elapsedSeconds,
                profileSteps = profileSteps,
                profileRecording = profileRecording,
            ),
            dataPoints = session.dataPoints,
        )
    }
}
