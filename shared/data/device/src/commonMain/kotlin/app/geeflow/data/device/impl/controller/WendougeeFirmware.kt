package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.ble.modbus.ModbusSession
import app.geeflow.data.device.model.SupportedDevice
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal const val AGILE_RING_MIN_FIRMWARE = 212

internal fun supportsAgileRing(model: SupportedDevice, firmware: Int?): Boolean =
    model.isLita && firmware != null && firmware >= AGILE_RING_MIN_FIRMWARE

@Suppress("TooGenericExceptionCaught")
internal suspend fun readWendougeeFirmware(modbus: ModbusSession): Int? = try {
    modbus.readHoldingRegisters(WendougeeRegisters.FIRMWARE_VERSION, 1).single().takeIf { it > 0 }
} catch (timeout: TimeoutCancellationException) {
    currentCoroutineContext().ensureActive()
    Logger.withTag(TAG).w(timeout) { "Firmware read timed out" }
    null
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Exception) {
    Logger.withTag(TAG).w(failure) { "Firmware version unavailable" }
    null
}
