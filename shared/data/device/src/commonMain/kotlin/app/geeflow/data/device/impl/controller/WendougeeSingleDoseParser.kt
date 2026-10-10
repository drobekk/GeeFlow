package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.impl.controller.WendougeeFrameParser.Companion.u8
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.SingleDoseGrinder

/** Receives complete, checksum-validated FF55 frames shared with the scale integration. */
internal class WendougeeSingleDoseParser(
    private val onStateUpdate: (DeviceState.() -> DeviceState) -> Unit,
    private val onFound: ((SingleDoseGrinder) -> Unit)?,
) {
    fun parse(frame: ByteArray) {
        val data = frame.copyOfRange(DATA_OFFSET, frame.lastIndex)
        when (u8(frame, COMMAND_OFFSET)) {
            GRIND_STATUS -> parseGrindStatus(frame, data)
            STATUS -> parseStatus(data)
            HISTORY -> parseHistory(data)
            FOUND -> reportName(data, connected = false)
            CONNECTED, CONNECT -> reportName(data, connected = true)
            DISCONNECTED -> disconnectName(data.decodeToString().trim())
            SCAN_STATUS -> if (data.isNotEmpty()) {
                onStateUpdate {
                    copy(singleDoseGrinderSearchActive = data[0] == 1.toByte() && singleDoseGrinderEnabled)
                }
            }
            FLAGS -> parseFlags(data)
        }
    }

    private fun parseStatus(data: ByteArray) {
        if (data.size < 2 || data[0] != SINGLE_DOSE_TYPE) return
        if (data[1] != 1.toByte()) {
            clearConnection()
        } else {
            report(data.copyOfRange(2, data.size).decodeToString().trim(), connected = true)
        }
    }

    private fun parseGrindStatus(frame: ByteArray, data: ByteArray) {
        if (data.size < MIN_GRIND_STATUS_BYTES) return
        val standing = (u8(frame, STANDING_HIGH_IDX) shl BYTE_SHIFT) or u8(frame, STANDING_LOW_IDX)
        val status = u8(data, GRIND_STATUS_OFFSET)
        onStateUpdate {
            val connected = singleDoseGrinder ?: return@onStateUpdate this
            copy(singleDoseGrinder = connected.copy(standing = standing, operationStatus = status))
        }
    }

    private fun parseHistory(data: ByteArray) {
        if (data.size < 2 || data[0] != SINGLE_DOSE_TYPE) return
        val length = u8(data, 1)
        if (length == 0 || length > data.size - 2) return
        report(data.copyOfRange(2, 2 + length).decodeToString().trim(), connected = false)
    }

    private fun reportName(data: ByteArray, connected: Boolean) {
        val name = data.decodeToString().trim().replace(Regex("[^\\x20-\\x7E]"), "")
        // GlobalBleState 0x16ad5dc: operateLink prefixes [BGM_, Milo].
        if (isSingleDoseName(name)) report(name, connected)
    }

    private fun report(name: String, connected: Boolean) {
        if (name.isBlank()) return
        var grinder = SingleDoseGrinder(name, connected)
        onStateUpdate {
            if (connected) {
                grinder = singleDoseGrinder?.takeIf { it.name == name }?.copy(isConnected = true) ?: grinder
                copy(singleDoseGrinder = grinder)
            } else {
                grinder = grinder.copy(isConnected = singleDoseGrinder?.name == name)
                this
            }
        }
        onFound?.invoke(grinder)
    }

    private fun disconnectName(name: String) {
        var wasConnected = false
        onStateUpdate {
            wasConnected = singleDoseGrinder?.name == name
            if (wasConnected) copy(singleDoseGrinder = null) else this
        }
        if (wasConnected || isSingleDoseName(name)) onFound?.invoke(SingleDoseGrinder(name, false))
    }

    private fun clearConnection() {
        var disconnected: SingleDoseGrinder? = null
        onStateUpdate {
            disconnected = singleDoseGrinder?.copy(isConnected = false)
            copy(singleDoseGrinder = null)
        }
        disconnected?.let { onFound?.invoke(it) }
    }

    private fun parseFlags(data: ByteArray) {
        if (data.isEmpty()) return
        val enabled = u8(data, 0) and SINGLE_DOSE_ENABLED_MASK != 0
        if (!enabled) clearConnection()
        onStateUpdate {
            copy(
                singleDoseGrinderEnabled = enabled,
                singleDoseGrinderSearchActive = enabled && singleDoseGrinderSearchActive,
            )
        }
    }

    private fun isSingleDoseName(name: String) = name.startsWith("BGM_") || name.startsWith("Milo")

    private companion object {
        const val COMMAND_OFFSET = 4
        const val DATA_OFFSET = 7
        const val STANDING_HIGH_IDX = 2
        const val STANDING_LOW_IDX = 3
        const val BYTE_SHIFT = 8
        const val GRIND_STATUS_OFFSET = 8
        const val MIN_GRIND_STATUS_BYTES = 12
        const val SINGLE_DOSE_TYPE: Byte = 0
        const val CONNECT = 0x80
        const val GRIND_STATUS = 0x20
        const val FOUND = 0x81
        const val SCAN_STATUS = 0x83
        const val CONNECTED = 0x86
        const val DISCONNECTED = 0x88
        const val STATUS = 0x8B
        const val HISTORY = 0x8C
        const val FLAGS = 0x9A
    }
}
