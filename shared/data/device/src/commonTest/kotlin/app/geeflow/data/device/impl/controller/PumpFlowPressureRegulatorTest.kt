package app.geeflow.data.device.impl.controller

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PumpFlowPressureRegulatorTest {
    @Test
    fun tracksFlowWithLagAndChangingResistance() {
        val regulator = PumpFlowPressureRegulator(maximumPressure = 6f)
        var flow = 0f
        var previous = 0f
        repeat(600) { tick ->
            val pressure = regulator.update(targetFlow = 3f, measuredFlow = flow, elapsedMillis = tick * 200L)
            assertTrue(pressure in 0f..6f)
            assertTrue(pressure - previous <= 0.101f)
            val conductance = if (tick < 300) 2f else 1f
            flow += (pressure * conductance - flow) * 0.2f
            previous = pressure
            if (tick == 299 || tick == 599) assertTrue(abs(flow - 3f) < 0.6f)
        }
    }

    @Test
    fun clampsPressureWithoutWindupAndZeroStopsImmediately() {
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
    fun delayedMeasurementsCannotCreateLargePressureJump() {
        val regulator = PumpFlowPressureRegulator(maximumPressure = 6f)
        regulator.update(targetFlow = 8f, measuredFlow = 0f, elapsedMillis = 0)
        assertEquals(0.5f, regulator.update(targetFlow = 8f, measuredFlow = 0f, elapsedMillis = 30000))
        assertFailsWith<IllegalArgumentException> {
            regulator.update(targetFlow = 8f, measuredFlow = Float.NaN, elapsedMillis = 31000)
        }
    }
}
