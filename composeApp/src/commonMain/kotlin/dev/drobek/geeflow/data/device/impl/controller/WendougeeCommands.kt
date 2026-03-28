package dev.drobek.geeflow.data.device.impl.controller

object WendougeeCommands {

    fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }
        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    val CMD_READ_CONFIG_LONG = "0103000000258411".decodeHex()
    val CMD_START_STREAMING = "ff55ffff070001015c".decodeHex()
    val CMD_SCALE_SEARCH_QUERY = "ff55ffff9a0000ed".decodeHex()

    val CMD_POLLING_LONG = "0103057C001484D1".decodeHex()
    val CMD_POLLING_SHORT = "010100B600079C2E".decodeHex()

    val CMD_MANUAL_ON = "0105009aff00ac15".decodeHex()
    val CMD_MANUAL_OFF = "0105009a0000ede5".decodeHex()

    val CMD_SHORT_PRESS_ON = "01050096ff006c16".decodeHex()
    val CMD_SHORT_PRESS_OFF = "0105009600002de6".decodeHex()

    val CMD_CLEANING_ON = "0105009bff00fdd5".decodeHex()
    val CMD_CLEANING_OFF = "0105009b0000bc25".decodeHex()

    val CMD_SCALE_SEARCH_ON = "ff55ffff9a000104f2".decodeHex()
    val CMD_SCALE_SEARCH_OFF = "ff55ffff9a000100ee".decodeHex()
    val CMD_SCALE_LIST_REQUEST = "ff55ffff8c000100e0".decodeHex()
    val CMD_SCALE_STATUS_REQUEST = "ff55ffff8b000100df".decodeHex()
}
