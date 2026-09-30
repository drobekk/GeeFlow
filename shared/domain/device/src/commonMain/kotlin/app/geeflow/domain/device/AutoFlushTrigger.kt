package app.geeflow.domain.device

import app.geeflow.data.device.model.DeviceState.BrewStatus

/** Recognizes completed brews without treating the automatic manual cycle as another shot. */
internal class AutoFlushTrigger {
    private var previousStatus = BrewStatus.Idle
    private var automaticStartPending = false
    private var automaticManualBrew = false

    fun automaticStartRequested() {
        automaticStartPending = true
    }

    fun automaticStartFailed() {
        automaticStartPending = false
    }

    fun onState(status: BrewStatus, connected: Boolean, treatManualAsFlush: Boolean = false): Boolean {
        if (!connected) {
            previousStatus = BrewStatus.Idle
            automaticStartPending = false
            automaticManualBrew = false
            return false
        }

        if (status == BrewStatus.Manual && automaticStartPending) {
            automaticManualBrew = true
            automaticStartPending = false
        } else if (status != BrewStatus.Idle && status != BrewStatus.Manual) {
            automaticStartPending = false
            automaticManualBrew = false
        }

        val finished = previousStatus in BrewStatuses && status == BrewStatus.Idle
        val manualFlush = previousStatus == BrewStatus.Manual && treatManualAsFlush
        val automatic = finished && automaticManualBrew
        previousStatus = status

        if (automatic) automaticManualBrew = false
        return finished && !automatic && !manualFlush
    }
}

private val BrewStatuses = setOf(BrewStatus.Manual, BrewStatus.Profile, BrewStatus.FreeVariable)
