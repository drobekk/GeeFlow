package app.geeflow.presentation.feature.device.dashboard.profiles

import app.geeflow.data.brew.model.BrewPhase
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.brew.model.FreeHandRecording
import app.geeflow.data.brew.model.FreeHandSample
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.presentation.feature.device.dashboard.model.ChartData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfilePreviewTargetTest {
    @Test
    fun `pressure to flow transition leaves only one target channel at each time`() {
        val profile = profile(
            program = BrewProgram.Phases(
                listOf(
                    BrewPhase(id = "pressure", control = PhaseControl.Pressure(4f), maximumDurationMillis = 1_000),
                    BrewPhase(id = "flow", control = PhaseControl.Flow(2.5f), maximumDurationMillis = 1_000),
                ),
            ),
            targetData = mapOf(
                0f to ChartData(pressure = 4f, weight = 0f, weightPerSecond = 0f, volume = 0f, volumePerSecond = 0f),
                1f to ChartData(pressure = 0f, weight = 0f, weightPerSecond = 0f, volume = 0f, volumePerSecond = 2.5f),
            ),
        )

        val targets = profile.previewTargets()

        assertEquals(4f, targets[0].pressure)
        assertNull(targets[0].waterFlow)
        assertNull(targets[1].pressure)
        assertEquals(2.5f, targets[1].waterFlow)
    }

    @Test
    fun `recorded flow preview ignores measured pressure channel`() {
        val profile = profile(
            program = BrewProgram.Recording(
                FreeHandRecording(
                    controlMode = FreeHandControlMode.Flow,
                    samples = listOf(FreeHandSample(0, BrewDataPoint(pressure = 7f, weight = 0f, volume = 0f, flowRate = 3f, weightRate = 0f))),
                ),
            ),
            targetData = mapOf(0f to ChartData(pressure = 7f, weight = 0f, weightPerSecond = 0f, volume = 0f, volumePerSecond = 3f)),
        )

        val target = profile.previewTargets().single()

        assertNull(target.pressure)
        assertEquals(3f, target.waterFlow)
    }

    private fun profile(
        program: BrewProgram,
        targetData: Map<Float, ChartData>,
    ) = ProfileListViewState.Profile(
        id = "1",
        number = "1",
        name = "Test",
        description = "",
        brewByWeight = false,
        program = program,
        targetData = targetData,
    )
}
