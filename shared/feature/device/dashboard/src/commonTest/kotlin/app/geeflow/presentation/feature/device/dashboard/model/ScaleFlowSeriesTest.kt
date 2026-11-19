package app.geeflow.presentation.feature.device.dashboard.model

import app.geeflow.data.brew.model.BrewScaleSample
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScaleFlowSeriesTest {
    @Test
    fun `older recordings use one second regression from timestamped weight`() {
        val samples = (0..8).map { index -> scaleSample(index * 250L, index * 0.25f) }

        val flows = oneSecondScaleFlowSeries(samples)

        assertTrue(flows.isNotEmpty())
        assertTrue(flows.all { (_, flow) -> kotlin.math.abs(flow - 1f) < 0.001f })
        assertTrue(flows.first().first.elapsedMillis >= 500L)
    }

    @Test
    fun `saved regression takes precedence over a recalculated value`() {
        val samples = (0..4).map { index ->
            scaleSample(index * 250L, index * 0.25f).copy(smoothedFlowGramsPerSecond = 1.7f)
        }

        assertEquals(1.7f, oneSecondScaleFlowSeries(samples).last().second)
    }

    @Test
    fun `raw adjacent derivative is not used before regression window is ready`() {
        val samples = listOf(scaleSample(0L, 0f), scaleSample(250L, 2f))

        assertTrue(oneSecondScaleFlowSeries(samples).isEmpty())
    }

    private fun scaleSample(elapsedMillis: Long, weight: Float) = BrewScaleSample(
        elapsedMillis = elapsedMillis,
        receivedAtEpochMillis = elapsedMillis,
        weightGrams = weight,
        derivedFlowGramsPerSecond = 8f,
        rawPacketHex = "",
        decoded = true,
    )
}
