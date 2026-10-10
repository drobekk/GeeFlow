package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.ble.modbus.ModbusSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class WendougeePulseTest {
    @Test
    fun `when a pulse is acknowledged then one press and one release are sent`() = runTest {
        val notifications = MutableSharedFlow<ByteArray>()
        val writes = mutableListOf<ByteArray>()
        val session = session(notifications) { request ->
            writes += request
            notifications.emit(request)
        }

        session.pulseCoil(158)

        assertEquals(listOf(0xFF, 0), writes.map { it[4].toInt() and 0xFF })
        assertTrue(writes.all { it[3].toInt() and 0xFF == 158 })
    }

    @Test
    fun `when a press acknowledgement is missing then the coil is still released without repeating the press`() = runTest {
        val notifications = MutableSharedFlow<ByteArray>()
        val writes = mutableListOf<ByteArray>()
        val session = session(notifications) { request ->
            writes += request
            if (request[4] == 0.toByte()) notifications.emit(request)
        }

        val result = runCatching { session.pulseCoil(159) }

        assertIs<TimeoutCancellationException>(result.exceptionOrNull())
        assertEquals(listOf(0xFF, 0), writes.map { it[4].toInt() and 0xFF })
    }

    @Test
    fun `when a pulse is cancelled after pressing then the coil is still released`() = runTest {
        val notifications = MutableSharedFlow<ByteArray>()
        val writes = mutableListOf<ByteArray>()
        val session = session(notifications) { request ->
            writes += request
            notifications.emit(request)
        }

        val job = launch { session.pulseCoil(158) }
        runCurrent()
        job.cancelAndJoin()

        assertTrue(job.isCancelled)
        assertEquals(listOf(0xFF, 0), writes.map { it[4].toInt() and 0xFF })
    }

    @Test
    fun `when the press and release both fail then the original failure is preserved`() = runTest {
        val notifications = MutableSharedFlow<ByteArray>()
        val pressFailure = IllegalStateException("Press transport failed")
        val releaseFailure = IllegalStateException("Release transport failed")
        val writes = mutableListOf<ByteArray>()
        val session = session(notifications) { request ->
            writes += request
            throw if (request[4] == 0.toByte()) releaseFailure else pressFailure
        }

        val result = runCatching { session.pulseCoil(158) }

        val failure = assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(pressFailure.message, failure.message)
        assertEquals(listOf(releaseFailure.message), failure.suppressedExceptions.map { it.message })
        assertEquals(listOf(0xFF, 0), writes.map { it[4].toInt() and 0xFF })
    }

    @Test
    fun `when only the release fails then its failure is returned`() = runTest {
        val notifications = MutableSharedFlow<ByteArray>()
        val releaseFailure = IllegalStateException("Release transport failed")
        val session = session(notifications) { request ->
            if (request[4] == 0.toByte()) throw releaseFailure
            notifications.emit(request)
        }

        val result = runCatching { session.pulseCoil(158) }

        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(releaseFailure.message, result.exceptionOrNull()?.message)
    }

    private fun session(notifications: MutableSharedFlow<ByteArray>, write: suspend (ByteArray) -> Unit): ModbusSession =
        ModbusSession(notifications, write, 1, 1000.milliseconds)
}
