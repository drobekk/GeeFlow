package app.geeflow.data.device.impl.controller

/** Reassembles FF55 notifications and validates each complete frame before dispatch. */
internal class WendougeeControlFrames {
    private var pending = byteArrayOf()

    fun reset() {
        pending = byteArrayOf()
    }

    fun receive(bytes: ByteArray): List<ByteArray> {
        pending += bytes
        val frames = mutableListOf<ByteArray>()
        while (pending.size >= HEADER_SIZE) {
            if (pending[0] != 0xFF.toByte() || pending[1] != 0x55.toByte()) {
                pending = pending.copyOfRange(1, pending.size)
            } else {
                if (pending.size < MIN_FRAME_SIZE) return frames
                val length = WendougeeFrameParser.u16be(pending, LENGTH_OFFSET) + MIN_FRAME_SIZE
                if (pending.size < length) return frames
                val checksum = pending.sliceArray(1 until length - 1).sumOf { it.toInt() and BYTE_MASK } and BYTE_MASK
                if (checksum != WendougeeFrameParser.u8(pending, length - 1)) {
                    pending = pending.copyOfRange(1, pending.size)
                } else {
                    frames += pending.copyOfRange(0, length)
                    pending = pending.copyOfRange(length, pending.size)
                }
            }
        }
        return frames
    }

    private companion object {
        const val HEADER_SIZE = 2
        const val MIN_FRAME_SIZE = 8
        const val LENGTH_OFFSET = 5
        const val BYTE_MASK = 0xFF
    }
}
