package app.geeflow.data.device.impl.controller

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PumpFlowPressureRegulatorTest {
    @Test
    fun `pressure corrections ease when rising flow is already approaching the target`() {
        val regulator = PumpFlowPressureRegulator(maximumPressure = 12f)
        regulator.update(targetFlow = 4f, measuredFlow = 0f, elapsedMillis = 0)
        val first = regulator.update(targetFlow = 4f, measuredFlow = 0f, elapsedMillis = 100)
        val approaching = regulator.update(targetFlow = 4f, measuredFlow = 1f, elapsedMillis = 200)
        assertTrue(approaching > first)
        assertTrue(approaching - first < first / 2f)

        val stalled = regulator.update(targetFlow = 4f, measuredFlow = 1f, elapsedMillis = 300)
        assertTrue(stalled - approaching > approaching - first)
    }

    @Test
    fun `falling flow reduces downward corrections but overshoot gets a full response`() {
        val regulator = PumpFlowPressureRegulator(maximumPressure = 12f)
        regulator.reset(pressure = 6f)
        regulator.update(targetFlow = 2f, measuredFlow = 6f, elapsedMillis = 0)
        val first = regulator.update(targetFlow = 2f, measuredFlow = 6f, elapsedMillis = 100)
        val approaching = regulator.update(targetFlow = 2f, measuredFlow = 5f, elapsedMillis = 200)
        assertTrue(first - approaching < (6f - first) / 2f)

        val overshot = regulator.update(targetFlow = 2f, measuredFlow = 0f, elapsedMillis = 300)
        assertTrue(overshot - approaching > first - approaching)
    }

    @Test
    fun `a changed target or delayed sample does not inherit trend damping`() {
        val regulator = PumpFlowPressureRegulator(maximumPressure = 12f)
        regulator.update(targetFlow = 4f, measuredFlow = 0f, elapsedMillis = 0)
        val first = regulator.update(targetFlow = 4f, measuredFlow = 0f, elapsedMillis = 100)
        val changed = regulator.update(targetFlow = 6f, measuredFlow = 1f, elapsedMillis = 200)
        assertEquals(0.2f, changed - first, absoluteTolerance = 0.001f)
        val delayed = regulator.update(targetFlow = 6f, measuredFlow = 2f, elapsedMillis = 2200)
        assertEquals(0.5f, delayed - changed, absoluteTolerance = 0.001f)
        regulator.reset(pressure = 3f)
        assertEquals(3f, regulator.update(targetFlow = 6f, measuredFlow = 3f, elapsedMillis = 2300))
    }

    @Test
    fun `flow follows the target with lag and changing resistance`() {
        val regulator = PumpFlowPressureRegulator(maximumPressure = 6f)
        var flow = 0f
        var previous = 0f
        repeat(600) { tick ->
            val pressure = regulator.update(targetFlow = 3f, measuredFlow = flow, elapsedMillis = tick * 200L)
            assertTrue(pressure in 0f..6f)
            assertTrue(pressure - previous <= 0.401f)
            val conductance = if (tick < 300) 2f else 1f
            flow += (pressure * conductance - flow) * 0.2f
            previous = pressure
            if (tick == 299 || tick == 599) assertTrue(abs(flow - 3f) < 0.6f)
        }
    }

    @Test
    fun `flow reaches the target promptly at different sampling rates`() {
        for (interval in listOf(100L, 200L)) {
            val regulator = PumpFlowPressureRegulator(maximumPressure = 12f)
            var flow = 0f
            var peakFlow = 0f
            val seconds = interval / 1000f
            for (elapsed in 0L..6000L step interval) {
                val pressure = regulator.update(targetFlow = 4f, measuredFlow = flow, elapsedMillis = elapsed)
                flow += (pressure * 2f - flow) * seconds
                peakFlow = maxOf(peakFlow, flow)
            }
            assertTrue(abs(flow - 4f) < 0.6f, "Flow $flow with ${interval}ms samples")
            assertTrue(peakFlow < 5f, "Overshoot $peakFlow with ${interval}ms samples")
        }
    }

    @Test
    fun `pressure stays within limits and a zero target stops immediately`() {
        val regulator = PumpFlowPressureRegulator(maximumPressure = 6f)
        var pressure = 0f
        repeat(100) { tick ->
            pressure = regulator.update(targetFlow = 8f, measuredFlow = 0f, elapsedMillis = tick * 1000L)
        }
        assertEquals(6f, pressure)
        assertTrue(regulator.update(targetFlow = 1f, measuredFlow = 8f, elapsedMillis = 100000) < 6f)
        assertEquals(0f, regulator.update(targetFlow = 0f, measuredFlow = 8f, elapsedMillis = 100001))
    }

    @Test
    fun `delayed measurements cannot create a large pressure jump`() {
        val regulator = PumpFlowPressureRegulator(maximumPressure = 6f)
        regulator.update(targetFlow = 8f, measuredFlow = 0f, elapsedMillis = 0)
        assertEquals(0.5f, regulator.update(targetFlow = 8f, measuredFlow = 0f, elapsedMillis = 30000))
        assertFailsWith<IllegalArgumentException> {
            regulator.update(targetFlow = 8f, measuredFlow = Float.NaN, elapsedMillis = 31000)
        }
    }
}
