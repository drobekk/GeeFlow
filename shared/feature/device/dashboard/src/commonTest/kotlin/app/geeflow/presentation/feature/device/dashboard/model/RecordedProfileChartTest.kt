package app.geeflow.presentation.feature.device.dashboard.model

import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.brew.model.FreeHandRecording
import app.geeflow.data.brew.model.FreeHandSample
import kotlin.test.Test
import kotlin.test.assertEquals

class RecordedProfileChartTest {
    @Test
    fun chartIncludesEveryChannelAndEndingWithoutTransportPaddingOrTargetSpike() {
        val recording = FreeHandRecording(
            FreeHandControlMode.Flow,
            List(77) {
                FreeHandSample(it * 500L, BrewDataPoint(12.9f, 0f, it * 3f, 9f, 0f))
            }
        )
        val profile = BrewProfile(
            userId = 1,
            name = "Flow",
            description = "",

            finishCondition = Condition.Volume(228f),
            program = BrewProgram.Recording(recording)
        )
        val chart = profile.toTargetData()
        assertEquals(77, chart.size)
        assertEquals(38f, chart.keys.max())
        assertEquals(12.9f, chart.getValue(38f).pressure)
        assertEquals(228f, chart.getValue(38f).volume)
        assertEquals(9f, chart.getValue(38f).volumePerSecond)
        assertEquals(chart, profile.copy(finishCondition = Condition.Weight(235f)).toTargetData())
        assertEquals(setOf(0f), chart.values.map { it.weight }.toSet())
    }
}
