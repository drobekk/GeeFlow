@file:Suppress("TooGenericExceptionCaught")

package app.geeflow.data.device.impl.controller

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Concurrent foreground/manual refreshes share one operation, including its failure. */
internal class AccessoryRefreshCoordinator {
    private val mutex = Mutex()
    private var running: CompletableDeferred<Unit>? = null

    suspend fun refresh(operation: suspend () -> Unit) {
        var owner = false
        val completion = mutex.withLock {
            running ?: CompletableDeferred<Unit>().also {
                running = it
                owner = true
            }
        }
        if (!owner) {
            completion.await()
            return
        }
        try {
            operation()
            completion.complete(Unit)
        } catch (failure: Throwable) {
            completion.completeExceptionally(failure)
            throw failure
        } finally {
            withContext(NonCancellable) {
                mutex.withLock { if (running === completion) running = null }
            }
        }
    }
}
