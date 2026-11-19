package app.geeflow.presentation.feature.device.dashboard.freecontrol

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Send during a continuous drag, retaining only the newest pending target. One write at a time. */
internal class ManualTargetSender<T>(private val scope: CoroutineScope, private val send: suspend (T) -> Unit) {
    private var pending: T? = null
    private var job: Job? = null

    fun submit(target: T) {
        pending = target
        if (job?.isActive == true) return
        job = scope.launch {
            while (true) {
                val next = pending ?: break
                pending = null
                send(next)
                delay(IntervalMs)
            }
        }
    }

    fun cancel() {
        pending = null
        job?.cancel()
        job = null
    }

    private companion object {
        const val IntervalMs = 200L
    }
}
