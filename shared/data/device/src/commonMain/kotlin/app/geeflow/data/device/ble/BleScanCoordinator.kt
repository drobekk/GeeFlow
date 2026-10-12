package app.geeflow.data.device.ble

import dev.bluefalcon.core.BlueFalcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

/** A scan belongs to all current consumers; cancelling one must not stop another. */
@Single
class BleScanCoordinator(private val client: BlueFalcon, private val scope: CoroutineScope) {
    private val mutex = Mutex()
    private var consumers = 0
    private var scanning: Job? = null

    suspend fun <T> scanning(block: suspend () -> T): T {
        mutex.withLock {
            if (consumers++ == 0) scanning = scope.launch { client.scan() }
        }
        try {
            return block()
        } finally {
            withContext(NonCancellable) {
                mutex.withLock {
                    if (--consumers == 0) {
                        scanning?.cancelAndJoin()
                        scanning = null
                        client.stopScanning()
                    }
                }
            }
        }
    }
}
