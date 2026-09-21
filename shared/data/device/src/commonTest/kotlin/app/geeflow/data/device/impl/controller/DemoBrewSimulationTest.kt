package app.geeflow.data.device.impl.controller

import app.geeflow.data.brew.model.PhaseControl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DemoBrewSimulationTest {
    @Test
    fun `pump approaches targets gradually and releases pressure during a pause`() {
        val simulation = DemoBrewSimulation()
        val initial = simulation.advance(PhaseControl.Pressure(9f), .1f)
        assertTrue(initial.pressure in 0f..3f)
        var running = initial
        repeat(100) { running = simulation.advance(PhaseControl.Pressure(9f), .1f) }
        assertTrue(running.pressure in 8.8f..9.2f)
        val pause = simulation.advance(PhaseControl.PumpPause, .1f)
        assertTrue(pause.pressure > 0f && pause.pressure < running.pressure)
        var resting = pause
        repeat(50) { resting = simulation.advance(PhaseControl.PumpPause, .1f) }
        assertTrue(resting.pressure < .01f && resting.flowRate < .01f)
        assertTrue(resting.weight >= pause.weight)
    }

    @Test
    fun `cup flow starts after wetting and totals integrate the reported flows`() {
        val simulation = DemoBrewSimulation()
        var volume = 0f
        var weight = 0f
        var drySamples = 0
        repeat(300) {
            val point = simulation.advance(PhaseControl.Flow(3f), .1f)
            if (point.volume > 0f && point.weight == 0f) drySamples++
            volume += point.flowRate * .1f
            weight += point.weightRate * .1f
            assertEquals(volume, point.volume, .001f)
            assertEquals(weight, point.weight, .001f)
            assertTrue(point.weight <= point.volume)
        }
        assertTrue(drySamples > 10)
        assertTrue(weight > 40f)
    }

    @Test
    fun `flow mode respects pump limits and remains repeatable`() {
        val first = DemoBrewSimulation()
        val second = DemoBrewSimulation()
        repeat(300) {
            val point = first.advance(PhaseControl.Flow(8f), .1f)
            assertEquals(point, second.advance(PhaseControl.Flow(8f), .1f))
            assertTrue(point.pressure in 0f..12f)
            assertTrue(point.flowRate in 0f..8f)
        }
    }
}
