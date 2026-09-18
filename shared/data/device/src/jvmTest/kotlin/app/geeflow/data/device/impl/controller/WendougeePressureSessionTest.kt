package app.geeflow.data.device.impl.controller

import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.pumpTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class WendougeePressureSessionTest {

    @Test
    fun mixedTargetsUseOnlyPressureAndIgnoreRepeatedTelemetry() = runBlocking {
        val device = MutableStateFlow(DeviceState(pressure = 1f, flowRate = 0f, telemetryTime = Clock.System.now()))
        val writes = mutableListOf<Float>()
        val controller = object : DeviceController by DemoDeviceController(CoroutineScope(Dispatchers.Default)) {
            override val deviceState = device
            override fun telemetry() = device.value.pumpTelemetry()
            override suspend fun setFreeBrewPressureTarget(pressure: Float) {
                writes += pressure
            }
            override suspend fun setFreeBrewFlowTarget(flow: Float) {
                error("Hardware mode must remain pressure")
            }
        }
        val session = WendougeePressureSession(controller = controller)
        session.applyTarget(PhaseControl.Pressure(1f))
        session.applyTarget(PhaseControl.Flow(4f))
        delay(300)
        session.applyTarget(PhaseControl.Flow(4f))
        assertEquals(listOf(1f), writes)
        device.update { it.copy(telemetryTime = Clock.System.now()) }
        val corrected = session.applyTarget(PhaseControl.Flow(4f))
        assertTrue((corrected as PhaseControl.Pressure).bar > 1f)
        session.applyTarget(PhaseControl.PumpPause)
        assertEquals(0f, writes.last())
        session.applyTarget(PhaseControl.Pressure(3f))
        assertEquals(3f, writes.last())
        session.applyTarget(PhaseControl.Pressure(12f))
        assertEquals(PhaseControl.Pressure(12f), session.applyTarget(PhaseControl.Flow(4f)))
        assertEquals(PhaseControl.Pressure(12f), session.applyTarget(PhaseControl.Flow(4f)))
        device.update { it.copy(telemetryTime = Clock.System.now() - 3.seconds) }
        assertFailsWith<IllegalStateException> { session.applyTarget(PhaseControl.Flow(4f)) }
        assertEquals(12f, writes.last())
    }
}
