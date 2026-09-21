package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.device.model.DeviceState
import kotlin.test.Test
import kotlin.test.assertEquals

class BrewDataMappingTest {
    private val previous = BrewDataPoint(pressure = 6f, weight = 35f, volume = 39f, flowRate = 2f, weightRate = 1f)

    @Test
    fun `terminal measurements preserve the sample that reaches the goal`() {
        val point = previous.withFinalTotals(DeviceState(volume = 40f, weight = 36f))
        assertEquals(40f, point.volume)
        assertEquals(36f, point.weight)
        assertEquals(previous.pressure, point.pressure)
    }

    @Test
    fun `cleared or missing terminal counters do not erase recorded totals`() {
        assertEquals(previous, previous.withFinalTotals(DeviceState(volume = 0f, weight = 0f)))
        assertEquals(previous, previous.withFinalTotals(DeviceState()))
    }
}
