package app.geeflow.data.device.impl.controller

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AccessoryRefreshCoordinatorTest {
    @Test
    fun `when foreground refreshes overlap then sends one request sequence`() = runTest {
        val coordinator = AccessoryRefreshCoordinator()
        val release = CompletableDeferred<Unit>()
        var requests = 0
        val operation: suspend () -> Unit = {
            requests++
            release.await()
        }

        val first = async(start = CoroutineStart.UNDISPATCHED) { coordinator.refresh(operation) }
        val second = async(start = CoroutineStart.UNDISPATCHED) { coordinator.refresh(operation) }
        release.complete(Unit)
        first.await()
        second.await()

        assertEquals(1, requests)
    }

    @Test
    fun `when refresh fails then next refresh can run`() = runTest {
        val coordinator = AccessoryRefreshCoordinator()
        var requests = 0

        val failure = runCatching { coordinator.refresh { error("offline") } }
        coordinator.refresh { requests++ }

        assertEquals(true, failure.isFailure)
        assertEquals(1, requests)
    }
}
