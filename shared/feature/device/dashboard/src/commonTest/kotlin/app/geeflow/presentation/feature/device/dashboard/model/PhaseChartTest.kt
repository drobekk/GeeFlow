package app.geeflow.presentation.feature.device.dashboard.model

import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.BrewPhase
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.ConditionOperator
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
        val program = BrewProgram.Phases(
            listOf(
                BrewPhase(id = "first", control = PhaseControl.Pressure(3f), maximumDurationMillis = 10000),
                BrewPhase(
                    id = "second",
                    control = PhaseControl.Pressure(7f),
                    maximumDurationMillis = 10000,
                    ramp = PhaseRamp(style = RampStyle.Linear, durationMillis = 2000),
                ),
            ),
        )
        val initial = program.toTargetData(transitions = emptyList())
        assertEquals(20f, initial.keys.max())
        assertEquals(7f, initial.getValue(20f).pressure)

        val curve = program.toTargetData(
            transitions = listOf(
                PhaseTransition(elapsedMillis = 3200, phaseId = "first", reason = PhaseExitReason.ConditionMatched),
            ),
        )
        assertEquals(13.2f, curve.keys.max())
        assertEquals(3f, curve.getValue(3.2f).pressure)
        assertEquals(5f, curve.getValue(4.2f).pressure)
        assertEquals(7f, curve.getValue(5.2f).pressure)
    }

    @Test
    fun firstRampStartsAtRestAndIncludesFractionalEndpoint() {
        val program = BrewProgram.Phases(
            listOf(
                BrewPhase(
                    id = "first",
                    control = PhaseControl.Pressure(6f),
                    maximumDurationMillis = 1250,
                    ramp = PhaseRamp(style = RampStyle.Linear, durationMillis = 1000),
                ),
            ),
        )
        val curve = program.toTargetData()
        assertEquals(0f, curve.getValue(0f).pressure)
        assertEquals(3f, curve.getValue(0.5f).pressure)
        assertEquals(6f, curve.getValue(1.25f).pressure)
    }

    @Test
    fun actualConditionMovesFollowingStepsAndHighlightsOnlyMatchingThreshold() {
        val above = ExitCondition(
            metric = BrewMetric.PumpPressure,
            comparison = ThresholdComparison.Above,
            threshold = 4f,
        )
        val below = above.copy(comparison = ThresholdComparison.Below, threshold = 2f)
        val phase = BrewPhase(
            id = "first",
            control = PhaseControl.Flow(3f),
            maximumDurationMillis = 10000,
            exitConditions = listOf(above, below),
        )
        val program = BrewProgram.Phases(listOf(phase, phase.copy(id = "second")))
        val boundaries = program.chartBoundaries(
            listOf(
                PhaseTransition(
                    elapsedMillis = 3200,
                    phaseId = "first",
                    reason = PhaseExitReason.ConditionMatched,
                    condition = below,
                ),
            ),
        )
        assertEquals(listOf(0.0, 3.2, 13.2), boundaries.map { it.seconds })
        assertEquals(1, boundaries[1].stepNumber)
        assertEquals(below, boundaries[1].matchedConditions.singleOrNull())
        assertNull(boundaries.last().matchedConditions.singleOrNull())
        assertEquals(2, boundaries.last().stepNumber)
    }

    @Test
    fun `native steps highlight elapsed time without marking future steps as completed`() {
        val phase = BrewPhase(id = "first", control = PhaseControl.Pressure(3f), maximumDurationMillis = 10000)
        val program = BrewProgram.Phases(listOf(phase, phase.copy(id = "second")))
        val boundaries = program.nativeChartBoundaries(
            data = mapOf(12f to ChartData(pressure = 0f, weight = 0f, weightPerSecond = 0f, volume = 0f, volumePerSecond = 0f)),
            finishCondition = Condition.Volume(40f),
        )
        assertEquals(BrewMetric.PhaseTime, boundaries.first().matchedConditions.singleOrNull()?.metric)
        assertNull(boundaries.last().matchedConditions.singleOrNull())
        assertEquals(
            listOf(BrewMetric.PumpedVolume),
            boundaries.last().conditions.map { it.metric },
        )
    }

    @Test
    fun `native volume target ends the current step and removes future boundaries`() {
        val phase = BrewPhase(id = "first", control = PhaseControl.Pressure(3f), maximumDurationMillis = 10000)
        val program = BrewProgram.Phases(listOf(phase, phase.copy(id = "second")))
        val boundaries = program.nativeChartBoundaries(
            data = mapOf(5f to ChartData(pressure = 0f, weight = 0f, weightPerSecond = 0f, volume = 40f, volumePerSecond = 0f)),
            finishCondition = Condition.Volume(40f),
        )
        assertEquals(1, boundaries.size)
        assertEquals(5.0, boundaries.single().seconds)
        assertEquals(BrewMetric.PumpedVolume, boundaries.single().matchedConditions.singleOrNull()?.metric)
    }

    @Test
    fun `native final weight highlights the last step at the recorded end time`() {
        val phase = BrewPhase(id = "first", control = PhaseControl.Pressure(6f), maximumDurationMillis = 5000)
        val program = BrewProgram.Phases(
            listOf(
                phase,
                phase.copy(id = "second", maximumDurationMillis = 2000),
                phase.copy(id = "third", maximumDurationMillis = 13000),
            ),
        )
        val point = ChartData(pressure = 7f, weight = 59f, weightPerSecond = 6f, volume = 88f, volumePerSecond = 6f)
        val before = program.nativeChartBoundaries(mapOf(17f to point), Condition.Weight(60f))
        assertNull(before.last().matchedConditions.singleOrNull())
        val after = program.nativeChartBoundaries(mapOf(17f to point.copy(weight = 60f)), Condition.Weight(60f))
        assertEquals(listOf(5.0, 7.0, 17.0), after.map { it.seconds })
        assertEquals(3, after.last().stepNumber)
        assertEquals(BrewMetric.CupWeight, after.last().matchedConditions.single().metric)
    }

    @Test
    fun `native volume completion after the planned duration moves and highlights the final marker`() {
        val program = BrewProgram.Phases(
            listOf(BrewPhase(id = "first", control = PhaseControl.Pressure(9f), maximumDurationMillis = 30000)),
        )
        val point = ChartData(pressure = 9f, weight = 86f, weightPerSecond = 3f, volume = 93.7f, volumePerSecond = 3f)
        val before = program.nativeChartBoundaries(mapOf(30f to point), Condition.Volume(100f))
        assertNull(before.single().matchedConditions.singleOrNull())
        val after = program.nativeChartBoundaries(
            mapOf(30f to point, 32f to point.copy(volume = 100f)),
            Condition.Volume(100f),
        )
        assertEquals(32.0, after.single().seconds)
        assertEquals(BrewMetric.PumpedVolume, after.single().matchedConditions.single().metric)
    }

    @Test
    fun `native weight target stays unhighlighted when stopped below target`() {
        val program = BrewProgram.Phases(
            listOf(BrewPhase(id = "first", control = PhaseControl.Pressure(3f), maximumDurationMillis = 10000)),
        )
        val stopped = program.nativeChartBoundaries(
            data = mapOf(5f to ChartData(pressure = 0f, weight = 20f, weightPerSecond = 0f, volume = 0f, volumePerSecond = 0f)),
            finishCondition = Condition.Weight(40f),
        )
        assertNull(stopped.single().matchedConditions.singleOrNull())
        val finished = program.nativeChartBoundaries(
            data = mapOf(5f to ChartData(pressure = 0f, weight = 40f, weightPerSecond = 0f, volume = 0f, volumePerSecond = 0f)),
            finishCondition = Condition.Weight(40f),
        )
        assertEquals(BrewMetric.CupWeight, finished.single().matchedConditions.singleOrNull()?.metric)
    }

    @Test
    fun `native goal after planned duration replaces the final boundary`() {
        val phase = BrewPhase(id = "first", control = PhaseControl.Pressure(3f), maximumDurationMillis = 10000)
        val program = BrewProgram.Phases(listOf(phase, phase.copy(id = "second")))
        val boundaries = program.nativeChartBoundaries(
            data = mapOf(25f to ChartData(pressure = 0f, weight = 40f, weightPerSecond = 0f, volume = 0f, volumePerSecond = 0f)),
            finishCondition = Condition.Weight(40f),
        )
        assertEquals(listOf(1, 2), boundaries.map { it.stepNumber })
        assertEquals(listOf(10.0, 25.0), boundaries.map { it.seconds })
        assertEquals(BrewMetric.CupWeight, boundaries.last().matchedConditions.singleOrNull()?.metric)
    }

    @Test
    fun `AND highlights all measurement conditions but not the time limit`() {
        val pressure = ExitCondition(BrewMetric.PumpPressure, ThresholdComparison.Above, 4f)
        val volume = ExitCondition(BrewMetric.PumpedVolume, ThresholdComparison.Above, 20f)
        val phase = BrewPhase(
            id = "first",
            control = PhaseControl.Pressure(6f),
            maximumDurationMillis = 10000,
            exitConditions = listOf(pressure, volume),
            conditionOperator = ConditionOperator.And,
        )
        val program = BrewProgram.Phases(listOf(phase))
        val matched = program.chartBoundaries(
            listOf(PhaseTransition(5000, phase.id, PhaseExitReason.ConditionMatched, volume)),
        ).last()
        assertEquals(setOf(pressure, volume), matched.matchedConditions)
        val timedOut = program.chartBoundaries(
            listOf(PhaseTransition(10000, phase.id, PhaseExitReason.MaximumDuration)),
        ).last()
        assertEquals(setOf(BrewMetric.PhaseTime), timedOut.matchedConditions.map { it.metric }.toSet())
    }
}
