package app.geeflow.presentation.feature.device.dashboard.profiles

import app.geeflow.data.brew.model.BrewPhase
import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.brew.model.FreeHandRecording
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.PhaseControl
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileControlKindTest {
    @Test
    fun classifiesPressureFlowMixedRecordedAndPauseOnlyPrograms() {
        assertEquals(ProfileControlKind.Pressure, program(PhaseControl.Pressure(9f)).controlKind())
        assertEquals(ProfileControlKind.Flow, program(PhaseControl.Flow(3f)).controlKind())
        assertEquals(
            ProfileControlKind.Mixed,
            program(PhaseControl.Flow(1.5f), PhaseControl.Pressure(9f)).controlKind(),
        )
        assertEquals(
            ProfileControlKind.Recorded,
            BrewProgram.Recording(FreeHandRecording(FreeHandControlMode.Flow, emptyList())).controlKind(),
        )
    }

    @Test
    fun pauseOnlyProgramsAreOther() {
        assertEquals(ProfileControlKind.Other, program(PhaseControl.PumpPause).controlKind())
    }

    private fun program(vararg controls: PhaseControl) = BrewProgram.Phases(
        controls.mapIndexed { index, control ->
            BrewPhase(
                id = index.toString(),
                control = control,
                maximumDurationMillis = 1_000,
            )
        },
    )
}
