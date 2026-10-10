package app.geeflow.data.device.impl.controller

internal fun buildScaleFrame(cmd: Int, name: String): ByteArray = buildControlFrame(cmd, name.encodeToByteArray())

internal fun buildScaleConnectivityFrame(flags: Int, enabled: Boolean): ByteArray =
    buildIntegrationConnectivityFrame(flags, SCALE_ENABLED_MASK, enabled)

internal fun buildIntegrationConnectivityFrame(flags: Int, mask: Int, enabled: Boolean): ByteArray {
    val updated = if (enabled) flags or mask else flags and mask.inv()
    return buildControlFrame(INTEGRATION_FLAGS_COMMAND, byteArrayOf(updated.toByte()))
}

internal fun buildControlFrame(cmd: Int, nameBytes: ByteArray): ByteArray {
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

internal const val SINGLE_DOSE_ENABLED_MASK = 0x01
internal const val SCALE_ENABLED_MASK = 0x04
private const val INTEGRATION_FLAGS_COMMAND = 0x9A

private const val SCALE_FRAME_DATA_OFFSET = 7
private const val SCALE_FRAME_CMD_IDX = 4
private const val SCALE_FRAME_CHECKSUM_SALT = 0x53
private val SCALE_FRAME_PREFIX = byteArrayOf(0xFF.toByte(), 0x55.toByte(), 0xFF.toByte(), 0xFF.toByte())
private const val BYTE_SHIFT = 8
private const val BYTE_MASK = 0xFF
