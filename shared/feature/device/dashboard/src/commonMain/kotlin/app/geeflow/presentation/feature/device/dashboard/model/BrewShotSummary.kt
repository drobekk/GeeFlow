package app.geeflow.presentation.feature.device.dashboard.model

import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.BrewScaleSample
import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.PhaseTransition
import app.geeflow.data.brew.model.plannedDurationMillis
import kotlin.math.roundToInt

internal data class BrewShotSummary(
    val splitEstimated: Boolean,
    val preinfusionTimeSeconds: Double?,
    val extractionTimeSeconds: Double?,
    val totalTimeSeconds: Double,
    val preinfusionWeightGrams: Float?,
    val extractionWeightGrams: Float?,
    val totalWeightGrams: Float?,
    val preinfusionVolumeMl: Float?,
    val extractionVolumeMl: Float?,
    val totalVolumeMl: Float?,
    val preinfusionTemperature: String?,
    val extractionTemperature: String?,
    val totalTemperature: String?,
    val preinfusionFlow: String?,
    val extractionFlow: String?,
    val totalFlow: String?,
    val preinfusionPressure: String?,
    val extractionPressure: String?,
    val totalPressure: String?,
    val volumeEstimated: Boolean,
)

internal fun brewShotSummary(
    measurements: Map<Float, ChartData>,
    scaleSamples: List<BrewScaleSample>,
    profileProgram: BrewProgram?,
    phaseTransitions: List<PhaseTransition>,
    recordedExtractionStartMillis: Long?,
    extractionStartEstimated: Boolean?,
    recordedEndSeconds: Double?,
    finalWeightGrams: Float?,
    shotInProgress: Boolean = false,
): BrewShotSummary {
    val points = measurements.entries
        .filter { it.key.isFinite() && it.key >= 0f }
        .sortedBy { it.key }
    val decodedScale = scaleSamples.filter { it.decoded && it.weightGrams != null }.sortedBy { it.elapsedMillis }
    val endSeconds = recordedEndSeconds?.takeIf { it.isFinite() && it > 0.0 }
        ?: maxOf(points.lastOrNull()?.key?.toDouble() ?: 0.0, decodedScale.lastOrNull()?.elapsedMillis?.div(1000.0) ?: 0.0)
    val hasProfileMarker = hasExtractionMarker(profileProgram)
    val phaseMarker = profileExtractionStart(profileProgram, phaseTransitions, endSeconds, shotInProgress)
    val recordedSplit = recordedExtractionStartMillis?.div(1000.0)
    val split = when {
        recordedSplit != null -> recordedSplit.coerceIn(0.0, endSeconds)
        phaseMarker != null -> phaseMarker.coerceIn(0.0, endSeconds)
        hasProfileMarker -> null
        else -> estimateExtractionStart(decodedScale.map { it.elapsedMillis / 1000.0 to it.weightGrams!! })
            ?: estimateExtractionStart(points.map { it.key.toDouble() to it.value.weight })
    }
    val splitEstimated = when {
        recordedSplit != null -> extractionStartEstimated == true
        phaseMarker != null -> false
        split != null -> true
        else -> false
    }
    val hasWeightReadings = decodedScale.isNotEmpty() || points.any { it.value.weight > 0f }
    val totalWeight = finalWeightGrams?.takeIf { it > 0f || decodedScale.isNotEmpty() } ?: decodedScale.lastOrNull()?.weightGrams
        ?: points.lastOrNull()?.value?.weight?.takeIf { hasWeightReadings }
    val splitWeight = split?.let { scaleWeightAt(decodedScale, it) ?: dataWeightAt(points, it)?.takeIf { hasWeightReadings } }
    val totalVolumeMeasured = points.any { it.value.volume > 0f }
    val hasPumpFlowReadings = points.count { it.value.volumePerSecond > 0f } >= 2
    val totalVolume = when {
        totalVolumeMeasured -> volumeAt(points, endSeconds, measured = true)
        hasPumpFlowReadings -> volumeAt(points, endSeconds, measured = false)
        else -> null
    }
    val splitVolume = split?.let { volumeAt(points, it, measured = totalVolumeMeasured) }
    val prePoints = split?.let { value -> points.filter { it.key.toDouble() < value } }.orEmpty()
    val extractionPoints = split?.let { value -> points.filter { it.key.toDouble() >= value && it.key.toDouble() <= endSeconds } }.orEmpty()
    val splitWeightValue = splitWeight
    return BrewShotSummary(
        splitEstimated = splitEstimated,
        preinfusionTimeSeconds = split,
        extractionTimeSeconds = split?.let { (endSeconds - it).coerceAtLeast(0.0) },
        totalTimeSeconds = endSeconds,
        preinfusionWeightGrams = splitWeightValue,
        extractionWeightGrams = if (splitWeightValue != null && totalWeight != null) totalWeight - splitWeightValue else null,
        totalWeightGrams = totalWeight,
        preinfusionVolumeMl = splitVolume,
        extractionVolumeMl = if (splitVolume != null && totalVolume != null) totalVolume - splitVolume else null,
        totalVolumeMl = totalVolume,
        preinfusionTemperature = temperatureSnapshot(prePoints),
        extractionTemperature = temperatureSnapshot(extractionPoints),
        totalTemperature = temperatureSnapshot(points),
        preinfusionFlow = sequenceSnapshot(prePoints.map { it.value.volumePerSecond }),
        extractionFlow = sequenceSnapshot(extractionPoints.map { it.value.volumePerSecond }),
        totalFlow = sequenceSnapshot(points.map { it.value.volumePerSecond }),
        preinfusionPressure = sequenceSnapshot(prePoints.map { it.value.pressure }),
        extractionPressure = sequenceSnapshot(extractionPoints.map { it.value.pressure }),
        totalPressure = sequenceSnapshot(points.map { it.value.pressure }),
        volumeEstimated = !totalVolumeMeasured && totalVolume != null,
    )
}

private fun profileExtractionStart(
    program: BrewProgram?,
    transitions: List<PhaseTransition>,
    endSeconds: Double,
    shotInProgress: Boolean,
): Double? {
    val phases = (program as? BrewProgram.Phases)?.phases ?: return null
    val extractionIndex = phases.indexOfFirst { phase ->
        phase.name.trim().equals("Extraction", ignoreCase = true) ||
            phase.name.trim().equals("Extraction start", ignoreCase = true)
    }
    if (extractionIndex < 0) return null
    if (extractionIndex == 0) return 0.0
    val previous = phases[extractionIndex - 1]
    transitions.firstOrNull { it.phaseId == previous.id }?.let { return it.elapsedMillis / 1000.0 }
    val fixedBefore = phases.take(extractionIndex).all { phase ->
        phase.exitConditions.all { it.metric == BrewMetric.PhaseTime }
    }
    if (fixedBefore) {
        val plannedStart = phases.take(extractionIndex).sumOf { it.plannedDurationMillis() } / 1000.0
        return plannedStart.takeIf { it <= endSeconds || !shotInProgress }
    }
    // No transition into the named extraction phase means the shot ended while still preinfusing.
    return endSeconds.takeUnless { shotInProgress }
}

private fun hasExtractionMarker(program: BrewProgram?): Boolean =
    ((program as? BrewProgram.Phases)?.phases).orEmpty().any { phase ->
        phase.name.trim().equals("Extraction", ignoreCase = true) ||
            phase.name.trim().equals("Extraction start", ignoreCase = true)
    }

private fun estimateExtractionStart(samples: List<Pair<Double, Float>>): Double? {
    if (samples.size < 3) return null
    val baseline = samples.first().second
    var startIndex: Int? = null
    for (index in samples.indices) {
        if (samples[index].second >= baseline + MinimumCupWeightRiseGrams) {
            if (startIndex == null) startIndex = index
        } else {
            startIndex = null
        }
        val first = startIndex ?: continue
        val candidate = samples[first].first
        if (index - first + 1 >= MinimumSustainedReadings && samples[index].first - candidate >= MinimumSustainedDurationSeconds) {
            return candidate
        }
    }
    return null
}

private fun scaleWeightAt(samples: List<BrewScaleSample>, seconds: Double): Float? {
    val at = (seconds * 1000).toLong()
    return samples.lastOrNull { it.elapsedMillis <= at }?.weightGrams
}

private fun dataWeightAt(points: List<Map.Entry<Float, ChartData>>, seconds: Double): Float? =
    points.lastOrNull { it.key <= seconds.toFloat() }?.value?.weight

private fun volumeAt(points: List<Map.Entry<Float, ChartData>>, seconds: Double, measured: Boolean): Float? {
    if (points.isEmpty()) return null
    if (measured) {
        val before = points.lastOrNull { it.key <= seconds.toFloat() } ?: points.first()
        val after = points.firstOrNull { it.key >= seconds.toFloat() } ?: points.last()
        if (before.key == after.key) return before.value.volume
        val fraction = (seconds - before.key) / (after.key - before.key)
        return before.value.volume + (after.value.volume - before.value.volume) * fraction.toFloat()
    }
    if (points.size < 2) return null
    var total = 0.0
    for (index in 1 until points.size) {
        val previous = points[index - 1]
        val current = points[index]
        val start = previous.key.toDouble()
        val end = minOf(current.key.toDouble(), seconds)
        if (end <= start) continue
        val duration = end - start
        val endFraction = (end - start) / (current.key - previous.key)
        val endFlow = previous.value.volumePerSecond + (current.value.volumePerSecond - previous.value.volumePerSecond) * endFraction.toFloat()
        total += (previous.value.volumePerSecond + endFlow) * 0.5 * duration
        if (current.key >= seconds) break
    }
    return total.toFloat()
}

private fun temperatureSnapshot(points: List<Map.Entry<Float, ChartData>>): String? {
    val values = points.mapNotNull { it.value.temperatureC }
    if (values.isEmpty()) return null
    val minimum = values.min().roundToInt()
    val maximum = values.max().roundToInt()
    return if (minimum == maximum) "$minimum" else "$minimum→$maximum"
}

private fun sequenceSnapshot(values: List<Float>): String? {
    if (values.isEmpty() || values.all { it == 0f }) return null
    val start = values.first().roundToOneDecimal()
    val peak = values.max().roundToOneDecimal()
    val end = values.last().roundToOneDecimal()
    return when {
        start == end -> start
        peak == start || peak == end -> "$start→$end"
        else -> "$start→$peak→$end"
    }
}

private fun Float.roundToOneDecimal(): String = ((this * 10f).roundToInt() / 10f).let { value ->
    if (value == 0f) "0.0" else value.toString()
}

private const val MinimumCupWeightRiseGrams = 0.5f
private const val MinimumSustainedDurationSeconds = 0.5
private const val MinimumSustainedReadings = 3
