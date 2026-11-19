package app.geeflow.presentation.feature.device.dashboard.model

import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.BrewPhase
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.BrewScaleSample
import app.geeflow.data.brew.model.ExitCondition
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.PhaseExitReason
import app.geeflow.data.brew.model.PhaseTransition
import app.geeflow.data.brew.model.ThresholdComparison
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BrewShotSummaryTest {
    @Test
    fun conditionalExtractionUsesRecordedTransitionAndReconcilesPhaseTotals() {
        val summary = brewShotSummary(
            measurements = mapOf(
                0f to data(0f, 0f, 0f, 0f, 84f),
                2f to data(3f, 3f, 5f, 2f, 87f),
                3f to data(4f, 4f, 8f, 4f, 89f),
                6f to data(2f, 10f, 20f, 1f, 90f),
            ),
            scaleSamples = listOf(scale(0, 0f), scale(2_000, 3f), scale(6_000, 9f)),
            profileProgram = conditionalProgram(),
            phaseTransitions = listOf(PhaseTransition(2_500, "pre", PhaseExitReason.ConditionMatched)),
            recordedExtractionStartMillis = null,
            extractionStartEstimated = null,
            recordedEndSeconds = 6.0,
            finalWeightGrams = 10f,
        )

        assertEquals(false, summary.splitEstimated)
        assertEquals(2.5, summary.preinfusionTimeSeconds)
        assertEquals(3.5, summary.extractionTimeSeconds)
        assertEquals(3f, summary.preinfusionWeightGrams)
        assertEquals(7f, summary.extractionWeightGrams)
        assertEquals(6.5f, summary.preinfusionVolumeMl)
        assertEquals(13.5f, summary.extractionVolumeMl)
        assertEquals("84→87", summary.preinfusionTemperature)
        assertEquals("0.0→2.0", summary.preinfusionFlow)
        assertEquals("0.0→3.0", summary.preinfusionPressure)
    }

    @Test
    fun fixedTimeShotStoppedBeforeExtractionKeepsAllElapsedTimeInPreinfusion() {
        val program = BrewProgram.Phases(
            listOf(
                phase("pre", "Preinfusion", listOf(ExitCondition(BrewMetric.PhaseTime, ThresholdComparison.Above, 5f))),
                phase("extract", "Extraction", emptyList()),
            ),
        )
        val summary = brewShotSummary(
            measurements = mapOf(0f to data(0f, 0f, 0f, 0f, null), 3f to data(2f, 2f, 6f, 2f, 87f)),
            scaleSamples = listOf(scale(0, 0f), scale(3_000, 2f)),
            profileProgram = program,
            phaseTransitions = emptyList(),
            recordedExtractionStartMillis = null,
            extractionStartEstimated = null,
            recordedEndSeconds = 3.0,
            finalWeightGrams = 2f,
        )

        assertEquals(3.0, summary.preinfusionTimeSeconds)
        assertEquals(0.0, summary.extractionTimeSeconds)
        assertEquals(2f, summary.preinfusionWeightGrams)
        assertEquals(0f, summary.extractionWeightGrams)
        assertEquals(false, summary.splitEstimated)
    }

    @Test
    fun estimatesSplitOnlyAfterSustainedCupWeightRise() {
        val summary = brewShotSummary(
            measurements = mapOf(
                0f to data(0f, 0f, 0f, 0f, 89f),
                .5f to data(0f, 0f, 0f, 2f, 89f),
                1f to data(1f, .5f, 0f, 2f, 90f),
                1.3f to data(1f, .6f, 0f, 2f, 90f),
                1.6f to data(1f, .7f, 0f, 2f, 90f),
                3f to data(0f, 5f, 0f, 0f, 88f),
            ),
            scaleSamples = listOf(scale(0, 0f), scale(1_000, .5f), scale(1_300, .6f), scale(1_600, .7f), scale(3_000, 5f)),
            profileProgram = null,
            phaseTransitions = emptyList(),
            recordedExtractionStartMillis = null,
            extractionStartEstimated = null,
            recordedEndSeconds = 3.0,
            finalWeightGrams = 5f,
        )

        assertTrue(summary.splitEstimated)
        assertEquals(1.0, summary.preinfusionTimeSeconds)
        assertEquals(2.0, summary.extractionTimeSeconds)
        assertTrue(summary.volumeEstimated)
        assertTrue(summary.totalVolumeMl != null)
    }

    @Test
    fun unknownSplitAndMissingSensorsLeavePhaseValuesUnavailable() {
        val summary = brewShotSummary(
            measurements = mapOf(0f to data(0f, 0f, 0f, 0f, null), 1f to data(0f, 0f, 0f, 0f, null)),
            scaleSamples = emptyList(),
            profileProgram = null,
            phaseTransitions = emptyList(),
            recordedExtractionStartMillis = null,
            extractionStartEstimated = null,
            recordedEndSeconds = 1.0,
            finalWeightGrams = 0f,
        )

        assertNull(summary.preinfusionTimeSeconds)
        assertNull(summary.extractionTimeSeconds)
        assertNull(summary.preinfusionWeightGrams)
        assertNull(summary.extractionWeightGrams)
        assertNull(summary.preinfusionVolumeMl)
        assertNull(summary.totalVolumeMl)
        assertNull(summary.preinfusionTemperature)
        assertNull(summary.preinfusionFlow)
        assertNull(summary.preinfusionPressure)
        assertNull(summary.totalWeightGrams)
    }

    @Test
    fun recordedBoundaryWeightAndFinalWeightMatchStoppedAndReopenedSummaryInputs() {
        val samples = listOf(scale(0, 0f), scale(2_000, 4.5f), scale(5_000, 9f))
        val measurements = mapOf(0f to data(0f, 0f, 0f, 0f, 90f), 2f to data(0f, 4.5f, 8f, 4f, 91f), 5f to data(0f, 8f, 20f, 3f, 90f))
        fun summary() = brewShotSummary(
            measurements = measurements,
            scaleSamples = samples,
            profileProgram = null,
            phaseTransitions = emptyList(),
            recordedExtractionStartMillis = 2_000,
            extractionStartEstimated = false,
            recordedEndSeconds = 5.0,
            finalWeightGrams = 9.4f,
        )

        val live = summary()
        val reopened = summary()
        assertEquals(live, reopened)
        assertEquals(4.5f, live.preinfusionWeightGrams)
        assertEquals(4.9f, live.extractionWeightGrams!!, .001f)
        assertEquals(9.4f, live.totalWeightGrams)
        assertEquals(12f, live.extractionVolumeMl)
    }

    private fun conditionalProgram() = BrewProgram.Phases(
        listOf(
            phase("pre", "Preinfusion", listOf(ExitCondition(BrewMetric.PumpedVolume, ThresholdComparison.Above, 8f))),
            phase("extract", "Extraction start", emptyList()),
        ),
    )

    private fun phase(id: String, name: String, exits: List<ExitCondition>) = BrewPhase(
        id = id,
        name = name,
        control = PhaseControl.Pressure(3f),
        maximumDurationMillis = 8_000,
        exitConditions = exits,
    )

    private fun data(pressure: Float, weight: Float, volume: Float, flow: Float, temp: Float?) = ChartData(
        pressure = pressure,
        weight = weight,
        weightPerSecond = 0f,
        volume = volume,
        volumePerSecond = flow,
        temperatureC = temp,
    )

    private fun scale(elapsed: Long, weight: Float) = BrewScaleSample(
        elapsedMillis = elapsed,
        receivedAtEpochMillis = elapsed,
        weightGrams = weight,
        rawPacketHex = "",
        decoded = true,
    )
}
