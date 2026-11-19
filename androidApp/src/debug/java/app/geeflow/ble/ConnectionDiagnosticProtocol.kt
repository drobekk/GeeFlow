package app.geeflow.ble

internal object ConnectionDiagnosticProtocol {
    const val SERVICE_UUID = "00010203-0405-0607-0809-0a0b0c0d1910"
    const val DATA_UUID = "00010203-0405-0607-0809-0a0b0c0d2b10"
    const val CONTROL_UUID = "00010203-0405-0607-0809-0a0b0c0d2c10"
    const val FALLBACK_SERIAL = "AB08118958"
    const val FALLBACK_NAME = "WDG_Data_1020BA044336"
    const val FALLBACK_MAC = "10:20:BA:04:43:36"
    const val READ_REQUEST_HEX = "0103000000258411"
    const val REGISTER_COUNT = 37

    fun serialMatches(name: String?, serial: String): Boolean {
        val candidate = name?.uppercase().orEmpty()
        val normalized = serial.trim().uppercase()
        if (normalized.isEmpty()) return false
        return Regex("(^|[^A-Z0-9])${Regex.escape(normalized)}($|[^A-Z0-9])").containsMatchIn(candidate)
    }

    fun serialMatchesData(advertisedValues: Collection<ByteArray>, serial: String): Boolean {
        val needle = serial.trim().uppercase().encodeToByteArray()
        if (needle.isEmpty()) return false
        return advertisedValues.any { value ->
            value.indices.any { start ->
                start + needle.size <= value.size && needle.indices.all { offset ->
                    (value[start + offset].toInt() and 0xFF).toChar().uppercaseChar() == needle[offset].toInt().toChar().uppercaseChar()
                }
            }
        }
    }

    fun fallbackMatches(name: String?, address: String?): Boolean {
        if (serialMatches(name, FALLBACK_SERIAL) || name.equals(FALLBACK_NAME, ignoreCase = true)) return true
        if (normalizeMac(address.orEmpty()) == normalizeMac(FALLBACK_MAC)) return true
        val embedded = Regex("([0-9A-Fa-f]{12})$").find(name.orEmpty())?.groupValues?.get(1)
        return normalizeMac(embedded.orEmpty()) == normalizeMac(FALLBACK_MAC)
    }

    fun readRequest(): ByteArray = READ_REQUEST_HEX.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    fun decodeFrame(frame: ByteArray): ReadResult {
        if (frame.size < 5) return ReadResult(false, "response shorter than minimum Modbus frame", emptyList())
        if ((frame[0].toInt() and 0xFF) != 0x01) return ReadResult(false, "unexpected unit id", emptyList())
        if ((frame[1].toInt() and 0xFF) == 0x83) {
            if (frame.size != 5) return ReadResult(false, "invalid Modbus exception length", emptyList())
            if (!validCrc(frame)) return ReadResult(false, "Modbus exception CRC mismatch", emptyList())
            return ReadResult(false, "Modbus exception ${frame.getOrNull(2)?.toInt()?.and(0xFF)}", emptyList())
        }
        if ((frame[1].toInt() and 0xFF) != 0x03) return ReadResult(false, "unexpected function code", emptyList())
        val byteCount = frame[2].toInt() and 0xFF
        if (byteCount != REGISTER_COUNT * 2) return ReadResult(false, "expected ${REGISTER_COUNT * 2} data bytes, received $byteCount", emptyList())
        if (frame.size != byteCount + 5) return ReadResult(false, "frame length does not match byte count", emptyList())
        if (!validCrc(frame)) return ReadResult(false, "Modbus CRC mismatch", emptyList())
        val values = (0 until REGISTER_COUNT).map { index ->
            ((frame[3 + index * 2].toInt() and 0xFF) shl 8) or (frame[4 + index * 2].toInt() and 0xFF)
        }
        return ReadResult(true, "valid FC03 response", values)
    }

    fun validCrc(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        var crc = 0xFFFF
        for (index in 0 until bytes.size - 2) {
            crc = crc xor (bytes[index].toInt() and 0xFF)
            repeat(8) { crc = if ((crc and 1) != 0) (crc ushr 1) xor 0xA001 else crc ushr 1 }
        }
        val received = (bytes[bytes.lastIndex - 1].toInt() and 0xFF) or
            ((bytes[bytes.lastIndex].toInt() and 0xFF) shl 8)
        return crc == received
    }

    fun normalizeMac(value: String): String = value.filter(Char::isLetterOrDigit).uppercase()

    data class ReadResult(val valid: Boolean, val reason: String, val registers: List<Int>)
}

/** Accumulates Android notification fragments until a complete Modbus response is available. */
internal class ModbusResponseAccumulator(
    private val onFrame: (ByteArray) -> Unit,
) {
    private val bytes = ArrayList<Byte>()

    @Synchronized
    fun append(fragment: ByteArray) {
        bytes.addAll(fragment.toList())
        while (bytes.size >= 3) {
            val header = (0 until bytes.lastIndex).firstOrNull { index ->
                (bytes[index].toInt() and 0xFF) == 0x01 &&
                    ((bytes[index + 1].toInt() and 0xFF) == 0x03 || (bytes[index + 1].toInt() and 0xFF) == 0x83)
            }
            if (header == null) {
                val preserveLast = bytes.lastOrNull()?.toInt()?.and(0xFF) == 0x01
                bytes.clear()
                if (preserveLast) bytes.add(0x01)
                return
            }
            if (header > 0) repeat(header) { bytes.removeAt(0) }
            val function = bytes[1].toInt() and 0xFF
            val expectedLength = if (function == 0x83) 5 else (bytes[2].toInt() and 0xFF) + 5
            if (bytes.size < expectedLength) return
            val frame = ByteArray(expectedLength) { bytes.removeAt(0) }
            onFrame(frame)
        }
    }

    @Synchronized
    fun pendingSnapshot(): ByteArray = ByteArray(bytes.size) { bytes[it] }
}
