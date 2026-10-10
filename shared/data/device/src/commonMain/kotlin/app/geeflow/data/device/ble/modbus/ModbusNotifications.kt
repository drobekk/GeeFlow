package app.geeflow.data.device.ble.modbus

/** Reassembles bounded RTU replies and discards noise and frames with invalid CRC. */
internal class ModbusNotifications {
    private var pending = byteArrayOf()

    fun reset() {
        pending = byteArrayOf()
    }

    @Suppress("LoopWithTooManyJumpStatements")
    fun receive(bytes: ByteArray): List<ByteArray> {
        pending = (pending + bytes).takeLast(MAX_BUFFERED_BYTES).toByteArray()
        val result = mutableListOf<ByteArray>()
        while (pending.size >= MINIMUM_HEADER_SIZE) {
            val length = responseLength(pending)
            if (length == null) {
                pending = pending.drop(1).toByteArray()
                continue
            }
            if (pending.size < length) {
                // A corrupt header must not indefinitely hide a later complete valid frame.
                val next = (1..pending.size - MINIMUM_HEADER_SIZE).firstOrNull { offset ->
                    val tail = pending.copyOfRange(offset, pending.size)
                    val tailLength = responseLength(tail)
                    tailLength != null && tail.size >= tailLength &&
                        ModbusCrc.verify(tail.copyOfRange(0, tailLength))
                }
                if (next == null) break
                pending = pending.drop(next).toByteArray()
                continue
            }
            val frame = pending.copyOfRange(0, length)
            if (ModbusCrc.verify(frame)) {
                result += frame
                pending = pending.drop(length).toByteArray()
            } else {
                pending = pending.drop(1).toByteArray()
            }
        }
        return result
    }

    @Suppress("ReturnCount")
    private fun responseLength(bytes: ByteArray): Int? {
        if (bytes.size < MINIMUM_HEADER_SIZE) return null
        if ((bytes[0].toInt() and BYTE_MASK) !in 1..MAXIMUM_UNIT_ID) return null
        val fc = bytes[1].toInt() and BYTE_MASK
        val base = fc and FUNCTION_MASK
        if (base !in SUPPORTED_FUNCTIONS) return null
        if (fc != base) return EXCEPTION_LENGTH
        return if (base in READ_FUNCTIONS) {
            val count = bytes[2].toInt() and BYTE_MASK
            if (count !in 1..MAXIMUM_PAYLOAD_BYTES) null else count + READ_OVERHEAD
        } else {
            WRITE_LENGTH
        }
    }

    private companion object {
        const val BYTE_MASK = 0xFF
        const val FUNCTION_MASK = 0x7F
        const val MAXIMUM_UNIT_ID = 247
        const val MAXIMUM_PAYLOAD_BYTES = 250
        const val MINIMUM_HEADER_SIZE = 3
        const val EXCEPTION_LENGTH = 5
        const val WRITE_LENGTH = 8
        const val READ_OVERHEAD = 5
        const val MAX_BUFFERED_BYTES = 1024
        val READ_FUNCTIONS = setOf(1, 2, 3, 4)
        val SUPPORTED_FUNCTIONS = READ_FUNCTIONS + setOf(5, 6, 15, 16)
    }
}
