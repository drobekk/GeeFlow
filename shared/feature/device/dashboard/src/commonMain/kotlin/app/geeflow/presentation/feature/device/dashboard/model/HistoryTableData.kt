package app.geeflow.presentation.feature.device.dashboard.model

import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.BrewScaleSample
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.PhaseTransition
import kotlin.math.abs

internal data class HistoryTableRow(
    val durationSeconds: Double?,
    val weightGrams: Float?,
    val volumeMl: Float?,
    val temperatureC: Float?,
    val flowMlPerSecond: Float?,
    val pressureBar: Float?,
)

internal data class HistoryTableData(
    val preinfusion: HistoryTableRow,
    val extraction: HistoryTableRow,
    val total: HistoryTableRow,
)

/** Summarizes the shot as preinfusion, extraction, and total using recorded samples. */
internal fun historyTableData(
    program: BrewProgram?,
    transitions: List<PhaseTransition>,
    measurements: Map<Float, ChartData>,
    scaleSamples: List<BrewScaleSample>,
    endSeconds: Double?,
    finalWeightGrams: Float?,
    recordedPhaseIds: Set<String> = emptySet(),
): HistoryTableData {
    val points = measurements.entries
        .filter { it.key.isFinite() && it.key >= 0f }
        .sortedBy { it.key }
        .map { (seconds, data) -> (seconds * 1000.0) to data }
    val scales = scaleSamples
        .filter { it.decoded && it.weightGrams?.isFinite() == true }
        .sortedBy { it.elapsedMillis }
    val scaleWeights = scales.mapNotNull { sample -> sample.weightGrams?.let { sample.elapsedMillis.toDouble() to it } }
    val chartWeights = points.map { (time, data) -> time to data.weight }
    val hasWeight = scaleWeights.isNotEmpty() || points.any { it.second.weight > 0f }
    val hasVolume = points.any { it.second.volume > 0f }
    val hasFlow = points.any { it.second.volumePerSecond > 0f }
    val hasPressure = points.any { it.second.pressure > 0f }

    val orderedTransitions = transitions.sortedBy { it.elapsedMillis }
    val phases = (program as? BrewProgram.Phases)?.phases.orEmpty()
    val recordedDurationMillis = endSeconds?.takeIf { it.isFinite() && it >= 0.0 }?.times(1000.0)
    val observedDurationMillis = listOfNotNull(
        points.lastOrNull()?.first,
        scales.lastOrNull()?.elapsedMillis?.toDouble(),
        orderedTransitions.lastOrNull()?.elapsedMillis?.toDouble(),
    ).maxOrNull() ?: 0.0
    val durationMillis = recordedDurationMillis ?: observedDurationMillis
    val durationSeconds = durationMillis / 1000.0

    fun phaseStart(index: Int): Double? = when {
        index <= 0 -> 0.0
        index > phases.lastIndex -> null
        else -> orderedTransitions.firstOrNull { it.phaseId == phases[index - 1].id }
            ?.elapsedMillis?.toDouble()
    }

    fun phaseEnd(index: Int): Double? = phases.getOrNull(index)?.let { phase ->
        orderedTransitions.firstOrNull { it.phaseId == phase.id }?.elapsedMillis?.toDouble()
    }

    fun phaseWasEntered(index: Int): Boolean {
        val phase = phases.getOrNull(index) ?: return false
        return index == 0 || phase.id in recordedPhaseIds || phaseStart(index) != null
    }

    fun isExtractionPhase(name: String): Boolean {
        val normalized = name.lowercase().trim()
        val preinfusionTerms = listOf("preinfusion", "pre-infusion", "pre infusion", "bloom", "soak", "预浸", "浸润")
        if (preinfusionTerms.any(normalized::contains)) return false
        return listOf("extraction", "extract", "pressure", "press", "espresso", "萃取", "压力", "增压", "高压")
            .any(normalized::contains)
    }

    fun pressureCrossing(startMillis: Double, endMillis: Double): Double? {
        val samples = points.filter { (time, _) -> time in startMillis..endMillis }
        val first = samples.firstOrNull() ?: return null
        if (first.second.pressure > ExtractionPressureSplitBar) return first.first
        for ((left, right) in samples.zipWithNext()) {
            val leftPressure = left.second.pressure
            val rightPressure = right.second.pressure
            if (leftPressure <= ExtractionPressureSplitBar && rightPressure > ExtractionPressureSplitBar) {
                val fraction = (ExtractionPressureSplitBar - leftPressure) / (rightPressure - leftPressure)
                return left.first + (right.first - left.first) * fraction
            }
        }
        return null
    }

    val namedExtractionIndex = phases.indices.firstOrNull { index ->
        phaseWasEntered(index) && isExtractionPhase(phases[index].name)
    }
    val highPressureIndex = phases.indices.firstOrNull { index ->
        phaseWasEntered(index) && (phases[index].control as? PhaseControl.Pressure)
            ?.bar?.let { it > ExtractionPressureSplitBar } == true
    }
    val firstTransitionAfterPreinfusion = phaseStart(1)
        ?: orderedTransitions.firstOrNull()?.elapsedMillis?.toDouble()

    val splitMillis = when {
        namedExtractionIndex != null && namedExtractionIndex == 0 -> 0.0
        namedExtractionIndex != null -> phaseStart(namedExtractionIndex)
            ?: pressureCrossing(0.0, phaseEnd(namedExtractionIndex) ?: durationMillis)
            ?: firstTransitionAfterPreinfusion
        highPressureIndex != null -> {
            val start = phaseStart(highPressureIndex) ?: 0.0
            pressureCrossing(start, phaseEnd(highPressureIndex) ?: durationMillis) ?: start
        }
        else -> pressureCrossing(0.0, durationMillis) ?: firstTransitionAfterPreinfusion ?: 0.0
    }?.coerceIn(0.0, durationMillis) ?: 0.0

    fun withZeroOrigin(samples: List<Pair<Double, Float>>): List<Pair<Double, Float>> = when {
        samples.isEmpty() || samples.first().first <= 0.0 -> samples
        else -> listOf(0.0 to 0f) + samples
    }

    fun valueAt(samples: List<Pair<Double, Float>>, timeMillis: Double): Float? {
        if (samples.isEmpty()) return null
        samples.firstOrNull { abs(it.first - timeMillis) < 1.0 }?.let { return it.second }
        val before = samples.lastOrNull { it.first < timeMillis } ?: return null
        val after = samples.firstOrNull { it.first > timeMillis } ?: return before.second
        val span = after.first - before.first
        if (span <= 0.0) return after.second
        val fraction = ((timeMillis - before.first) / span).toFloat()
        return before.second + (after.second - before.second) * fraction
    }

    val cumulativeWeights = withZeroOrigin(if (scaleWeights.isNotEmpty()) scaleWeights else chartWeights)
    val cumulativeVolumes = withZeroOrigin(points.map { (time, data) -> time to data.volume })

    fun weightAt(timeMillis: Double): Float? {
        if (!hasWeight) return null
        if (timeMillis <= 0.0) return 0f
        if (timeMillis >= durationMillis - 1.0) finalWeightGrams?.takeIf { it.isFinite() }?.let { return it }
        return valueAt(cumulativeWeights, timeMillis)
    }

    fun volumeAt(timeMillis: Double): Float? = if (hasVolume) valueAt(cumulativeVolumes, timeMillis) else null
    fun delta(start: Float?, end: Float?): Float? =
        if (start != null && end != null && end >= start) end - start else null

    fun row(startMillis: Double, endMillis: Double): HistoryTableRow {
        val start = startMillis.coerceIn(0.0, durationMillis)
        val end = endMillis.coerceIn(start, durationMillis)
        val stagePoints = points.filter { (time, _) -> time >= start && time <= end }
        val last = stagePoints.lastOrNull()?.second
        val lastWithTemperature = stagePoints.lastOrNull { it.second.temperatureC != null }?.second
        return HistoryTableRow(
            durationSeconds = (end - start) / 1000.0,
            weightGrams = delta(weightAt(start), weightAt(end)),
            volumeMl = delta(volumeAt(start), volumeAt(end)),
            temperatureC = lastWithTemperature?.temperatureC,
            flowMlPerSecond = last?.volumePerSecond?.takeIf { hasFlow },
            pressureBar = last?.pressure?.takeIf { hasPressure },
        )
    }

    val last = points.lastOrNull()?.second
    val total = HistoryTableRow(
        durationSeconds = durationSeconds,
        weightGrams = finalWeightGrams?.takeIf { it.isFinite() }
            ?: scales.lastOrNull()?.weightGrams
            ?: last?.weight?.takeIf { hasWeight },
        volumeMl = last?.volume?.takeIf { hasVolume },
        temperatureC = points.lastOrNull { it.second.temperatureC != null }?.second?.temperatureC,
        flowMlPerSecond = last?.volumePerSecond?.takeIf { hasFlow },
        pressureBar = last?.pressure?.takeIf { hasPressure },
    )

    return HistoryTableData(
        preinfusion = row(0.0, splitMillis),
        extraction = row(splitMillis, durationMillis),
        total = total,
    )
}

private const val ExtractionPressureSplitBar = 6f
