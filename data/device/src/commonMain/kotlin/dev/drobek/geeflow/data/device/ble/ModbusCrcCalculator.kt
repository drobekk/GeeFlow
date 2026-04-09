package dev.drobek.geeflow.data.device.ble

object ModbusCrcCalculator {
    fun calculateCRC(bytes: ByteArray): ByteArray {
        var crc = 0xFFFF
        for (b in bytes) {
            crc = crc xor (b.toInt() and 0xFF)
            repeat(8) {
                crc = if (crc and 0x0001 != 0) {
                    (crc ushr 1) xor 0xA001
                } else {
                    crc ushr 1
                }
            }
        }
        return byteArrayOf(
            (crc and 0xFF).toByte(),
            ((crc ushr 8) and 0xFF).toByte()
        )
    }
}
