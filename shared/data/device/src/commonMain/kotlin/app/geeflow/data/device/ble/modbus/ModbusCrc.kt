package app.geeflow.data.device.ble.modbus

/**
 * Modbus RTU CRC-16 (polynomial 0xA001, initial value 0xFFFF, little-endian output).
 */
internal object ModbusCrc {
    private const val CRC_INITIAL = 0xFFFF
    private const val CRC_POLY = 0xA001
    private const val CRC_BITS = 8
    private const val BYTE_MASK = 0xFF
    private const val LOW_BIT_MASK = 0x0001

    fun calculate(bytes: ByteArray): ByteArray {
        var crc = CRC_INITIAL
        for (b in bytes) {
            crc = crc xor (b.toInt() and BYTE_MASK)
            repeat(CRC_BITS) {
                crc = if (crc and LOW_BIT_MASK != 0) {
                    (crc ushr 1) xor CRC_POLY
                } else {
                    crc ushr 1
                }
            }
        }
        return byteArrayOf(
            (crc and BYTE_MASK).toByte(),
            ((crc ushr CRC_BITS) and BYTE_MASK).toByte(),
        )
    }

    fun append(bytes: ByteArray): ByteArray = bytes + calculate(bytes)

    fun verify(frame: ByteArray): Boolean {
        if (frame.size < CRC_SIZE + 1) return false
        val payload = frame.copyOfRange(0, frame.size - CRC_SIZE)
        val expected = calculate(payload)
        return expected[0] == frame[frame.size - CRC_SIZE] &&
            expected[1] == frame[frame.size - CRC_SIZE + 1]
    }

    const val CRC_SIZE: Int = 2
}
