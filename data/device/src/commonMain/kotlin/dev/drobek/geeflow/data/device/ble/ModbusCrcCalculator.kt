package dev.drobek.geeflow.data.device.ble

object ModbusCrcCalculator {
    private const val CRC_INITIAL = 0xFFFF
    private const val CRC_POLY = 0xA001
    private const val CRC_BITS = 8
    private const val BYTE_MASK = 0xFF
    private const val LOW_BIT_MASK = 0x0001

    fun calculateCRC(bytes: ByteArray): ByteArray {
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
}
