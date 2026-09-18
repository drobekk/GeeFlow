package app.geeflow.domain.device

import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.BrewPhase
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.ExitCondition
import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.brew.model.FreeHandRecording
import app.geeflow.data.brew.model.FreeHandSample
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.PhaseExitReason
import app.geeflow.data.brew.model.PhaseRamp
import app.geeflow.data.brew.model.RampStart
import app.geeflow.data.brew.model.RampStyle
import app.geeflow.data.brew.model.ThresholdComparison
import app.geeflow.data.device.model.BrewTelemetry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ProfileProgramEngineTest {
    private fun profile(vararg phases: BrewPhase) = BrewProfile(
        userId = 1,
        name = "Test",
        description = "",
        program = BrewProgram.Phases(phases.toList())
    )
    private fun telemetry(volume: Float = 0f, pressure: Float = 2f, weight: Float = 0f) = BrewTelemetry(
        mapOf(
            BrewMetric.PumpedVolume to volume,
            BrewMetric.PumpPressure to pressure,
            BrewMetric.CupWeight to weight
        )
    )
    private val first = BrewPhase("first", control = PhaseControl.Pressure(3f), maximumDurationMillis = 1000)

    @Test fun rampStartUsesChosenSource() {
        for ((start, expected) in listOf(RampStart.PreviousTarget to 5f, RampStart.CurrentMeasurement to 4.5f)) {
            val second = BrewPhase(
                "second",
                control = PhaseControl.Pressure(7f),
                maximumDurationMillis = 5000,
                ramp = PhaseRamp(RampStyle.Linear, 2000, start)
            )
            val engine = ProfileProgramEngine(profile(first, second))
            engine.tick(0, telemetry())
            engine.tick(1000, telemetry())
            assertEquals(PhaseControl.Pressure(expected), engine.tick(2000, telemetry()).target)
        }
    }

    @Test fun conditionsUseTotalVolumeInLaterPhase() {
        val second = first.copy(
            id = "second",
            maximumDurationMillis = 10000,
            minimumDurationMillis = 500,
            exitConditions = listOf(
                ExitCondition(BrewMetric.PumpedVolume, ThresholdComparison.Above, 40f),
                ExitCondition(BrewMetric.CupWeight, ThresholdComparison.Above, 36f)
            )
        )
        val engine = ProfileProgramEngine(profile(first, second))
        engine.tick(0, telemetry())
        engine.tick(1000, telemetry(volume = 20f))
        assertNull(engine.tick(1200, telemetry(volume = 70f)).finish)
        assertEquals("program_complete", engine.tick(1600, telemetry(volume = 50f)).finish)
        assertEquals(PhaseExitReason.ConditionMatched, engine.transitions.last().reason)
        assertEquals(BrewMetric.PumpedVolume, engine.transitions.last().condition?.metric)
    }

    @Test fun missingPressureCannotSatisfyBelowCondition() {
        val phase = first.copy(
            exitConditions = listOf(
                ExitCondition(
                    BrewMetric.PumpPressure,
                    ThresholdComparison.Below,
                    3f,
                ),
            ),
        )
        val engine = ProfileProgramEngine(profile(phase))
        assertNull(engine.tick(0, BrewTelemetry(emptyMap())).finish)
        assertEquals("program_complete", engine.tick(1000, BrewTelemetry(emptyMap())).finish)
    }

    @Test fun crossModeRampRequiresMeasurementOfNewDimension() {
        val next = first.copy(id = "next", control = PhaseControl.Flow(4f), ramp = PhaseRamp(RampStyle.Linear, 500))
        val engine = ProfileProgramEngine(profile(first, next))
        engine.tick(0, telemetry())
        assertFailsWith<IllegalArgumentException> { engine.tick(1000, telemetry()) }
    }

    @Test fun globalGoalStopsBeforePhaseTimeout() {
        val engine = ProfileProgramEngine(profile(first).copy(finishCondition = Condition.Weight(35f)))
        assertEquals("global_goal", engine.tick(0, telemetry(weight = 35f)).finish)
    }

    @Test fun longRecordingKeepsTimingThenHoldsFinalTargetUntilGoalOrTimeout() {
        val recording = FreeHandRecording(
            FreeHandControlMode.Flow,
            listOf(
                FreeHandSample(0, BrewDataPoint(0f, 0f, 0f, 2f, 0f)),
                FreeHandSample(83000, BrewDataPoint(0f, 0f, 457f, 4f, 0f))
            )
        )
        val profile = BrewProfile(
            userId = 1,
            name = "Long",
            description = "",
            program = BrewProgram.Recording(recording),
            finishCondition = Condition.Volume(500f)
        )
        val engine = ProfileProgramEngine(profile)
        assertEquals(PhaseControl.Flow(2f), engine.tick(63000, telemetry()).target)
        assertEquals(PhaseControl.Flow(4f), engine.tick(90000, telemetry()).target)
        assertEquals("recording_timeout", engine.tick(166000, telemetry()).finish)
    }

    @Test fun easingHasCorrectShapeAndEndpoints() {
        assertEquals(.25f, PhaseRamp(RampStyle.EaseIn, 1000).fraction(500))
        assertEquals(.75f, PhaseRamp(RampStyle.EaseOut, 1000).fraction(500))
        assertEquals(.125f, PhaseRamp(RampStyle.EaseInOut, 1000).fraction(250))
        RampStyle.entries.forEach { assertEquals(1f, PhaseRamp(it, 1000).fraction(1200)) }
    }
}
