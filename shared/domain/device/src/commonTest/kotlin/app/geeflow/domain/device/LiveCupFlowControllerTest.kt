package app.geeflow.domain.device

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.BrewPhase
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.ExitCondition
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.PhaseRamp
import app.geeflow.data.brew.model.RampStart
import app.geeflow.data.brew.model.RampStyle
import app.geeflow.data.brew.model.ThresholdComparison
import app.geeflow.data.device.model.BrewTelemetry
import app.geeflow.data.device.model.BookooScaleFrame

class LiveCupFlowControllerTest {
    @Test fun derivesFlowFromRecentScaleSamples() {
        val estimator = CupFlowEstimator()
        assertNull(estimator.add(0, 0f))
        assertNull(estimator.add(100, 0.2f))
        assertEquals(2f, estimator.add(300, .6f)!!, .01f)
    }

    @Test fun regressionToleratesNoisySamplesAndRejectsDelayedOutOfOrderSamples() {
        val estimator = CupFlowEstimator()
        assertNull(estimator.add(0, 0f))
        assertNull(estimator.add(100, .15f))
        assertEquals(2f, estimator.add(250, .52f)!!, .15f)
        assertNull(estimator.add(200, .4f))
    }

    @Test fun noDripsKeepCupFlowControllerAtBasePressure() {
        val recipe = recipe(BrewPhase("flow", control = PhaseControl.Pressure(3f), maximumDurationMillis = 10_000L))
            .copy(targets = mapOf("flow" to LiveControlTarget.CupFlow(1.5f, 3f)))
        val engine = LiveTestProgramEngine(recipe, LiveControlSettings())
        assertEquals(3f, (engine.tick(0, BrewTelemetry(emptyMap()), null, 0).target as PhaseControl.Pressure).bar)
        assertEquals(3f, (engine.tick(1_000, BrewTelemetry(emptyMap()), null, 1_000, false).target as PhaseControl.Pressure).bar)
    }

    @Test fun controllerRespectsPressureLimitsAndSlew() {
        val controller = LiveCupFlowController(LiveControlSettings())
        assertEquals(3f, controller.update(2f, 0f, 0))
        assertEquals(3.1f, controller.update(2f, 0f, 100), .001f)
        assertEquals(0f, controller.update(0f, 20f, 100_000))
    }

    @Test fun controllerSaturatesAtCeilingWithoutWindingUpPastIt() {
        val settings = LiveControlSettings(maximumPressureBar = 4f)
        val controller = LiveCupFlowController(settings)
        var target = 3f
        for (index in 0..30) target = controller.update(20f, 0f, index * 1_000L)
        assertEquals(4f, target, .001f)
        assertEquals(3f, controller.update(0f, 20f, 31_000L), .001f)
    }

    @Test fun scaleLossFallsBackAndHonorsEarlierDeadline() {
        assertEquals(ScaleFallback(3f, 6_000), scaleLossFallback(5f, 1_000, 6_000))
        assertEquals(ScaleFallback(1.5f, 7_000), scaleLossFallback(1.5f, 2_000, 9_000))
        assertEquals(ScaleFallback(2f, 6_000), scaleLossFallback(8f, 1_000, 9_000, 2f))
    }

    @Test fun scaleFallbackRemainsLatchedAfterFreshSamplesReturn() {
        val started = scaleFallbackStartMillis(null, scaleDataValid = true, scaleSampleAgeMillis = 1_001, nowMillis = 2_000)
        assertEquals(2_000L, started)
        assertEquals(
            2_000L,
            scaleFallbackStartMillis(started, scaleDataValid = true, scaleSampleAgeMillis = 20, nowMillis = 2_500),
        )
        assertEquals(null, scaleFallbackStartMillis(null, scaleDataValid = true, scaleSampleAgeMillis = 20, nowMillis = 500))
    }

    @Test fun commandGateCoalescesCongestedTargetsAndDropsThemAfterStop() {
        LiveControlSettings.ALLOWED_COMMAND_INTERVALS_MILLIS.forEach { interval ->
            val gate = LiveTargetCommandGate(interval)
            assertEquals(PhaseControl.Pressure(3f), gate.offer(PhaseControl.Pressure(3f), 0))
            assertNull(gate.offer(PhaseControl.Pressure(4f), interval - 1))
            assertEquals(PhaseControl.Pressure(5f), gate.offer(PhaseControl.Pressure(5f), interval))
            gate.offer(PhaseControl.Pressure(6f), interval + 1)
            gate.stop()
            assertNull(gate.offer(PhaseControl.Pressure(7f), interval * 2))
        }
    }

    @Test fun onlyMeasuredTrialCommandIntervalsAreAccepted() {
        LiveControlSettings.ALLOWED_COMMAND_INTERVALS_MILLIS.forEach { interval ->
            assertEquals(interval, LiveControlSettings(commandIntervalMillis = interval).commandIntervalMillis)
        }
        assertFailsWith<IllegalArgumentException> { LiveControlSettings(commandIntervalMillis = 50L) }
        assertFailsWith<IllegalArgumentException> { LiveControlSettings(commandIntervalMillis = 750L) }
    }

    @Test fun scaleGuardRejectsTimerResetAndTareOrCupRemovalUntilNextShot() {
        val guard = LiveScaleSampleGuard(scaleFrame(timer = 1000, weight = 3f))
        assertNull(guard.accept(scaleFrame(timer = 1100, weight = 3.2f)))
        assertEquals(ScaleInvalidReason.TimerReset, guard.accept(scaleFrame(timer = 200, weight = 3.2f)))
        assertEquals(ScaleInvalidReason.TimerReset, guard.accept(scaleFrame(timer = 1200, weight = 3.4f)))

        val cupRemoved = LiveScaleSampleGuard(scaleFrame(timer = 1000, weight = 8f))
        assertEquals(ScaleInvalidReason.TareOrCupRemoval, cupRemoved.accept(scaleFrame(timer = 1100, weight = 4f)))
        assertEquals(false, cupRemoved.isValid)
    }

    @Test fun shadowEngineDoesNotIntegrateARepeatedScaleMeasurement() {
        val recipe = recipe(BrewPhase("flow", control = PhaseControl.Pressure(3f), maximumDurationMillis = 10_000L))
        val engine = LiveTestProgramEngine(
            recipe.copy(targets = mapOf("flow" to LiveControlTarget.CupFlow(0.5f))),
            LiveControlSettings(),
        )
        assertEquals(3f, (engine.tick(0, BrewTelemetry(emptyMap()), 2f, 0, false).target as PhaseControl.Pressure).bar)
        assertEquals(3f, (engine.tick(100, BrewTelemetry(emptyMap()), 2f, 100, true).target as PhaseControl.Pressure).bar)
        assertEquals(3f, (engine.tick(150, BrewTelemetry(emptyMap()), 2f, 150, false).target as PhaseControl.Pressure).bar)
        assertEquals(2.9f, (engine.tick(200, BrewTelemetry(emptyMap()), 2f, 200, true).target as PhaseControl.Pressure).bar, .001f)
    }

    @Test fun phaseTransitionCarriesCurrentPressureWithoutAJump() {
        val recipe = recipe(
            BrewPhase("high", control = PhaseControl.Pressure(6f), maximumDurationMillis = 1_000L),
            BrewPhase("flow", control = PhaseControl.Pressure(3f), maximumDurationMillis = 9_000L),
        ).copy(targets = mapOf(
            "high" to LiveControlTarget.Pressure(6f),
            "flow" to LiveControlTarget.CupFlow(2f, basePressureBar = 3f),
        ))
        val engine = LiveTestProgramEngine(recipe, LiveControlSettings())
        assertEquals(6f, (engine.tick(0, BrewTelemetry(emptyMap()), null, 0).target as PhaseControl.Pressure).bar)
        assertEquals(6f, (engine.tick(1_000, BrewTelemetry(emptyMap()), null, 1_000, false).target as PhaseControl.Pressure).bar)
        assertEquals(6f, (engine.tick(1_100, BrewTelemetry(emptyMap()), 2f, 1_100, true).target as PhaseControl.Pressure).bar)
        assertEquals(5.9f, (engine.tick(1_200, BrewTelemetry(emptyMap()), 2f, 1_200, true).target as PhaseControl.Pressure).bar, .001f)
    }

    @Test fun pressureRampIsEvaluatedBeforeFlowControl() {
        val recipe = recipe(
            BrewPhase("preinfusion", control = PhaseControl.Pressure(3f), maximumDurationMillis = 1_000L),
            BrewPhase(
                "ramp",
                control = PhaseControl.Pressure(7f),
                maximumDurationMillis = 5_000L,
                ramp = PhaseRamp(RampStyle.Linear, 2_000L, RampStart.PreviousTarget),
            ),
        )
        val engine = LiveTestProgramEngine(recipe, LiveControlSettings())
        assertEquals(3f, (engine.tick(0, BrewTelemetry(emptyMap()), null, 0).target as PhaseControl.Pressure).bar)
        assertEquals(3f, (engine.tick(1_000, BrewTelemetry(emptyMap()), null, 1_000).target as PhaseControl.Pressure).bar)
        assertEquals(5f, (engine.tick(2_000, BrewTelemetry(emptyMap()), null, 2_000).target as PhaseControl.Pressure).bar)
    }

    @Test fun pausePhasePreservesPressureModeTarget() {
        val recipe = LiveTestRecipe(
            profile = BrewProfile(
                userId = 0,
                name = "pause test",
                description = "",
                finishCondition = Condition.Weight(36f),
                program = BrewProgram.Phases(
                    listOf(BrewPhase("pause", control = PhaseControl.PumpPause, maximumDurationMillis = 1_000L)),
                ),
            ),
            targets = mapOf("pause" to LiveControlTarget.Pause),
        )
        val output = LiveTestProgramEngine(recipe, LiveControlSettings()).tick(0, BrewTelemetry(emptyMap()), null, 0)
        assertEquals(PhaseControl.PumpPause, output.target)
        assertEquals("pause", output.phaseId)
    }

    @Test fun pressureSensorExitUsesSharedRecipeConditions() {
        val phase = BrewPhase(
            "pressure-ramp",
            control = PhaseControl.Pressure(4f),
            maximumDurationMillis = 5_000L,
            exitConditions = listOf(ExitCondition(BrewMetric.PumpPressure, ThresholdComparison.Above, 6f)),
        )
        val engine = LiveTestProgramEngine(recipe(phase), LiveControlSettings())
        assertEquals(PhaseControl.Pressure(4f), engine.tick(0, telemetry(0f), null, 0).target)
        val output = engine.tick(100, BrewTelemetry(mapOf(BrewMetric.PumpPressure to 6.1f)), null, 100)
        assertEquals("program_complete", output.finish)
        assertNull(output.target)
    }

    @Test fun weightTargetFinishesTheRecipeAndSuppressesTargets() {
        val recipe = recipe(BrewPhase("pressure", control = PhaseControl.Pressure(4f), maximumDurationMillis = 10_000L))
        val engine = LiveTestProgramEngine(recipe, LiveControlSettings())
        assertEquals(PhaseControl.Pressure(4f), engine.tick(0, telemetry(35.9f), null, 0).target)
        val finish = engine.tick(100, telemetry(36f), null, 100)
        assertEquals("global_goal", finish.finish)
        assertNull(finish.target)
    }

    private fun recipe(vararg phases: BrewPhase) = LiveTestRecipe(
        profile = BrewProfile(
            userId = 0,
            name = "test",
            description = "",
            finishCondition = Condition.Weight(36f),
            program = BrewProgram.Phases(phases.toList()),
        ),
        targets = phases.associate { phase ->
            phase.id to LiveControlTarget.Pressure((phase.control as PhaseControl.Pressure).bar)
        },
    )

    private fun telemetry(weight: Float) = BrewTelemetry(mapOf(app.geeflow.data.brew.model.BrewMetric.CupWeight to weight))

    private fun scaleFrame(timer: Long, weight: Float) = BookooScaleFrame(timer, weight, 0f, 100)
}
