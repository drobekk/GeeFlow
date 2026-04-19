@file:Suppress("TooManyFunctions")

package dev.drobek.geeflow.data.device.ble.modbus

/**
 * Modbus RTU frame builders and response parsers.
 *
 * Frame layout (request & response): unitId | fc | payload | crcLo | crcHi
 */
internal object ModbusFrame {

    fun readCoils(unitId: Byte, address: Int, count: Int): ByteArray =
        readRequest(unitId, ModbusFunctionCode.READ_COILS, address, count)

    fun readDiscreteInputs(unitId: Byte, address: Int, count: Int): ByteArray =
        readRequest(unitId, ModbusFunctionCode.READ_DISCRETE_INPUTS, address, count)

    fun readHoldingRegisters(unitId: Byte, address: Int, count: Int): ByteArray =
        readRequest(unitId, ModbusFunctionCode.READ_HOLDING_REGISTERS, address, count)

    fun readInputRegisters(unitId: Byte, address: Int, count: Int): ByteArray =
        readRequest(unitId, ModbusFunctionCode.READ_INPUT_REGISTERS, address, count)

    fun writeSingleCoil(unitId: Byte, address: Int, state: Boolean): ByteArray {
        val value = if (state) COIL_ON else COIL_OFF
        val body = byteArrayOf(
            unitId,
            ModbusFunctionCode.WRITE_SINGLE_COIL,
            hi(address),
            lo(address),
            hi(value),
            lo(value),
        )
        return ModbusCrc.append(body)
    }

    fun writeSingleRegister(unitId: Byte, address: Int, value: Int): ByteArray {
        val body = byteArrayOf(
            unitId,
            ModbusFunctionCode.WRITE_SINGLE_REGISTER,
            hi(address),
            lo(address),
            hi(value),
            lo(value),
        )
        return ModbusCrc.append(body)
    }

    fun writeMultipleRegisters(unitId: Byte, address: Int, values: List<Int>): ByteArray {
        val count = values.size
        val byteCount = count * 2
        val header = byteArrayOf(
            unitId,
            ModbusFunctionCode.WRITE_MULTIPLE_REGISTERS,
            hi(address),
            lo(address),
            hi(count),
            lo(count),
            byteCount.toByte(),
        )
        val data = ByteArray(byteCount)
        values.forEachIndexed { i, v ->
            data[i * 2] = hi(v)
            data[i * 2 + 1] = lo(v)
        }
        return ModbusCrc.append(header + data)
    }

    fun writeMultipleCoils(unitId: Byte, address: Int, states: List<Boolean>): ByteArray {
        val count = states.size
        val byteCount = (count + BITS_PER_BYTE - 1) / BITS_PER_BYTE
        val packed = ByteArray(byteCount)
        states.forEachIndexed { i, on ->
            if (on) {
                packed[i / BITS_PER_BYTE] =
                    (packed[i / BITS_PER_BYTE].toInt() or (1 shl (i % BITS_PER_BYTE))).toByte()
            }
        }
        val header = byteArrayOf(
            unitId,
            ModbusFunctionCode.WRITE_MULTIPLE_COILS,
            hi(address),
            lo(address),
            hi(count),
            lo(count),
            byteCount.toByte(),
        )
        return ModbusCrc.append(header + packed)
    }

    /**
     * Parse a raw response frame. Verifies unitId, CRC, and detects exception responses.
     * Returns the payload after the function code (everything between fc and crc).
     */
    @Suppress("ThrowsCount")
    fun parseResponse(frame: ByteArray, expectedUnitId: Byte, expectedFc: Byte): ByteArray {
        require(frame.size >= MIN_RESPONSE_SIZE) {
            "Modbus response too short: ${frame.size} bytes"
        }
        if (!ModbusCrc.verify(frame)) throw ModbusCrcException("Modbus response CRC mismatch")
        if (frame[UNIT_ID_IDX] != expectedUnitId) {
            throw ModbusUnexpectedResponseException(
                "Modbus response unitId mismatch: expected=$expectedUnitId actual=${frame[UNIT_ID_IDX]}",
            )
        }
        val fc = frame[FC_IDX]
        if (isException(fc, expectedFc)) {
            val exceptionCode = frame[EXCEPTION_CODE_IDX]
            throw ModbusProtocolException(fc, exceptionCode)
        }
        if (fc != expectedFc) {
            throw ModbusUnexpectedResponseException(
                "Modbus response fc mismatch: expected=0x${expectedFc.toHex()} actual=0x${fc.toHex()}",
            )
        }
        return frame.copyOfRange(FC_IDX + 1, frame.size - ModbusCrc.CRC_SIZE)
    }

    /** For read responses: first byte of payload is byte count, followed by the data bytes. */
    fun parseByteCountedPayload(payload: ByteArray): ByteArray {
        require(payload.isNotEmpty()) { "Read response payload is empty" }
        val byteCount = payload[0].toInt() and BYTE_MASK
        require(payload.size >= byteCount + 1) {
            "Read response payload truncated: byteCount=$byteCount available=${payload.size - 1}"
        }
        return payload.copyOfRange(1, 1 + byteCount)
    }

    fun decodeRegisters(data: ByteArray): List<Int> {
        require(data.size % 2 == 0) { "Register data must be even length, was ${data.size}" }
        return (0 until data.size / 2).map { i ->
            ((data[i * 2].toInt() and BYTE_MASK) shl BITS_PER_BYTE) or
                (data[i * 2 + 1].toInt() and BYTE_MASK)
        }
    }

    fun decodeCoils(data: ByteArray, count: Int): List<Boolean> {
        require(count <= data.size * BITS_PER_BYTE) {
            "Coil count $count exceeds available bits ${data.size * BITS_PER_BYTE}"
        }
        return (0 until count).map { i ->
            (data[i / BITS_PER_BYTE].toInt() shr (i % BITS_PER_BYTE)) and 1 == 1
        }
    }

    private fun readRequest(unitId: Byte, fc: Byte, address: Int, count: Int): ByteArray {
        val body = byteArrayOf(
            unitId,
            fc,
            hi(address),
            lo(address),
            hi(count),
            lo(count),
        )
        return ModbusCrc.append(body)
    }

    private fun isException(actualFc: Byte, expectedFc: Byte): Boolean {
        val expectedException = (expectedFc.toInt() or ModbusFunctionCode.EXCEPTION_MASK.toInt()).toByte()
        return actualFc == expectedException
    }

    private fun hi(v: Int): Byte = ((v ushr BITS_PER_BYTE) and BYTE_MASK).toByte()
    private fun lo(v: Int): Byte = (v and BYTE_MASK).toByte()

    private fun Byte.toHex(): String =
        (this.toInt() and BYTE_MASK).toString(HEX_RADIX).padStart(2, '0')

    private const val BYTE_MASK = 0xFF
    private const val BITS_PER_BYTE = 8
    private const val HEX_RADIX = 16
    private const val MIN_RESPONSE_SIZE = 4
    private const val UNIT_ID_IDX = 0
    private const val FC_IDX = 1
    private const val EXCEPTION_CODE_IDX = 2
    private const val COIL_ON = 0xFF00
    private const val COIL_OFF = 0x0000
}
