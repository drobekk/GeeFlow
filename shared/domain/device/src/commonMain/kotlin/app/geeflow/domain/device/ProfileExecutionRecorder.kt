package app.geeflow.domain.device

import app.geeflow.data.brew.BrewHistoryRepository
import app.geeflow.data.brew.model.BrewHistoryEntry
import app.geeflow.data.brew.model.BrewMode
import app.geeflow.data.brew.model.ProfileExecutionTrace
import org.koin.core.annotation.Single

/** Persists app-driven shots even when their original screen has been removed from navigation. */
@Single
class ProfileExecutionRecorder(private val history: BrewHistoryRepository) {
    fun save(trace: ProfileExecutionTrace) {
        val startedAt = trace.startedAt ?: return
        if (trace.measurements.isEmpty()) return
        history.addBrew(
            BrewHistoryEntry(
                userId = trace.profile.userId,
                profileId = trace.profile.id.takeIf { it != 0L },
                profileName = trace.profile.name,
                mode = BrewMode.Profile,
                startedAt = startedAt,
                durationSeconds = (trace.measurements.last().elapsedMillis / MILLISECONDS_PER_SECOND).toInt(),
                executionTrace = trace.copy(measurements = emptyList()),
            ),
            trace.measurements.associate { it.elapsedMillis.toFloat() / MILLISECONDS_PER_SECOND to it.data },
        )
    }
}

private const val MILLISECONDS_PER_SECOND = 1000L
