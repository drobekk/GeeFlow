package app.geeflow.data.device.model

/** A validated BOOKOO 0x03/0x0B measurement packet. Values use g, g/s and milliseconds. */
data class BookooScaleFrame(
    val elapsedMillis: Long,
    val weightGrams: Float,
    val reportedFlowGramsPerSecond: Float,
    val batteryPercent: Int,
) {
    companion object {
        fun decode(bytes: ByteArray): BookooScaleFrame? {
            if (bytes.size != PACKET_SIZE || bytes.u8(0) != PRODUCT || bytes.u8(1) != WEIGHT_TYPE) return null
            var checksum = 0
            for (index in 0 until bytes.lastIndex) checksum = checksum xor bytes.u8(index)
            if (checksum != bytes.u8(bytes.lastIndex) || bytes.u8(5) != GRAM_UNIT) return null
            if (bytes.u8(6) != SIGN_PLUS && bytes.u8(6) != SIGN_MINUS) return null
            if (bytes.u8(10) != SIGN_PLUS && bytes.u8(10) != SIGN_MINUS) return null
            val weightMagnitude = bytes.u24(7) / 100f
            val weight = if (bytes.u8(6) == SIGN_MINUS) -weightMagnitude else weightMagnitude
            val rateMagnitude = bytes.u16(11) / 100f
            val rate = if (bytes.u8(10) == SIGN_MINUS) -rateMagnitude else rateMagnitude
            val battery = bytes.u8(13)
            if (battery !in 0..100 || !weight.isFinite() || !rate.isFinite()) return null
            return BookooScaleFrame(bytes.u24(2).toLong(), weight, rate, battery)
        }

        private const val PACKET_SIZE = 20
        private const val PRODUCT = 0x03
        private const val WEIGHT_TYPE = 0x0B
        private const val GRAM_UNIT = 0x01
        // BOOKOO documents +/- symbols. The tablet capture confirms ASCII '+' (0x2B)
        // and '-' (0x2D) in both sign fields; 0/1 was an incorrect provisional mapping.
        private const val SIGN_PLUS = 0x2B
        private const val SIGN_MINUS = 0x2D
    }
}

private fun ByteArray.u8(index: Int) = this[index].toInt() and 0xff
private fun ByteArray.u16(index: Int) = (u8(index) shl 8) or u8(index + 1)
private fun ByteArray.u24(index: Int) = (u8(index) shl 16) or (u8(index + 1) shl 8) or u8(index + 2)
