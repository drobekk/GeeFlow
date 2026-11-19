package app.geeflow.domain.device

import app.geeflow.data.brew.BrewHistoryRepository
import app.geeflow.data.brew.model.BrewHistoryEntry
import app.geeflow.data.brew.model.BrewMode
import app.geeflow.data.brew.model.ProfileExecutionTrace
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.plannedDurationMillis
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.milliseconds

/** Persists app-driven shots even when their original screen has been removed from navigation. */
@Single
class ProfileExecutionRecorder(private val history: BrewHistoryRepository) {
    fun save(deviceId: Long, trace: ProfileExecutionTrace) {
        val startedAt = trace.startedAt ?: return
        if (trace.measurements.isEmpty() && trace.scaleSamples.isEmpty() && trace.machineDiagnostics.isEmpty()) return
        val boundary = trace.extractionStartMillis?.let { it to (trace.extractionStartEstimated == true) }
            ?: extractionStart(trace)
        history.addBrew(
            BrewHistoryEntry(
                userId = trace.profile.userId,
                deviceId = deviceId,
                profileId = trace.profile.id.takeIf { it != 0L },
                profileName = trace.profile.name,
                mode = BrewMode.Profile,
                startedAt = startedAt,
                durationSeconds = (
                    maxOf(
                        trace.measurements.lastOrNull()?.elapsedMillis ?: 0L,
                        trace.scaleSamples.lastOrNull()?.elapsedMillis ?: 0L,
                    ) / MILLISECONDS_PER_SECOND
                    ).toInt(),
                executionTrace = trace.copy(measurements = emptyList()),
                scaleSamples = trace.scaleSamples,
                profileProgram = trace.profile.program,
                doseGrams = trace.doseGrams
                    ?: (trace.profile.program as? app.geeflow.data.brew.model.BrewProgram.Phases)
                        ?.recipe?.settings?.doseG,
                grindSetting = trace.grindSetting,
                endedAt = trace.stopConfirmedAt ?: (trace.measurements.lastOrNull()?.elapsedMillis?.let {
                    startedAt + it.milliseconds
                }),
                extractionStartMillis = boundary?.first,
                extractionStartEstimated = boundary?.second,
                finalWeightGrams = trace.finalScaleWeightGrams
                    ?: trace.scaleSamples.lastOrNull { it.decoded }?.weightGrams,
            ),
            trace.measurements.associate { it.elapsedMillis.toFloat() / MILLISECONDS_PER_SECOND to it.data },
        )
    }
}

private fun extractionStart(trace: ProfileExecutionTrace): Pair<Long, Boolean>? {
    val phases = (trace.profile.program as? BrewProgram.Phases)?.phases ?: return null
    val index = phases.indexOfFirst {
        it.name.trim().equals("Extraction", ignoreCase = true) ||
            it.name.trim().equals("Extraction start", ignoreCase = true)
    }
    if (index < 0) return null
    if (index == 0) return 0L to false
    val phase = phases[index]
    trace.targets.firstOrNull { it.phaseId == phase.id }?.let { return it.elapsedMillis to false }
    val previous = phases[index - 1]
    trace.transitions.firstOrNull { it.phaseId == previous.id }?.let { return it.elapsedMillis to false }
    val endedAt = trace.stopConfirmedAt ?: trace.measurements.lastOrNull()?.let { trace.startedAt!! + it.elapsedMillis.milliseconds }
    val endMillis = endedAt?.let { end -> trace.startedAt?.let { (end - it).inWholeMilliseconds } }
    if (phases.take(index).all { prior -> prior.exitConditions.all { it.metric == BrewMetric.PhaseTime } }) {
        val plannedMillis = phases.take(index).sumOf { it.plannedDurationMillis() }
        return minOf(plannedMillis, endMillis ?: plannedMillis) to false
    }
    if (endMillis != null) return endMillis.coerceAtLeast(0) to false
    val baseline = trace.scaleSamples.firstOrNull { it.decoded }?.weightGrams ?: return null
    val candidates = trace.scaleSamples.filter { it.decoded && it.weightGrams != null }.sortedBy { it.elapsedMillis }
    var first: Int? = null
    for (sampleIndex in candidates.indices) {
        if (candidates[sampleIndex].weightGrams!! >= baseline + 0.5f) {
            if (first == null) first = sampleIndex
        } else first = null
        val initialIndex = first ?: continue
        if (sampleIndex - initialIndex >= 2 && candidates[sampleIndex].elapsedMillis - candidates[initialIndex].elapsedMillis >= 500) {
            return candidates[initialIndex].elapsedMillis to true
        }
    }
    return null
}

private const val MILLISECONDS_PER_SECOND = 1000L
