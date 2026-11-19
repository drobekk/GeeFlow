package app.geeflow.presentation.feature.device.dashboard.model

import app.geeflow.data.brew.model.BrewPhase
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.PhaseExitReason
import app.geeflow.data.brew.model.PhaseRamp
import app.geeflow.data.brew.model.PhaseTransition
import app.geeflow.data.brew.model.RampStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HistoryTableDataTest {
    @Test
    fun `named extraction stage splits into two phases and interpolates cumulative samples`() {
        val program = BrewProgram.Phases(listOf(
            BrewPhase("pre", "Flow preinfusion", PhaseControl.Flow(2f), 5000),
            BrewPhase("extract", "Pressure extraction", PhaseControl.Pressure(9f), 5000),
            BrewPhase("taper", "Taper", PhaseControl.Pressure(5f), 5000),
        ))
        val result = historyTableData(
            program = program,
            transitions = listOf(
                PhaseTransition(3000, "pre", PhaseExitReason.ConditionMatched),
                PhaseTransition(6000, "extract", PhaseExitReason.MaximumDuration),
            ),
            measurements = mapOf(
                0f to point(pressure = 1f, weight = 0f, volume = 0f),
                2f to point(pressure = 2f, weight = 1f, volume = 3f),
                4f to point(pressure = 4f, weight = 3f, volume = 7f),
                6f to point(pressure = 8f, weight = 5f, volume = 12f),
                8f to point(pressure = 9f, weight = 8f, volume = 20f),
            ),
            scaleSamples = emptyList(),
            endSeconds = 8.0,
            finalWeightGrams = 8f,
            recordedPhaseIds = setOf("taper"),
        )

        assertEquals(3.0, result.preinfusion.durationSeconds)
        assertEquals(5.0, result.extraction.durationSeconds)
        assertEquals(2f, result.preinfusion.weightGrams)
        assertEquals(6f, result.extraction.weightGrams)
        assertEquals(5f, result.preinfusion.volumeMl)
        assertEquals(15f, result.extraction.volumeMl)
        assertEquals(8f, result.total.weightGrams)
        assertEquals(20f, result.total.volumeMl)
    }

    @Test
    fun `unnamed pressure ramp splits when recorded pressure crosses six bar`() {
        val program = BrewProgram.Phases(listOf(
            BrewPhase("fill", "Fill", PhaseControl.Flow(2f), 5000),
            BrewPhase(
                id = "ramp",
                name = "Ramp to 9 bar",
                control = PhaseControl.Pressure(9f),
                maximumDurationMillis = 5000,
                ramp = PhaseRamp(style = RampStyle.Linear, durationMillis = 5000),
            ),
            BrewPhase("hold", "Hold", PhaseControl.Pressure(9f), 5000),
        ))
        val result = historyTableData(
            program = program,
            transitions = listOf(
                PhaseTransition(4000, "fill", PhaseExitReason.ConditionMatched),
                PhaseTransition(9000, "ramp", PhaseExitReason.MaximumDuration),
            ),
            measurements = mapOf(
                0f to point(pressure = 1f, weight = 0f, volume = 0f),
                4f to point(pressure = 4f, weight = 2f, volume = 6f),
                6f to point(pressure = 5.5f, weight = 4f, volume = 10f),
                8f to point(pressure = 7f, weight = 7f, volume = 16f),
                10f to point(pressure = 9f, weight = 9f, volume = 21f),
                12f to point(pressure = 9f, weight = 12f, volume = 28f),
            ),
            scaleSamples = emptyList(),
            endSeconds = 12.0,
            finalWeightGrams = 12f,
            recordedPhaseIds = setOf("hold"),
        )

        assertEquals(6.6667, result.preinfusion.durationSeconds!!, 0.001)
        assertEquals(5.3333, result.extraction.durationSeconds!!, 0.001)
    }

    @Test
    fun `missing weight and volume stay unavailable while table retains two phases`() {
        val program = BrewProgram.Phases(listOf(
            BrewPhase("a", "Preinfusion", PhaseControl.Flow(2f), 5000),
            BrewPhase("b", "Extraction", PhaseControl.Pressure(8f), 5000),
        ))
        val result = historyTableData(
            program = program,
            transitions = listOf(PhaseTransition(2500, "a", PhaseExitReason.ConditionMatched)),
            measurements = mapOf(
                0f to point(pressure = 1f, weight = 0f, volume = 0f),
                2f to point(pressure = 2f, weight = 0f, volume = 0f),
                8f to point(pressure = 7f, weight = 0f, volume = 0f),
            ),
            scaleSamples = emptyList(),
            endSeconds = 8.0,
            finalWeightGrams = null,
            recordedPhaseIds = setOf("b"),
        )

        assertEquals(2.5, result.preinfusion.durationSeconds)
        assertEquals(5.5, result.extraction.durationSeconds)
        assertNull(result.preinfusion.weightGrams)
        assertNull(result.extraction.weightGrams)
        assertNull(result.preinfusion.volumeMl)
        assertNull(result.extraction.volumeMl)
        assertNull(result.total.weightGrams)
        assertNull(result.total.volumeMl)
    }

    private fun point(pressure: Float, weight: Float, volume: Float) =
        ChartData(pressure, weight, 0f, volume, 2f, 93f)
}
