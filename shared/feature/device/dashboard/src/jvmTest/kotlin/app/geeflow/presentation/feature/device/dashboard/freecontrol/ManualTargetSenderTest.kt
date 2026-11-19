package app.geeflow.presentation.feature.device.dashboard.freecontrol

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ManualTargetSenderTest {
    @Test
    fun `a thirty second continuous drag sends during movement and retains its final target`() = runTest {
        val writes = mutableListOf<Pair<Long, Int>>()
        val sender = ManualTargetSender<Int>(this) { writes += testScheduler.currentTime to it }
        repeat(1500) { target ->
            sender.submit(target)
            advanceTimeBy(20)
        }
        advanceUntilIdle()
        assertTrue(writes.first().first < 200)
        assertTrue(writes.any { it.first in 10000L..11000L })
        assertTrue(writes.size in 150..152)
        assertEquals(1499, writes.last().second)
        assertTrue(writes.zipWithNext().all { (a, b) -> b.first - a.first >= 200 })
    }

    @Test
    fun `slow writes stay serial and intermediate values are conflated`() = runTest {
        val writes = mutableListOf<Int>()
        val sender = ManualTargetSender<Int>(this) { target ->
            delay(500)
            writes += target
        }
        sender.submit(1)
        runCurrent()
        sender.submit(2)
        sender.submit(3)
        advanceUntilIdle()
        assertEquals(listOf(1, 3), writes)
    }

    @Test
    fun `stop discards pending writes and a later gesture can start again`() = runTest {
        val writes = mutableListOf<Int>()
        val sender = ManualTargetSender<Int>(this) { writes += it }
        sender.submit(1)
        runCurrent()
        sender.submit(2)
        sender.cancel()
        advanceUntilIdle()
        assertEquals(listOf(1), writes)
        sender.submit(3)
        advanceUntilIdle()
        assertEquals(listOf(1, 3), writes)
    }
}
