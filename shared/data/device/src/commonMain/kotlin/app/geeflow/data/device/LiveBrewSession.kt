package app.geeflow.data.device

import app.geeflow.data.brew.model.PhaseControl

interface LiveBrewSession {
    /** Feedback controllers must run even when the profile's requested target stays constant. */
    val requiresContinuousUpdates: Boolean get() = false

    /** Returns the actual quantized target sent to the device. Calls must be serialized. */
    suspend fun applyTarget(target: PhaseControl): PhaseControl
    suspend fun stop()
}
