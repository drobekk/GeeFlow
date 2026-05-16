package app.geeflow.data.device.ble.modbus

open class ModbusException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Thrown when the response CRC does not match the payload. */
class ModbusCrcException(message: String) : ModbusException(message)

/**
 * Thrown when the remote returns a Modbus exception response (fc | 0x80 + exception code).
 * See Modbus Application Protocol spec §7 for code meanings.
 */
class ModbusProtocolException(
    val functionCode: Byte,
    val exceptionCode: Byte,
) : ModbusException(
    "Modbus exception: fc=0x${functionCode.toHex()} code=0x${exceptionCode.toHex()}",
)

/** Thrown when a response does not match the expected function code or register. */
class ModbusUnexpectedResponseException(message: String) : ModbusException(message)

private fun Byte.toHex(): String {
    val v = this.toInt() and BYTE_MASK
    return v.toString(HEX_RADIX).padStart(2, '0')
}

private const val BYTE_MASK = 0xFF
private const val HEX_RADIX = 16
