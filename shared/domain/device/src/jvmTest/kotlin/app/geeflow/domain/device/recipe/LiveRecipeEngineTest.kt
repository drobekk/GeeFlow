package app.geeflow.domain.device.recipe

import app.geeflow.data.brew.model.*
import app.geeflow.data.device.model.*
import kotlin.test.*

class LiveRecipeEngineTest {
    private fun fixture(name: String) = LiveRecipeImport.parse(requireNotNull(javaClass.getResource("/live_recipes/$name")).readText(), name)
    private fun engine(document: LiveRecipeDocument) = LiveRecipeEngine(LiveRecipeCompiler.compile(document))
    private fun measurements(at: Long, pressure: Float = 3f, flow: Float = 1.3f, cupFlow: Float = .9f, weight: Float = 0f, volume: Float = 0f) = RecipeMeasurements(mapOf(
        RecipeMetric.Pressure to TimedRecipeValue(pressure, at), RecipeMetric.MachineFlow to TimedRecipeValue(flow, at),
        RecipeMetric.CupFlow to TimedRecipeValue(cupFlow, at), RecipeMetric.Weight to TimedRecipeValue(weight, at), RecipeMetric.Volume to TimedRecipeValue(volume, at),
    ))
    private fun pressurize(runtime: LiveRecipeEngine, flow: Float): RecipeEngineOutput {
        runtime.tick(0, measurements(0, pressure = 0f, flow = 8f))
        runtime.tick(5_000, measurements(5_000, pressure = 1f, flow = 8f, volume = 40f))
        runtime.tick(5_100, measurements(5_100, pressure = 3.1f, flow = 5f, volume = 41f))
        runtime.tick(5_200, measurements(5_200, pressure = 3f, flow = 2.5f, volume = 42f))
        runtime.tick(11_200, measurements(11_200, pressure = .1f, flow = .2f, volume = 43f))
        runtime.tick(14_200, measurements(14_200, pressure = 5f, flow = flow, volume = 45f))
        return runtime.tick(17_200, measurements(17_200, pressure = 8.9f, flow = flow, volume = 48f))
    }
    @Test fun sourceImportPreservesAdaptivePreparationAndLimits() {
        val recipe = fixture("best_practice.json")
        assertEquals(7, recipe.phases.size)
        assertEquals(101_000L, recipe.settings.deadlineMillis)
        assertEquals(11f, recipe.settings.pressureCeilingBar)
        assertEquals(88, recipe.settings.temperatureC)
        assertEquals(8f, recipe.phases[0].target.value)
        assertEquals(3.5f, recipe.phases[4].machineFlowLimitMlPerSecond)
        assertEquals(9.5f, recipe.phases.last().pressureLimitBar)
        assertEquals(0, LiveRecipeCompiler.issues(recipe).size)
        assertNotNull(recipe.originalSourceJson)
    }
    @Test fun restrictiveAndPermissivePucksInitializeDifferentFlowRamps() {
        val recipe = fixture("best_practice.json")
        val fine = engine(recipe); val coarse = engine(recipe)
        val a = pressurize(fine, 1.3f); val b = pressurize(coarse, 2.2f)
        assertEquals("step-6", a.phaseId)
        assertEquals(1.3f, a.requestedTarget!!, .001f)
        assertEquals(2.2f, b.requestedTarget!!, .001f)
        assertEquals(RecipeUnit.MillilitresPerSecond, a.targetUnit)
        assertEquals(17_200L, fine.captures.values.single().sample.atMillis)
        assertTrue(a.events.any { it.type == "phase_entered" && it.fields["phase_id"] == "step-5" })
        // The zero-duration source frame never emits its fixed 1.5 mL/s target.
        assertNotEquals(1.5f, a.requestedTarget)
        val later = fine.tick(47_200, measurements(47_200))
        assertEquals(2.4f, later.requestedTarget!!, .001f)
    }
    @Test fun capturedValuesAreResetBetweenShotsAndAfterStopNoTargetReturns() {
        val recipe = fixture("best_practice.json")
        val first = engine(recipe); pressurize(first, 1.3f); first.stop()
        assertNull(first.tick(17_300, measurements(17_300)).appliedPressureBar)
        val second = engine(recipe)
        assertTrue(second.captures.isEmpty())
        assertEquals(2.2f, pressurize(second, 2.2f).requestedTarget!!, .001f)
    }
    @Test fun cupFlowConversionRequiresExplicitTargetAndUsesMassMeasurement() {
        val source = fixture("best_practice.json")
        val copy = LiveRecipeImport.cupFlowCopy(source, "step-6", 1.5f, "cup-copy")
        assertEquals(RecipeControl.MachineFlow, source.phases.last().control)
        val output = pressurize(engine(copy), 2.2f)
        assertEquals(RecipeUnit.GramsPerSecond, output.targetUnit)
        assertEquals(.9f, output.requestedTarget!!, .001f)
        assertEquals(1.5f, copy.phases.last().target.value)
        assertFailsWith<IllegalArgumentException> { LiveRecipeImport.cupFlowCopy(source, "step-6", Float.NaN, "invalid") }
    }
    @Test fun creminaMaintainsPressureTrajectoryAndIndependentYield() {
        val recipe = fixture("cremina.json")
        assertEquals(75_000L, recipe.settings.deadlineMillis)
        assertEquals(92, recipe.settings.temperatureC)
        assertTrue(recipe.phases.all { it.control == RecipeControl.Pressure })
        val runtime = engine(recipe)
        runtime.tick(0, measurements(0, pressure = 0f))
        runtime.tick(2_000, measurements(2_000, pressure = 1.1f))
        runtime.tick(5_000, measurements(5_000, pressure = 1.1f))
        runtime.tick(15_000, measurements(15_000, pressure = 1.1f))
        assertEquals(5.05f, runtime.tick(20_000, measurements(20_000)).requestedTarget!!, .001f)
        runtime.tick(25_000, measurements(25_000, pressure = 9f))
        assertEquals(6f, runtime.tick(50_000, measurements(50_000)).requestedTarget!!, .001f)
        val changed = recipe.copy(settings = recipe.settings.copy(targetWeightGrams = 30f))
        assertEquals(recipe.phases, changed.phases)
        assertEquals("global_weight", engine(changed).tick(0, measurements(0, weight = 30f)).finish)
    }
    @Test fun genericSyntheticRecipeWorksWithAnUnrelatedNameAndPhaseOrder() {
        val synthetic = LiveRecipeDocument(id = "unseen", name = "Different cup experiment", phases = listOf(
            RecipePhase("saturate", target = RecipeValue(value = 2f), maximumDurationMillis = 100, captures = listOf(RecipeCapture("measured-cup", RecipeMetric.CupFlow))),
            RecipePhase("marker", maximumDurationMillis = 0),
            RecipePhase("output", control = RecipeControl.CupFlow, target = RecipeValue(value = 2f), maximumDurationMillis = 1_000,
                rampMillis = 1_000, rampStart = RecipeValue(RecipeValueSource.Capture, captureId = "measured-cup")),
        ))
        val decoded = LiveRecipeImport.parse(LiveRecipeJson.encode(synthetic), "imported-unseen")
        val runtime = engine(decoded)
        runtime.tick(0, measurements(0))
        assertEquals(.9f, runtime.tick(100, measurements(100)).requestedTarget!!, .001f)
        assertEquals(1.45f, runtime.tick(600, measurements(600)).requestedTarget!!, .001f)
    }
    @Test fun rejectsForwardCaptureReferencesAndMixedFlowUnits() {
        val source = fixture("best_practice.json")
        val phase = source.phases.last().copy(rampStart = RecipeValue(RecipeValueSource.Capture, captureId = "missing"))
        assertFailsWith<IllegalArgumentException> { LiveRecipeCompiler.compile(source.copy(phases = source.phases.dropLast(1) + phase)) }
        val wrongUnit = source.phases.last().copy(control = RecipeControl.CupFlow)
        assertTrue(LiveRecipeCompiler.issues(source.copy(phases = source.phases.dropLast(1) + wrongUnit)).any { "different unit" in it })
    }
    @Test fun missingOrStaleTransitionMeasurementNeverGetsSubstituted() {
        val source = fixture("best_practice.json")
        val runtime = engine(source)
        runtime.tick(0, measurements(0)); runtime.tick(5_000, measurements(5_000, pressure = 4f, flow = 2f))
        runtime.tick(5_100, measurements(5_100, flow = 2f))
        runtime.tick(11_100, measurements(11_100))
        val data = measurements(17_100).values.toMutableMap().also { it[RecipeMetric.MachineFlow] = TimedRecipeValue(1.3f, 1) }
        assertFailsWith<IllegalArgumentException> { runtime.tick(17_100, RecipeMeasurements(data)) }
    }
    @Test fun pressureSaturationAndSlewApplyToGenericFlowControllers() {
        val doc = LiveRecipeDocument(id = "saturation", name = "saturation", phases = listOf(
            RecipePhase("flow", control = RecipeControl.MachineFlow, target = RecipeValue(value = 8f), maximumDurationMillis = 39_000, pressureLimitBar = 4f),
        ), settings = RecipeShotSettings(pressureCeilingBar = 9f))
        val runtime = engine(doc)
        var previous = 0f
        for (time in 0..300) {
            val output = runtime.tick(time * 100L, measurements(time * 100L, flow = 0f))
            val pressure = output.appliedPressureBar!!
            assertTrue(pressure in 0f..4f)
            assertTrue(kotlin.math.abs(pressure - previous) <= .1001f)
            previous = pressure
        }
        assertEquals(4f, previous, .001f)
    }
    @Test fun timestampFreshnessAndRepeatedSamplesDoNotAdvanceIntegration() {
        val doc = LiveRecipeDocument(id = "stamps", name = "stamps", phases = listOf(RecipePhase("flow", control = RecipeControl.MachineFlow, target = RecipeValue(value = 3f), maximumDurationMillis = 5_000)))
        val runtime = engine(doc)
        runtime.tick(0, measurements(0, flow = 0f))
        val at100 = runtime.tick(100, measurements(100, flow = 0f)).appliedPressureBar!!
        val repeated = runtime.tick(150, measurements(100, flow = 0f)).requestedPressureBar!!
        assertEquals(at100, repeated, .001f)
        assertNull(measurements(0).fresh(RecipeMetric.CupFlow, 1_001))
    }
    @Test fun schemaAndUnknownExecutionFeaturesAreRejected() {
        val source = fixture("cremina.json")
        assertFailsWith<IllegalArgumentException> { LiveRecipeCompiler.compile(source.copy(version = 2)) }
        assertFailsWith<IllegalArgumentException> { LiveRecipeCompiler.compile(source.copy(unsupportedFeatures = listOf("unknown pump"))) }
        val json = source.originalSourceJson!!.replace("\"pump\": \"pressure\"", "\"pump\": \"unknown\"")
        assertTrue(LiveRecipeImport.parse(json, "unsupported").unsupportedFeatures.isNotEmpty())
    }
    @Test fun jsonAndLocalStorageRoundTripEveryExecutableField() {
        val source = fixture("best_practice.json").copy(mappingsAccepted = true)
        assertEquals(source, LiveRecipeJson.decode(LiveRecipeJson.encode(source)))
        var disk: String? = null
        val store = LocalLiveRecipeStore({ disk }, { disk = it })
        store.save(source)
        assertEquals(source, LocalLiveRecipeStore({ disk }, {}).recipes.value.single())
        store.save(source.copy(name = "Edited"))
        assertEquals(1, store.recipes.value.size)
        assertEquals(source.phases, store.recipes.value.single().phases)
        store.delete(source.id)
        assertTrue(store.recipes.value.isEmpty())
    }
    @Test fun corruptLocalDataCannotBeOverwrittenBySaving() {
        var disk = "corrupt original"
        val store = LocalLiveRecipeStore({ disk }, { disk = it })
        assertNotNull(store.loadError)
        assertFailsWith<IllegalStateException> { store.save(fixture("cremina.json")) }
        assertEquals("corrupt original", disk)
    }
    @Test fun compilationFreezesEditorCollections() {
        val phases = mutableListOf(RecipePhase("a"))
        val compiled = LiveRecipeCompiler.compile(LiveRecipeDocument(id = "freeze", name = "freeze", phases = phases))
        phases.clear()
        assertEquals(1, compiled.document.phases.size)
    }
    @Test fun limitsAndStepRelativeConditionsAreGeneric() {
        val recipe = LiveRecipeDocument(id = "step", name = "step", phases = listOf(
            RecipePhase("one", maximumDurationMillis = 100),
            RecipePhase("two", maximumDurationMillis = 10_000, minimumDurationMillis = 100,
                exits = listOf(RecipeExit(RecipeMetric.Weight, threshold = 2f, scope = MeasurementScope.Step))),
        ))
        val runtime = engine(recipe)
        runtime.tick(0, measurements(0, weight = 4f))
        runtime.tick(100, measurements(100, weight = 5f))
        assertNull(runtime.tick(150, measurements(150, weight = 9f)).finish)
        assertEquals("program_complete", runtime.tick(200, measurements(200, weight = 7f)).finish)
    }
    @Test fun deadlinesAreUnconditionalWhenSensorExitsDoNotMatch() {
        val document = LiveRecipeDocument(id = "deadline", name = "deadline", phases = listOf(RecipePhase("pressure", maximumDurationMillis = 50_000)), settings = RecipeShotSettings(deadlineMillis = 40_000))
        assertEquals("shot_deadline", engine(document).tick(40_000, RecipeMeasurements(emptyMap())).finish)
    }
    @Test fun legacyPhaseCopyDoesNotMutateTheOriginal() {
        val profile = BrewProfile(userId = 1, name = "Legacy", description = "", finishCondition = Condition.Weight(30f), program = BrewProgram.Phases(listOf(BrewPhase("phase", control = PhaseControl.Pressure(4f), maximumDurationMillis = 5_000))))
        val copy = LiveRecipeImport.fromProfile(profile, "local-copy")
        assertEquals(30f, copy.settings.yieldGrams)
        assertEquals(4f, copy.phases.single().target.value)
        assertEquals(Condition.Weight(30f), profile.finishCondition)
    }
    @Test fun maximumPhaseDurationAdvancesWithoutConsultingMissingExitSensor() {
        val document = LiveRecipeDocument(id = "max", name = "max", phases = listOf(RecipePhase("phase", maximumDurationMillis = 100,
            exits = listOf(RecipeExit(RecipeMetric.MachineFlow, threshold = 2f)))))
        val runtime = engine(document)
        runtime.tick(0, measurements(0))
        assertEquals("program_complete", runtime.tick(100, RecipeMeasurements(mapOf(RecipeMetric.Weight to TimedRecipeValue(0f, 100)))).finish)
    }
    @Test fun andConditionsRequireBothMeasurementsAndOrAcceptsEither() {
        val phase = RecipePhase("and", maximumDurationMillis = 10_000, conditionOperator = ConditionOperator.And,
            exits = listOf(RecipeExit(RecipeMetric.Pressure, threshold = 4f), RecipeExit(RecipeMetric.MachineFlow, threshold = 2f)))
        val document = LiveRecipeDocument(id = "conditions", name = "conditions", phases = listOf(phase))
        val runtime = engine(document)
        assertNull(runtime.tick(0, measurements(0, pressure = 5f, flow = 1f)).finish)
        assertEquals("program_complete", runtime.tick(100, measurements(100, pressure = 5f, flow = 3f)).finish)
        val either = engine(document.copy(phases = listOf(phase.copy(conditionOperator = ConditionOperator.Or))))
        assertEquals("program_complete", either.tick(0, measurements(0, pressure = 5f, flow = 1f)).finish)
    }
    @Test fun globalVolumeStopIsIndependentOfAndPhaseExits() {
        val profile = BrewProfile(userId = 1, name = "volume", description = "", finishCondition = Condition.Volume(40f),
            program = BrewProgram.Phases(listOf(BrewPhase("and", control = PhaseControl.Pressure(4f), maximumDurationMillis = 10_000,
                conditionOperator = ConditionOperator.And, exitConditions = listOf(ExitCondition(BrewMetric.PumpPressure, ThresholdComparison.Above, 8f))))))
        val document = LiveRecipeImport.fromProfile(profile, "volume-copy")
        assertEquals("global_volume", engine(document).tick(0, measurements(0, pressure = 2f, volume = 40f)).finish)
    }
    @Test fun existingAdaptiveProbeConvertsWithoutRecipeSpecificRuntimeLogic() {
        val source = BrewProfile(
            userId = 1,
            name = "Adaptive probe fixture",
            description = "",
            finishCondition = Condition.Volume(60f),
            program = BrewProgram.Phases(
                phases = listOf(
                    BrewPhase(
                        id = "preinfusion",
                        control = PhaseControl.Flow(3.5f),
                        maximumDurationMillis = 15_000,
                        exitConditions = listOf(
                            ExitCondition(BrewMetric.PumpPressure, ThresholdComparison.StrictlyAbove, 4f),
                        ),
                    ),
                    BrewPhase(
                        id = "adaptive-extraction",
                        control = PhaseControl.Flow(12f),
                        maximumDurationMillis = 60_000,
                        ramp = PhaseRamp(RampStyle.Linear, 7_000, RampStart.PreviousTarget),
                        adaptiveFlowProbe = AdaptiveFlowProbe(
                            samplingDurationMillis = 7_000,
                            pressureCeilingBar = 8.8f,
                            flowBackoffMillilitresPerSecond = 0.1f,
                        ),
                    ),
                ),
            ),
        )
        val document = LiveRecipeImport.fromProfile(source, "generic-probe")
        assertTrue(LiveRecipeCompiler.issues(document).isEmpty())
        val runtime = engine(document)
        runtime.tick(0, measurements(0, flow = 3.5f))
        runtime.tick(8_000, measurements(8_000, pressure = 4.1f, flow = 2f))
        runtime.tick(9_000, measurements(9_000, pressure = 5f, flow = 2.5f))
        runtime.tick(14_000, measurements(14_000, pressure = 8f, flow = 3f))
        assertEquals(3f, runtime.tick(15_000, measurements(15_000, pressure = 8f, flow = 3f)).requestedTarget!!, .001f)
        assertEquals(2.7f, runtime.tick(15_100, measurements(15_100, pressure = 9f, flow = 2.8f)).requestedTarget!!, .001f)
    }
    @Test fun sourceBoundsAreCheckedAgainstActualMachineRange() {
        val recipe = fixture("best_practice.json").copy(mappingsAccepted = true)
        LiveRecipeCompiler.compile(recipe)
        val capabilities = ProfilingCapabilities(livePressure = mapOf(PressureLocation.Pump to TargetRange(0f, 10f, .1f)),
            minimumWriteIntervalMillis = 100, telemetry = setOf(BrewMetric.PumpPressure, BrewMetric.PumpFlow, BrewMetric.PumpedVolume))
        assertTrue(LiveRecipeCompiler.issues(recipe, capabilities).any { "Machine cannot provide" in it })
        assertFalse(LiveRecipeCompiler.issues(fixture("cremina.json").copy(mappingsAccepted = true), capabilities).any { "Machine cannot provide" in it })
    }
}
