package app.geeflow.app.profilesync

import app.geeflow.data.brew.model.AdaptiveFlowProbe
import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewPhase
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.ConditionOperator
import app.geeflow.data.brew.model.ExitCondition
import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.brew.model.FreeHandRecording
import app.geeflow.data.brew.model.FreeHandSample
import app.geeflow.data.brew.model.MeasurementScope
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.PhaseRamp
import app.geeflow.data.brew.model.PressureLocation
import app.geeflow.data.brew.model.RampStart
import app.geeflow.data.brew.model.RampStyle
import app.geeflow.data.brew.model.SingleDoseSettings
import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.ThresholdComparison
import app.geeflow.data.brew.model.validate
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ProfileLibraryRoundTripTest {
    @Test
    fun `cloud catalog profile fields decode and become complete local profiles`() {
        val phaseProgram = BrewProgram.Phases(
            phases = listOf(
                BrewPhase(
                    id = "pressure-start",
                    name = "Preinfusion",
                    control = PhaseControl.Pressure(3.5f, PressureLocation.Group),
                    maximumDurationMillis = 5000,
                    minimumDurationMillis = 1000,
                    ramp = PhaseRamp(RampStyle.Linear, 4000, RampStart.CurrentMeasurement),
                    exitConditions = listOf(
                        ExitCondition(BrewMetric.PumpedVolume, ThresholdComparison.Above, 4f, MeasurementScope.Total),
                    ),
                    conditionOperator = ConditionOperator.And,
                ),
                BrewPhase(
                    id = "adaptive-flow",
                    name = "Adaptive finish",
                    control = PhaseControl.Flow(4.2f),
                    maximumDurationMillis = 8000,
                    ramp = PhaseRamp(RampStyle.Linear, 4000, RampStart.PreviousTarget),
                    exitConditions = listOf(
                        ExitCondition(BrewMetric.CupWeight, ThresholdComparison.Above, 36f, MeasurementScope.Total),
                    ),
                    adaptiveFlowProbe = AdaptiveFlowProbe(2500, 9f, 0.2f),
                ),
            ),
        )
        val phaseProfile = PortableBrewProfile(
            name = "Cloud pressure profile",
            description = "All portable settings round-trip.",
            finishCondition = Condition.Weight(36f),
            autoLinkOpen = true,
            singleDoseSettings = SingleDoseSettings(enabled = true, grindingSize = 440, grindingSpeed = 720),
            program = phaseProgram,
        )
        val recordingProgram = BrewProgram.Recording(
            FreeHandRecording(
                controlMode = FreeHandControlMode.Flow,
                samples = listOf(
                    FreeHandSample(0, BrewDataPoint(0.5f, 0f, 0f, 0f, 0f)),
                    FreeHandSample(750, BrewDataPoint(2.5f, 1.2f, 3f, 1f, 0.6f)),
                ),
            ),
        )
        val recordingProfile = PortableBrewProfile(
            name = "Cloud recording",
            description = "Recorded telemetry stays intact.",
            finishCondition = Condition.Volume(42f),
            autoLinkOpen = false,
            singleDoseSettings = SingleDoseSettings(),
            program = recordingProgram,
        )
        val catalog = ProfileLibraryCatalog(
            revision = 18,
            profiles = listOf(
                ProfileLibraryRecord("550e8400-e29b-41d4-a716-446655440001", 4, true, false, phaseProfile),
                ProfileLibraryRecord("550e8400-e29b-41d4-a716-446655440002", 2, false, false, recordingProfile),
            ),
        )
        val json = Json { classDiscriminator = "type" }

        val decoded = json.decodeFromString<ProfileLibraryCatalog>(json.encodeToString(catalog))
        assertEquals(catalog, decoded)

        val phaseRecord = decoded.profiles.first()
        val localPhaseProfile = phaseRecord.profile.toLocalProfile(userId = 7, position = 2)
        assertEquals(phaseProfile.name, localPhaseProfile.name)
        assertEquals(phaseProfile.description, localPhaseProfile.description)
        assertEquals(phaseProfile.finishCondition, localPhaseProfile.finishCondition)
        assertEquals(phaseProfile.autoLinkOpen, localPhaseProfile.autoLinkOpen)
        assertEquals(phaseProfile.singleDoseSettings, localPhaseProfile.singleDoseSettings)
        assertEquals(phaseProgram, assertIs<BrewProgram.Phases>(localPhaseProfile.program))
        localPhaseProfile.program.validate()

        val recordingRecord = decoded.profiles.last()
        val localRecordingProfile = recordingRecord.profile.toLocalProfile(userId = 7, position = 3)
        assertEquals(recordingProfile.finishCondition, localRecordingProfile.finishCondition)
        assertEquals(recordingProfile.singleDoseSettings, localRecordingProfile.singleDoseSettings)
        assertEquals(recordingProgram, assertIs<BrewProgram.Recording>(localRecordingProfile.program))
        localRecordingProfile.program.validate()
    }
}
