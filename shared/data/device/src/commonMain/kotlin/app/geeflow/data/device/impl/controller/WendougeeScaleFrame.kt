package app.geeflow.data.device.impl.controller

internal fun buildScaleFrame(cmd: Int, name: String): ByteArray {
    val nameBytes = name.encodeToByteArray()
    val len = nameBytes.size
    val buffer = ByteArray(SCALE_FRAME_DATA_OFFSET + len)
    SCALE_FRAME_PREFIX.copyInto(buffer)
    buffer[SCALE_FRAME_CMD_IDX] = cmd.toByte()
    buffer[SCALE_FRAME_CMD_IDX + 1] = (len ushr BYTE_SHIFT).toByte()
    buffer[SCALE_FRAME_CMD_IDX + 2] = (len and BYTE_MASK).toByte()
    nameBytes.copyInto(buffer, destinationOffset = SCALE_FRAME_DATA_OFFSET)
    var sum = 0
    for (i in SCALE_FRAME_CMD_IDX until buffer.size) sum += buffer[i].toInt() and BYTE_MASK
    return buffer + ((sum + SCALE_FRAME_CHECKSUM_SALT) and BYTE_MASK).toByte()
}

private const val SCALE_FRAME_DATA_OFFSET = 7
private const val SCALE_FRAME_CMD_IDX = 4
private const val SCALE_FRAME_CHECKSUM_SALT = 0x53
private val SCALE_FRAME_PREFIX = byteArrayOf(0xFF.toByte(), 0x55.toByte(), 0xFF.toByte(), 0xFF.toByte())
private const val BYTE_SHIFT = 8
private const val BYTE_MASK = 0xFF
