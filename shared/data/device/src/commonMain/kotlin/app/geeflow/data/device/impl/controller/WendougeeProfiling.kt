package app.geeflow.data.device.impl.controller

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.device.ble.modbus.ModbusProtocolException
import app.geeflow.data.device.ble.modbus.ModbusSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.time.Duration.Companion.milliseconds

internal suspend fun ModbusSession.uploadProfile(profile: BrewProfile, isBinding: Boolean) {
    // Compile and validate the complete program before the first register write.
    val writes = WendougeeProfileCompiler().buildProfileWrites(profile, isBinding)
    for (write in writes) {
        writeProfileWithRetry {
            when (write) {
                is ProfileWrite.Single -> writeSingleRegister(write.register, write.value, PROFILE_WRITE_TIMEOUT_MS.milliseconds)
                is ProfileWrite.Multiple -> writeMultipleRegisters(
                    write.register,
                    write.values,
                    PROFILE_WRITE_TIMEOUT_MS.milliseconds,
                )
            }
        }
    }
}

/** Only idempotent register writes may use the native five-attempt policy; never retry action pulses. */
internal suspend fun writeProfileWithRetry(write: suspend () -> Unit) {
    repeat(PROFILE_WRITE_ATTEMPTS) { attempt ->
        currentCoroutineContext().ensureActive()
        try {
            write()
            return
        } catch (error: Exception) {
            currentCoroutineContext().ensureActive()
            if (!error.isRetryableProfileWrite() || attempt == PROFILE_WRITE_ATTEMPTS - 1) {
                throw error
            }
        }
        delay(PROFILE_RETRY_BACKOFF_MS * (attempt + 1))
    }
}

private fun Exception.isRetryableProfileWrite(): Boolean = when (this) {
    is CancellationException -> this is TimeoutCancellationException
    is ModbusProtocolException, is IllegalArgumentException -> false
    else -> true
}

private const val PROFILE_WRITE_TIMEOUT_MS = 200
private const val PROFILE_WRITE_ATTEMPTS = 5
private const val PROFILE_RETRY_BACKOFF_MS = 50L
