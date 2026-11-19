package app.geeflow.data.device.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BookooScaleFrameTest {
    @Test fun decodesCapturedBookooAsciiSignPacketsAndValidatesChecksum() {
        val positive = BookooScaleFrame.decode("030B000000012B0000002B00023200460100007E".hex())!!
        assertEquals(0L, positive.elapsedMillis)
        assertEquals(0f, positive.weightGrams)
        assertEquals(0.02f, positive.reportedFlowGramsPerSecond)
        assertEquals(50, positive.batteryPercent)

        val negative = BookooScaleFrame.decode("030B000000012D0005962D17943200460100006C".hex())!!
        assertEquals(-14.3f, negative.weightGrams)
        assertEquals(-60.36f, negative.reportedFlowGramsPerSecond)

        val invalidSign = "030B000000012B0000002B00023200460100007E".hex()
        invalidSign[6] = 0
        updateChecksum(invalidSign)
        assertNull(BookooScaleFrame.decode(invalidSign))

        val invalidChecksum = "030B000000012B0000002B00023200460100007E".hex()
        invalidChecksum[19] = 0
        assertNull(BookooScaleFrame.decode(invalidChecksum))
    }

    private fun String.hex() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private fun updateChecksum(bytes: ByteArray) {
        var checksum = 0
        for (i in 0 until 19) checksum = checksum xor (bytes[i].toInt() and 0xff)
        bytes[19] = checksum.toByte()
    }
}
