package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.ble.modbus.ModbusSession
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

/** Actions are toggles: send the press once and always attempt its release, including on cancellation. */
@Suppress("TooGenericExceptionCaught") // Releasing hardware is necessary for every failure; retain the original cause.
internal suspend fun ModbusSession.pulseCoil(address: Int) {
    var pressFailure: Throwable? = null
    var releaseFailure: Throwable? = null
    try {
        writeSingleCoil(address, true, PULSE_WRITE_TIMEOUT_MS.milliseconds)
        delay(PULSE_DURATION_MS)
    } catch (error: Throwable) {
        pressFailure = error
        throw error
    } finally {
        try {
            withContext(NonCancellable) {
                withTimeout(PULSE_RELEASE_TIMEOUT_MS.milliseconds) {
                    writeSingleCoil(address, false, PULSE_WRITE_TIMEOUT_MS.milliseconds)
                }
            }
        } catch (releaseError: Throwable) {
            val originalError = pressFailure
            if (originalError == null) {
                releaseFailure = releaseError
            } else {
                originalError.addSuppressed(releaseError)
            }
        }
    }
    releaseFailure?.let { throw it }
}

private const val PULSE_DURATION_MS = 100L
private const val PULSE_WRITE_TIMEOUT_MS = 1000
private const val PULSE_RELEASE_TIMEOUT_MS = 4000
