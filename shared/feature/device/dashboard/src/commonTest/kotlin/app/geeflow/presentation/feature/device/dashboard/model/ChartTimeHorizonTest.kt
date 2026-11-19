package app.geeflow.presentation.feature.device.dashboard.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ChartTimeHorizonTest {
    @Test
    fun beginsAtFiveSecondsThenKeepsLatestDataAtNinetyThreePercent() {
        assertEquals(5.0, liveChartTimeHorizon(0.0))
        assertEquals(5.0, liveChartTimeHorizon(4.65))
        assertEquals(5.0, liveChartTimeHorizon(4.0))
        assertEquals(5.0 / .93, liveChartTimeHorizon(5.0))
        assertEquals(20.0 / .93, liveChartTimeHorizon(20.0))
        assertEquals(139.0 / .93, liveChartTimeHorizon(139.0))
    }

    @Test
    fun invalidElapsedTimeUsesTheInitialWindow() {
        assertEquals(5.0, liveChartTimeHorizon(Double.NaN))
    }

    @Test
    fun zeroTimePlaceholderDoesNotCountAsARecordedTimeline() {
        assertEquals(false, hasElapsedChartMeasurements(listOf(0f)))
        assertEquals(true, hasElapsedChartMeasurements(listOf(0f, 0.1f)))
    }
}
