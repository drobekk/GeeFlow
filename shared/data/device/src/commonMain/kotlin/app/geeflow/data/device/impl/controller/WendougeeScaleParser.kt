package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.impl.controller.WendougeeFrameParser.Companion.u8
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.SmartScale
import co.touchlab.kermit.Logger

/** Handles only complete, checksum-validated control frames. */
internal class WendougeeScaleParser(
    private val onStateUpdate: (DeviceState.() -> DeviceState) -> Unit,
    private val onScaleFound: ((SmartScale) -> Unit)?,
    private val onScanStatus: ((Boolean) -> Unit)?,
    private val onIntegrationFlags: ((Int) -> Unit)?,
) {
    fun parse(payload: ByteArray) {
        try {
            if (payload.size < FrameOffsets.PROP_MIN_PARSE_SIZE) return
            val command = payload[FrameOffsets.PROP_CMD].toInt() and BYTE_MASK
            val length = (payload[FrameOffsets.PROP_LEN_HI].toInt() and BYTE_MASK shl BYTE_SHIFT) or
                (payload[FrameOffsets.PROP_LEN_LO].toInt() and BYTE_MASK)
            if (payload.size < FrameOffsets.PROP_DATA_START + length) return
            val safeEndIndex = minOf(FrameOffsets.PROP_DATA_START + length, payload.size)
            val asciiString = payload.decodeToString(
                startIndex = FrameOffsets.PROP_DATA_START,
                endIndex = safeEndIndex,
            )
            when (command) {
                ProprietaryFrame.STATUS_RESPONSE -> parseStatusResponse(payload, length)
                ProprietaryFrame.SCAN_STATUS -> parseScanStatus(payload, length)
                ProprietaryFrame.SCALE_SEARCH_ECHO -> parseIntegrationFlags(payload, length)

                ProprietaryFrame.SERIAL_NUMBER -> Logger.withTag(TAG).d { "Serial number: $asciiString" }
                ProprietaryFrame.SCALE_FOUND -> parseScaleFound(asciiString)
                ProprietaryFrame.SCALE_LIST_RESPONSE -> parseScaleListResponse(payload, length)
                ProprietaryFrame.SCALE_ACTIVE -> parseScaleActive(asciiString)
                ProprietaryFrame.SCALE_CONNECTED -> parseScaleActive(asciiString)
                ProprietaryFrame.SCALE_DISCONNECTED -> parseScaleDisconnected(asciiString)
            }
        } catch (e: IndexOutOfBoundsException) {
            Logger.withTag(TAG).e(e) { "Error parsing proprietary frame" }
        }
    }

    private fun parseStatusResponse(payload: ByteArray, length: Int) {
        if (length < 2) return
        val type = payload[FrameOffsets.PROP_DATA_START].toInt() and BYTE_MASK
        if (type != FrameOffsets.SCALE_TYPE) return
        val connectionStatus = payload[FrameOffsets.PROP_DATA_START + 1].toInt() and BYTE_MASK
        val nameLen = length - 2
        if (connectionStatus != 1) {
            clearConnectedScale()
            return
        }
        if (nameLen <= 0) return
        val nameStart = FrameOffsets.PROP_DATA_START + 2
        val name = payload.decodeToString(startIndex = nameStart, endIndex = nameStart + nameLen).trim()
        if (name.length > 2) {
            Logger.withTag(TAG).i { "Scale status: $name (connected)" }
            val scale = SmartScale(name, isConnected = true)
            onScaleFound?.invoke(scale)
            onStateUpdate { copy(smartScale = scale) }
        }
    }

    private fun parseScaleFound(asciiString: String) {
        val name = asciiString.trim().replace(Regex("[^\\x20-\\x7E]"), "")
        if (!isScaleName(name)) return
        Logger.withTag(TAG).i { "Smart scale found: $name" }
        reportFoundScale(name)
    }

    private fun parseScaleListResponse(payload: ByteArray, length: Int) {
        if (length < 2) return
        val type = payload[FrameOffsets.PROP_DATA_START].toInt() and BYTE_MASK
        if (type != FrameOffsets.SCALE_TYPE) return
        val nameLen = payload[FrameOffsets.PROP_DATA_START + 1].toInt() and BYTE_MASK
        if (nameLen <= 0 || nameLen > length - 2) return
        val nameStart = FrameOffsets.PROP_DATA_START + 2
        val name = payload.decodeToString(startIndex = nameStart, endIndex = nameStart + nameLen).trim()
        if (name.length <= 2) return
        Logger.withTag(TAG).i { "Scale history: $name" }
        reportFoundScale(name)
    }

    private fun reportFoundScale(name: String) {
        var connected = false
        onStateUpdate {
            connected = smartScale?.name == name && smartScale.isConnected
            this
        }
        onScaleFound?.invoke(SmartScale(name, isConnected = connected))
    }

    private fun parseScaleActive(asciiString: String) {
        val name = asciiString.trim().replace(Regex("[^\\x20-\\x7E]"), "")
        if (!isScaleName(name)) return
        Logger.withTag(TAG).i { "Smart scale connected: $name" }
        val scale = SmartScale(name, isConnected = true)
        onScaleFound?.invoke(scale)
        onStateUpdate { copy(smartScale = scale) }
    }

    private fun parseScaleDisconnected(asciiString: String) {
        val name = asciiString.trim().replace(Regex("[^\\x20-\\x7E]"), "")
        Logger.withTag(TAG).i { "Smart scale disconnected: $name" }
        onStateUpdate {
            if (smartScale?.name == name) copy(smartScale = null, weight = null, weightRate = null) else this
        }
        if (isScaleName(name)) onScaleFound?.invoke(SmartScale(name, isConnected = false))
    }

    private fun clearConnectedScale() {
        var disconnected: SmartScale? = null
        onStateUpdate {
            disconnected = smartScale?.copy(isConnected = false)
            copy(smartScale = null, weight = null, weightRate = null)
        }
        disconnected?.let { onScaleFound?.invoke(it) }
    }

    private fun parseScanStatus(payload: ByteArray, length: Int) {
        if (length < 1) return
        val active = u8(payload, FrameOffsets.PROP_DATA_START) == 1
        onStateUpdate { copy(smartScaleSearchActive = active && smartScaleEnabled) }
        onScanStatus?.invoke(active)
    }

    private fun parseIntegrationFlags(payload: ByteArray, length: Int) {
        if (length < 1) return
        val flags = u8(payload, FrameOffsets.PROP_DATA_START)
        val active = flags and SCALE_ENABLED_MASK != 0
        onIntegrationFlags?.invoke(flags)
        if (!active) clearConnectedScale()
        onStateUpdate { copy(smartScaleEnabled = active, smartScaleSearchActive = active && smartScaleSearchActive) }
    }

    private object ProprietaryFrame {
        const val SCAN_STATUS = 0x83
        const val STATUS_RESPONSE = 0x8B
        const val SCALE_SEARCH_ECHO = 0x9A
        const val SERIAL_NUMBER = 0x04
        const val SCALE_FOUND = 0x81
        const val SCALE_LIST_RESPONSE = 0x8C
        const val SCALE_ACTIVE = 0x80
        const val SCALE_CONNECTED = 0x86
        const val SCALE_DISCONNECTED = 0x88
    }

    private object FrameOffsets {
        const val PROP_CMD = 4
        const val PROP_LEN_HI = 5
        const val PROP_LEN_LO = 6
        const val PROP_DATA_START = 7
        const val PROP_MIN_PARSE_SIZE = 8
        const val SCALE_TYPE = 2
    }

    private companion object {
        const val TAG = "WendougeeController"
        const val BYTE_MASK = 0xFF
        const val BYTE_SHIFT = 8
    }
}

private val SCALE_NAME_PREFIXES = listOf("BOOKOO", "TFY_", "AiLink_", "ECLAIR", "wiseda")

private fun isScaleName(name: String): Boolean = SCALE_NAME_PREFIXES.any { name.startsWith(it) }
