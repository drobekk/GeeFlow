package app.geeflow.data.device.impl.controller

internal fun buildScaleFrame(cmd: Int, name: String): ByteArray = buildControlFrame(cmd, name.encodeToByteArray())

internal fun buildScaleConnectivityFrame(flags: Int, enabled: Boolean): ByteArray =
    buildIntegrationConnectivityFrame(flags, SCALE_ENABLED_MASK, enabled)

internal fun buildIntegrationConnectivityFrame(flags: Int, mask: Int, enabled: Boolean): ByteArray {
    val updated = if (enabled) flags or mask else flags and mask.inv()
    return buildControlFrame(INTEGRATION_FLAGS_COMMAND, byteArrayOf(updated.toByte()))
}

internal fun buildControlFrame(cmd: Int, nameBytes: ByteArray, standing: Int = DEFAULT_STANDING): ByteArray {
    require(standing in 0..MAX_STANDING)
    val len = nameBytes.size
    val buffer = ByteArray(SCALE_FRAME_DATA_OFFSET + len)
    SCALE_FRAME_PREFIX.copyInto(buffer)
    buffer[STANDING_HIGH_IDX] = (standing ushr BYTE_SHIFT).toByte()
    buffer[STANDING_LOW_IDX] = standing.toByte()
    buffer[SCALE_FRAME_CMD_IDX] = cmd.toByte()
    buffer[SCALE_FRAME_CMD_IDX + 1] = (len ushr BYTE_SHIFT).toByte()
    buffer[SCALE_FRAME_CMD_IDX + 2] = (len and BYTE_MASK).toByte()
    nameBytes.copyInto(buffer, destinationOffset = SCALE_FRAME_DATA_OFFSET)
    var sum = 0
    for (i in CHECKSUM_START_IDX until buffer.size) sum += buffer[i].toInt() and BYTE_MASK
    return buffer + (sum and BYTE_MASK).toByte()
}

/** The APK sends five BE16 words: three zeros, grinding size and motor speed. */
internal fun buildSingleDoseGrindFrame(standing: Int, grindingSize: Int, grindingSpeed: Int): ByteArray {
    require(grindingSize in MIN_GRINDING_SIZE..MAX_GRINDING_SIZE)
    require(grindingSpeed in MIN_GRINDING_SPEED..MAX_GRINDING_SPEED)
    val payload = byteArrayOf(
        0, 0, 0, 0, 0, 0,
        (grindingSize ushr BYTE_SHIFT).toByte(), grindingSize.toByte(),
        (grindingSpeed ushr BYTE_SHIFT).toByte(), grindingSpeed.toByte(),
    )
    return buildControlFrame(GRIND_COMMAND, payload, standing)
}

internal const val MIN_GRINDING_SIZE = 0
internal const val MAX_GRINDING_SIZE = 600
internal const val MIN_GRINDING_SPEED = 200
internal const val MAX_GRINDING_SPEED = 1000
internal const val SINGLE_DOSE_ENABLED_MASK = 0x01
internal const val SCALE_ENABLED_MASK = 0x04
private const val INTEGRATION_FLAGS_COMMAND = 0x9A

private const val SCALE_FRAME_DATA_OFFSET = 7
private const val SCALE_FRAME_CMD_IDX = 4
private const val STANDING_HIGH_IDX = 2
private const val STANDING_LOW_IDX = 3
private const val CHECKSUM_START_IDX = 1
private const val DEFAULT_STANDING = 0xFFFF
private const val MAX_STANDING = 0xFFFF
private const val GRIND_COMMAND = 0x20
private val SCALE_FRAME_PREFIX = byteArrayOf(0xFF.toByte(), 0x55.toByte(), 0xFF.toByte(), 0xFF.toByte())
private const val BYTE_SHIFT = 8
private const val BYTE_MASK = 0xFF
