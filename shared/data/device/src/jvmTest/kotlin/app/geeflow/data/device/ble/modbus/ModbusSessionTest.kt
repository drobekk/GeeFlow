@file:OptIn(ExperimentalCoroutinesApi::class)

package app.geeflow.data.device.ble.modbus

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ModbusSessionTest {
    @Test
    fun `when read receives wrong unit count and CRC then waits for the complete correct response`() = runTest {
        val notifications = MutableSharedFlow<ByteArray>()
        val reply = frame(1, 3, 2, 0, 7)
        val session = ModbusSession(notifications, {
            notifications.emit(frame(2, 3, 2, 0, 99))
            notifications.emit(frame(1, 3, 4, 0, 88, 0, 77))
            notifications.emit(reply.copyOf().also { corrupted -> corrupted[corrupted.lastIndex] = 0 })
            notifications.emit(reply.copyOfRange(0, 4))
            notifications.emit(reply.copyOfRange(4, reply.size))
        }, 1, 1.seconds)

        val result = session.readHoldingRegisters(396, 1)

        assertEquals(listOf(7), result)
    }

    @Test
    fun `when write receives a stale value echo then waits for its own value`() = runTest {
        val notifications = MutableSharedFlow<ByteArray>()
        val writes = mutableListOf<ByteArray>()
        val session = ModbusSession(notifications, { request ->
            writes += request
        }, 1, 1.seconds)

        val pending = async { session.writeSingleRegister(396, 1) }
        runCurrent()
        notifications.emit(ModbusFrame.writeSingleRegister(1, 396, 0))
        runCurrent()
        val ignoredStaleValue = !pending.isCompleted
        notifications.emit(writes.single())
        pending.await()

        assertTrue(ignoredStaleValue)
        assertEquals(1, writes.size)
        assertEquals(1, writes.single()[5].toInt())
    }

    @Test
    fun `when multiple write receives another count then does not accept that acknowledgement`() = runTest {
        val notifications = MutableSharedFlow<ByteArray>()
        val session = ModbusSession(notifications, {}, 1, 1.seconds)

        val pending = async { session.writeMultipleRegisters(396, listOf(1)) }
        runCurrent()
        notifications.emit(frame(1, 16, 1, 140, 0, 2))
        runCurrent()
        val ignoredWrongCount = !pending.isCompleted
        notifications.emit(frame(1, 16, 1, 140, 0, 1))
        pending.await()

        assertTrue(ignoredWrongCount)
    }

    @Test
    fun `when raw coil request receives protocol exception then propagates the exception`() = runTest {
        val notifications = MutableSharedFlow<ByteArray>()
        val session = ModbusSession(notifications, { notifications.emit(frame(1, 133, 2)) }, 1, 1.seconds)
        val request = ModbusFrame.writeSingleCoil(1, 154, true)

        val result = runCatching { session.sendAndAwaitFc(request, 5) }

        assertIs<ModbusProtocolException>(result.exceptionOrNull())
    }

    @Test
    fun `when prefix read receives protocol exception then propagates it instead of waiting for timeout`() = runTest {
        val notifications = MutableSharedFlow<ByteArray>()
        val session = ModbusSession(notifications, { notifications.emit(frame(1, 131, 2)) }, 1, 1.seconds)
        val request = ModbusFrame.readHoldingRegisters(1, 1404, 20)

        val result = runCatching { session.sendAndAwaitPrefix(request, byteArrayOf(1, 3, 40)) }

        assertIs<ModbusProtocolException>(result.exceptionOrNull())
    }

    @Test
    fun `when timed out response arrives during recovery then discards it before the next request`() = runTest {
        val notifications = MutableSharedFlow<ByteArray>()
        val writes = mutableListOf<ByteArray>()
        val session = ModbusSession(notifications, { request ->
            writes += request
            if (writes.size == 2) notifications.emit(frame(1, 3, 2, 0, 8))
        }, 1, 1.seconds)
        val first = async { runCatching { session.readHoldingRegisters(396, 1) } }

        runCurrent()
        advanceTimeBy(1000)
        runCurrent()
        val firstResult = first.await()
        val second = async { session.readHoldingRegisters(396, 1) }
        runCurrent()
        advanceTimeBy(100)
        notifications.emit(frame(1, 3, 2, 0, 99))
        advanceTimeBy(199)
        runCurrent()
        val wasWaitingForQuiet = !second.isCompleted && writes.size == 1
        advanceTimeBy(1)
        runCurrent()
        val secondResult = second.await()

        assertIs<TimeoutCancellationException>(firstResult.exceptionOrNull())
        assertTrue(wasWaitingForQuiet)
        assertEquals(listOf(8), secondResult)
        assertEquals(2, writes.size)
    }

    @Test
    fun `when noise contains an incomplete header then resynchronizes to a valid reply`() {
        val decoder = ModbusNotifications()
        val reply = frame(1, 3, 2, 0, 5)

        val result = decoder.receive(byteArrayOf(1, 3, 100) + reply)

        assertEquals(1, result.size)
        assertTrue(reply.contentEquals(result.single()))
    }

    @Test
    fun `when read frame includes trailing payload then rejects the length mismatch`() {
        val reply = frame(1, 3, 2, 0, 5, 0, 6)

        val result = runCatching { ModbusFrame.parseResponse(reply, 1, 3) }

        assertFalse(result.isSuccess)
    }

    private fun frame(vararg bytes: Int) = ModbusCrc.append(bytes.map { it.toByte() }.toByteArray())
}
