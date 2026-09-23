package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.DeviceState.BrewStatus
import app.geeflow.data.device.model.DeviceState.ConnectionStatus
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Instant

class FinalBrewTelemetryTest {
    private val stamp = Instant.fromEpochMilliseconds(1000)
    private val stopped = DeviceState(
        connectionStatus = ConnectionStatus.Connected,
        brewStatus = BrewStatus.Idle,
        telemetryTime = stamp,
        weight = 59f,
        volume = 88f,
    )

    @Test
    fun `status before telemetry captures the final totals without adding a chart tail`() = runTest {
        val states = MutableStateFlow(stopped)
        val pending = async(start = CoroutineStart.UNDISPATCHED) {
            states.awaitFinalBrewTelemetry(stopped, stamp)
        }
        assertFalse(pending.isCompleted)
        states.value = stopped.copy(
            telemetryTime = Instant.fromEpochMilliseconds(1200),
            weight = 60f,
            volume = 89f,
            pressure = 0f,
            flowRate = 0f,
        )
        val recorded = mapOf(17f to BrewDataPoint(7f, 59f, 88f, 6f, 6.5f))
        val final = recorded.mapValues { (_, point) -> point.withFinalTotals(pending.await()) }
        assertEquals(recorded.keys, final.keys)
        assertEquals(BrewDataPoint(7f, 60f, 89f, 6f, 6.5f), final.getValue(17f))
        states.value = states.value.copy(weight = 65f)
        assertEquals(60f, final.getValue(17f).weight)
    }

    @Test
    fun `a fresh measurement in the stop update finishes immediately`() = runTest {
        val final = stopped.copy(telemetryTime = Instant.fromEpochMilliseconds(1200), weight = 60f)
        val states = MutableStateFlow(final)
        assertEquals(final, states.awaitFinalBrewTelemetry(final, stamp))
    }

    @Test
    fun `a conflated measurement before the stop still waits for terminal telemetry`() = runTest {
        val stop = stopped.copy(
            telemetryTime = Instant.fromEpochMilliseconds(1100),
            statusTime = Instant.fromEpochMilliseconds(1200),
        )
        val states = MutableStateFlow(stop)
        val pending = async(start = CoroutineStart.UNDISPATCHED) {
            states.awaitFinalBrewTelemetry(stop, stamp)
        }
        assertFalse(pending.isCompleted)
        states.value = stop.copy(telemetryTime = Instant.fromEpochMilliseconds(1300), weight = 60f)
        assertEquals(60f, pending.await().weight)
    }

    @Test
    fun `atomic final telemetry and status finish without another poll`() = runTest {
        val final = stopped.copy(statusTime = stamp, weight = 60f)
        val states = MutableStateFlow(final)
        assertEquals(final, states.awaitFinalBrewTelemetry(final, stamp))
    }

    @Test
    fun `a new status frame alone is not a final measurement`() = runTest {
        val states = MutableStateFlow(stopped)
        val pending = async(start = CoroutineStart.UNDISPATCHED) {
            states.awaitFinalBrewTelemetry(stopped, stamp)
        }
        states.value = stopped.copy(statusTime = Instant.fromEpochMilliseconds(1200))
        assertFalse(pending.isCompleted)
        states.value = stopped.copy(telemetryTime = Instant.fromEpochMilliseconds(1300), weight = 60f)
        assertEquals(60f, pending.await().weight)
    }

    @Test
    fun `missing telemetry times and active brewing do not wait`() = runTest {
        val states = MutableStateFlow(stopped)
        assertEquals(stopped, states.awaitFinalBrewTelemetry(stopped, null))
        val brewing = stopped.copy(brewStatus = BrewStatus.Profile)
        assertEquals(brewing, states.awaitFinalBrewTelemetry(brewing, stamp))
    }

    @Test
    fun `missing final frame falls back to the recorded totals`() = runTest {
        val states = MutableStateFlow(stopped)
        assertEquals(stopped, states.awaitFinalBrewTelemetry(stopped, stamp))
    }

    @Test
    fun `disconnection or a new brew cannot supply totals to the previous brew`() = runTest {
        val interrupted = listOf(
            stopped.copy(connectionStatus = ConnectionStatus.Disconnected),
            stopped.copy(brewStatus = BrewStatus.Profile, weight = 100f),
        )
        for (state in interrupted) {
            val states = MutableStateFlow(stopped)
            val pending = async(start = CoroutineStart.UNDISPATCHED) {
                states.awaitFinalBrewTelemetry(stopped, stamp)
            }
            states.value = state
            assertEquals(stopped, pending.await())
        }
    }

    @Test
    fun `a manual stop below the goal does not invent a reached target`() = runTest {
        val states = MutableStateFlow(stopped)
        val pending = async(start = CoroutineStart.UNDISPATCHED) {
            states.awaitFinalBrewTelemetry(stopped, stamp)
        }
        states.value = stopped.copy(telemetryTime = Instant.fromEpochMilliseconds(1200), weight = 59f)
        assertEquals(59f, pending.await().weight)
    }
}
