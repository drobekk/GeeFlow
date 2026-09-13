package app.geeflow.data.brew.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhaseConditionsTest {
    private val conditions = listOf(
        ExitCondition(BrewMetric.PhaseTime, ThresholdComparison.Above, 10f),
        ExitCondition(BrewMetric.PumpedVolume, ThresholdComparison.Above, 40f),
        ExitCondition(BrewMetric.PumpPressure, ThresholdComparison.Below, 2f))
    private fun phase() = BrewPhase(
        id = "test",
        control = PhaseControl.Pressure(3f),
        maximumDurationMillis = 60000,
        exitConditions = conditions,
    )

    @Test fun anyConditionEndsPhase() {
        conditions.forEach { matching -> assertTrue(phase().conditionsMet { it == matching }) }
        assertFalse(phase().conditionsMet { false })
    }

    @Test fun singleTimeConditionDeterminesPreviewDuration() {
        assertEquals(10000, phase().copy(exitConditions = conditions.take(1)).plannedDurationMillis())
        assertEquals(60000, phase().plannedDurationMillis())
    }
}
