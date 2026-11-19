package app.geeflow.domain.device

import app.geeflow.data.brew.model.*
import app.geeflow.data.device.model.BrewTelemetry
import app.geeflow.data.device.model.value
import app.geeflow.data.device.model.withValue
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import kotlin.math.round
import kotlin.test.*

class RecipeV2Test {
    @Test fun sharedBrowserContract() {
        val text = requireNotNull(javaClass.getResource("/runtime-contract.json")).readText()
        val cases = Json.parseToJsonElement(text).jsonArray
        cases.forEach { item ->
            val case = item.jsonObject
            val profile = Json.decodeFromString<BrewProfile>(case.getValue("profile").toString())
            profile.program.validate()
            val engine = ProfileProgramEngine(profile)
            val expected = case.getValue("expected").jsonArray
            case.getValue("samples").jsonArray.forEachIndexed { index, element ->
                val sample = element.jsonObject
                val values = sample.getValue("values").jsonObject.mapKeys { BrewMetric.valueOf(it.key) }.mapValues { it.value.jsonPrimitive.float }
                val stamps = sample.getValue("sampledAtMillis").jsonObject.mapKeys { BrewMetric.valueOf(it.key) }.mapValues { it.value.jsonPrimitive.long }
                val output = engine.tick(sample.getValue("elapsedMillis").jsonPrimitive.long, BrewTelemetry(values, stamps))
                val target = assertNotNull(output.target, case.getValue("name").jsonPrimitive.content)
                assertEquals(expected[index].jsonPrimitive.float, target.value(), 0.0001f)
                val targets = assertNotNull(output.controlTargets)
                assertEquals(0f, if (targets.controller == FreeHandControlMode.Pressure) targets.flowMillilitresPerSecond else targets.pressureBar)
                case["expectedControllers"]?.jsonArray?.let { modes ->
                    assertEquals(modes[index].jsonPrimitive.content, if (targets.controller == FreeHandControlMode.Flow) "flow" else "pressure")
                }
                engine.acknowledgeApplied(target.withValue(round(target.value() * 10f) / 10f))
            }
        }
    }
    @Test fun deadlineAppliesToLegacyAndRecordingAndIsTerminal() {
        val phase = BrewPhase("p", control = PhaseControl.Pressure(3f), maximumDurationMillis = 60000)
        val profile = BrewProfile(userId=1,name="Deadline",description="",program=BrewProgram.Phases(listOf(phase)))
        val engine = ProfileProgramEngine(profile)
        assertNotNull(engine.tick(0,BrewTelemetry(emptyMap())).target)
        assertEquals("profile_timeout",engine.tick(40000,BrewTelemetry(emptyMap())).finish)
        engine.acknowledgeApplied(PhaseControl.Pressure(9f))
        assertNull(engine.tick(41000,BrewTelemetry(emptyMap())).target)
        val recording = profile.copy(program=BrewProgram.Recording(FreeHandRecording(FreeHandControlMode.Pressure,listOf(FreeHandSample(0,BrewDataPoint(3f,0f,0f,0f,0f)),FreeHandSample(60000,BrewDataPoint(3f,0f,0f,0f,0f))))))
        assertEquals("profile_timeout",ProfileProgramEngine(recording).tick(40000,BrewTelemetry(emptyMap())).finish)
    }
    @Test fun capturesRejectStaleReadingsAndInvalidReferences() {
        val phase=BrewPhase("capture",control=PhaseControl.Flow(3f),maximumDurationMillis=5000,entryCaptures=listOf(CaptureAction("x",BrewMetric.PumpFlow)),targetSource=TargetSource(TargetSourceKind.CapturedValue,capture="x"))
        val profile=BrewProfile(userId=1,name="Capture",description="",program=BrewProgram.Phases(listOf(phase),version=2))
        assertFails { ProfileProgramEngine(profile).tick(1000,BrewTelemetry(mapOf(BrewMetric.PumpFlow to 2f),mapOf(BrewMetric.PumpFlow to 0L))) }
        assertFails { (profile.program as BrewProgram.Phases).copy(phases=listOf(phase.copy(entryCaptures=emptyList()))).validate() }
    }
    @Test fun sensorStabilityNeverDelaysTheTimeDeadline() {
        val phase=BrewPhase("rules", control=PhaseControl.Pressure(3f), maximumDurationMillis=10000,
            conditionOperator=ConditionOperator.And, stabilityDurationMillis=5000,
            exitConditions=listOf(ExitCondition(BrewMetric.PumpFlow,ThresholdComparison.Above,2f), ExitCondition(BrewMetric.PhaseTime,ThresholdComparison.Above,1f)))
        val engine=ProfileProgramEngine(BrewProfile(userId=1,name="Deadline",description="",program=BrewProgram.Phases(listOf(phase),version=2)))
        assertNotNull(engine.tick(0,BrewTelemetry(mapOf(BrewMetric.PumpFlow to 3f),mapOf(BrewMetric.PumpFlow to 0L))).target)
        assertEquals("program_complete",engine.tick(1000,BrewTelemetry(mapOf(BrewMetric.PumpFlow to 3f),mapOf(BrewMetric.PumpFlow to 1000L))).finish)
    }
    @Test fun sustainedSecondaryOvershootTripsBeforeAnotherCommand() {
        val phase=BrewPhase("limited",control=PhaseControl.Flow(3f),maximumDurationMillis=10000,
            secondaryLimit=SecondaryLimit(BrewMetric.PumpPressure,9f,tripDurationMillis=200))
        val engine=ProfileProgramEngine(BrewProfile(userId=1,name="Limit",description="",program=BrewProgram.Phases(listOf(phase),version=2)))
        fun data(t:Long)=BrewTelemetry(mapOf(BrewMetric.PumpPressure to 11f),mapOf(BrewMetric.PumpPressure to t))
        assertNotNull(engine.tick(0,data(0)).target)
        assertNotNull(engine.tick(100,data(100)).target)
        val stop=engine.tick(200,data(200))
        assertEquals("secondary_limit_fault",stop.finish)
        assertNull(stop.target)
    }
    @Test fun controllerTargetsRejectSimultaneousCommandsAndKeepRecordedMeasurements() {
        assertFails { ControlTargets(FreeHandControlMode.Pressure,9f,2f) }
        assertFails { ControlTargets(FreeHandControlMode.Flow,9f,2f) }
        val data=BrewDataPoint(9f,5f,10f,2f,1f)
        for (mode in listOf(FreeHandControlMode.Pressure,FreeHandControlMode.Flow)) {
            val recording=FreeHandRecording(mode,listOf(FreeHandSample(0,data),FreeHandSample(1000,data)))
            val profile=BrewProfile(userId=1,name="Recording",description="",program=BrewProgram.Recording(recording))
            val output=ProfileProgramEngine(profile).tick(0,BrewTelemetry(emptyMap()))
            val targets=assertNotNull(output.controlTargets)
            assertEquals(mode,targets.controller)
            assertEquals(if(mode==FreeHandControlMode.Pressure)9f else 0f,targets.pressureBar)
            assertEquals(if(mode==FreeHandControlMode.Flow)2f else 0f,targets.flowMillilitresPerSecond)
            assertEquals(data,recording.samples.first().data)
        }
    }
    @Test fun rangesMustBeOrderedAndMonitorTheOtherParameter() {
        val phase=BrewPhase("p",control=PhaseControl.Pressure(9f),maximumDurationMillis=1000,
            secondaryLimit=SecondaryLimit(BrewMetric.PumpFlow,2.5f,minimum=1.5f))
        val program=BrewProgram.Phases(listOf(phase),version=2)
        program.validate()
        assertFails { program.copy(phases=listOf(phase.copy(secondaryLimit=phase.secondaryLimit?.copy(minimum=2.5f)))).validate() }
        assertFails { program.copy(phases=listOf(phase.copy(secondaryLimit=phase.secondaryLimit?.copy(metric=BrewMetric.PumpPressure)))).validate() }
        assertEquals(program,Json.decodeFromString<BrewProgram>(Json.encodeToString(BrewProgram.serializer(),program)))
    }
    @Test fun sourceMetadataAndOriginalRecordingRoundTrip() {
        val recording=FreeHandRecording(FreeHandControlMode.Flow,listOf(FreeHandSample(0,BrewDataPoint(1f,0f,0f,2f,0f))))
        val program=BrewProgram.Recording(recording,version=2,originalRecording=recording,replayPolicy=ReplayPolicy.Hold)
        program.validate()
        assertEquals(program,Json.decodeFromString<BrewProgram>(kotlinx.serialization.json.Json.encodeToString(BrewProgram.serializer(),program)))
        assertFails { program.copy(version=1).validate() }
    }
}
