package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.device.model.DeviceState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class FreeHandRecorderTest {
    private val start = Instant.fromEpochMilliseconds(1000)
    private fun state(time: Long, volume: Float = 5f) = DeviceState(
        telemetryTime = Instant.fromEpochMilliseconds(time),
        brewStatus = DeviceState.BrewStatus.FreeVariable,
        pressure = 6f,
        flowRate = 4f,
        volume = volume,
    )

    @Test
    fun ignoresStaleTelemetryAndChartTicksButKeepsUnchangedNewMeasurements() {
        val recorder = FreeHandRecorder(start, FreeHandControlMode.Flow)
        recorder.capture(state(900))
        assertNull(recorder.snapshot())
        recorder.capture(state(1100))
        recorder.capture(state(1100))
        recorder.capture(state(1600))
        val recording = assertNotNull(recorder.snapshot())
        assertEquals(FreeHandControlMode.Flow, recording.controlMode)
        assertEquals(listOf(0L, 500L), recording.samples.map { it.elapsedMillis })
    }

    @Test
    fun stopRequestFreezesBeforeDelayedIdleAndPostBrewTail() {
        val recorder = FreeHandRecorder(start, FreeHandControlMode.Pressure)
        recorder.capture(state(1100, 120f))
        recorder.capture(state(1200, 126f).copy(freeHandStopRequested = true))
        recorder.capture(state(1400, 130f))
        recorder.capture(state(1600, 132f).copy(brewStatus = DeviceState.BrewStatus.Idle))
        assertEquals(listOf(120f), assertNotNull(recorder.snapshot()).samples.map { it.data.volume })
    }

    @Test
    fun machineStopAlsoFreezesRecording() {
        val recorder = FreeHandRecorder(start, FreeHandControlMode.Flow)
        recorder.capture(state(1100))
        recorder.capture(state(1600).copy(brewStatus = DeviceState.BrewStatus.Idle))
        recorder.capture(state(2100))
        assertEquals(1, assertNotNull(recorder.snapshot()).samples.size)
    }
}
