package app.geeflow.presentation.feature.device.dashboard.model

import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.BrewPhase
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.ExitCondition
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.PhaseExitReason
import app.geeflow.data.brew.model.PhaseRamp
import app.geeflow.data.brew.model.PhaseTransition
import app.geeflow.data.brew.model.RampStyle
import app.geeflow.data.brew.model.ThresholdComparison
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PhaseChartTest {
    @Test
    fun livePlanKeepsFutureRampAndMovesItAfterEarlyTransition() {
        val program = BrewProgram.Phases(listOf(
            BrewPhase(id = "first", control = PhaseControl.Pressure(3f), maximumDurationMillis = 10000),
            BrewPhase(
                id = "second",
                control = PhaseControl.Pressure(7f),
                maximumDurationMillis = 10000,
                ramp = PhaseRamp(style = RampStyle.Linear, durationMillis = 2000),
            ),
        ))
        val initial = program.toTargetData(transitions = emptyList())
        assertEquals(20f, initial.keys.max())
        assertEquals(7f, initial.getValue(20f).pressure)

        val curve = program.toTargetData(transitions = listOf(
            PhaseTransition(elapsedMillis = 3200, phaseId = "first", reason = PhaseExitReason.ConditionMatched),
        ))
        assertEquals(13.2f, curve.keys.max())
        assertEquals(3f, curve.getValue(3.2f).pressure)
        assertEquals(5f, curve.getValue(4.2f).pressure)
        assertEquals(7f, curve.getValue(5.2f).pressure)
    }

    @Test
    fun firstRampStartsAtRestAndIncludesFractionalEndpoint() {
        val program = BrewProgram.Phases(listOf(
            BrewPhase(
                id = "first",
                control = PhaseControl.Pressure(6f),
                maximumDurationMillis = 1250,
                ramp = PhaseRamp(style = RampStyle.Linear, durationMillis = 1000),
            ),
        ))
        val curve = program.toTargetData()
        assertEquals(0f, curve.getValue(0f).pressure)
        assertEquals(3f, curve.getValue(0.5f).pressure)
        assertEquals(6f, curve.getValue(1.25f).pressure)
    }

    @Test
    fun actualConditionMovesFollowingStepsAndHighlightsOnlyMatchingThreshold() {
        val above = ExitCondition(metric = BrewMetric.PumpPressure, comparison = ThresholdComparison.Above, threshold = 4f)
        val below = above.copy(comparison = ThresholdComparison.Below, threshold = 2f)
        val phase = BrewPhase(
            id = "first",
            control = PhaseControl.Flow(3f),
            maximumDurationMillis = 10000,
            exitConditions = listOf(above, below),
        )
        val program = BrewProgram.Phases(listOf(phase, phase.copy(id = "second")))
        val boundaries = program.chartBoundaries(listOf(
            PhaseTransition(elapsedMillis = 3200, phaseId = "first", reason = PhaseExitReason.ConditionMatched, condition = below),
        ))
        assertEquals(listOf(0.0, 3.2, 13.2), boundaries.map { it.seconds })
        assertEquals(1, boundaries[1].stepNumber)
        assertEquals(below, boundaries[1].matchedCondition)
        assertNull(boundaries.last().matchedCondition)
        assertEquals(2, boundaries.last().stepNumber)
    }
}
