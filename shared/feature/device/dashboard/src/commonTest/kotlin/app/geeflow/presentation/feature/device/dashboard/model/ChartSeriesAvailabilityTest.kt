package app.geeflow.presentation.feature.device.dashboard.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChartSeriesAvailabilityTest {
    @Test
    fun zeroFilledCupFlowDoesNotImplyARecordedNativeSeries() {
        fun point(flow: Float) = ChartData(3f, 0f, flow, 2f, 1f)
        assertFalse(hasRecordedNativeCupFlow(listOf(point(0f), point(0f))))
        assertTrue(hasRecordedNativeCupFlow(listOf(point(0f), point(1.2f), point(0f))))
    }
}
