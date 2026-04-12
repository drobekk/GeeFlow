package dev.drobek.geeflow.data.device.impl.controller

import co.touchlab.kermit.Logger
import dev.drobek.geeflow.data.device.model.DeviceState
import dev.drobek.geeflow.data.device.model.DeviceState.BrewStatus
import dev.drobek.geeflow.data.device.model.DeviceState.HeatingMode
import dev.drobek.geeflow.data.device.model.SmartScale

class WendougeeFrameParser(
    private val onStateUpdate: (DeviceState.() -> DeviceState) -> Unit,
    private val onScaleFound: ((SmartScale) -> Unit)? = null,
    private val onHeartbeat: (() -> Unit)? = null,
    private val shouldLogPolling: () -> Boolean = { false },
) {
    companion object {
        private const val TAG = "WendougeeController"
        private const val BLE_TRACE_TAG = "WendougeeBle"
        private const val BYTE_MASK = 0xFF
        private const val BYTE_SHIFT = 8
        private const val MODBUS_MIN_FRAME_SIZE = 3
        private const val MODBUS_FC_READ = 0x03
        private const val MODBUS_FC_STATUS = 0x01
        private const val TELEMETRY_BYTE_COUNT = 0x28
        private const val CONFIG_BYTE_COUNT = 0x4A
        private const val STATUS_MASK_PROFILE = 0x03
        private const val STATUS_MASK_MANUAL = 0x10
        private const val STATUS_MASK_CLEANING = 0x20
        private const val SENSOR_SCALE_FACTOR = 10f
        private const val HEX_RADIX = 16
        private val PROPRIETARY_PREFIX = byteArrayOf(0xFF.toByte(), 0x55.toByte(), 0xFF.toByte(), 0xFF.toByte())

        fun isProprietaryFrame(data: ByteArray): Boolean =
            data.size >= PROPRIETARY_PREFIX.size &&
                data.sliceArray(PROPRIETARY_PREFIX.indices).contentEquals(PROPRIETARY_PREFIX)

        fun isPollingFrame(data: ByteArray): Boolean {
            if (data.size < MODBUS_MIN_FRAME_SIZE) return false
            if (data[0] == 0x01.toByte()) {
                val fc = data[1].toInt() and BYTE_MASK
                val byteCount = data[2].toInt() and BYTE_MASK
                return fc == MODBUS_FC_STATUS || (fc == MODBUS_FC_READ && byteCount == TELEMETRY_BYTE_COUNT)
            }
            if (data.size >= FrameOffsets.POLLING_MIN_SIZE && isProprietaryFrame(data)) {
                return data[FrameOffsets.PROP_CMD].toInt() and BYTE_MASK == ProprietaryFrame.HEARTBEAT
            }
            return false
        }

        fun toHexString(arr: ByteArray): String = arr.joinToString("") {
            (it.toInt() and BYTE_MASK).toString(HEX_RADIX).padStart(2, '0')
        }

        fun u8(arr: ByteArray, i: Int): Int = arr[i].toInt() and BYTE_MASK
        fun u16be(arr: ByteArray, i: Int): Int = (u8(arr, i) shl BYTE_SHIFT) or u8(arr, i + 1)
    }

    private object FrameOffsets {
        const val PROP_CMD = 4
        const val PROP_LEN_HI = 5
        const val PROP_LEN_LO = 6
        const val PROP_DATA_START = 7
        const val PROP_MIN_PARSE_SIZE = 8
        const val POLLING_MIN_SIZE = 5
        const val STATUS_MIN_SIZE = 4
        const val STATUS_BYTE_OFFSET = 3
        const val SCALE_ACTIVE_FLAG = 0x04
    }

    fun handleIncomingFrame(data: ByteArray, channel: String) {
        val polling = isPollingFrame(data)
        if (!polling || shouldLogPolling()) {
            Logger.withTag(BLE_TRACE_TAG).v { "← [$channel] ${toHexString(data)} (${data.size}B)" }
        }
        if (isProprietaryFrame(data)) {
            parseProprietaryFrame(data)
            return
        }

        if (data.size < MODBUS_MIN_FRAME_SIZE) return
        val functionCode = data[1].toInt() and BYTE_MASK

        when (functionCode) {
            MODBUS_FC_READ -> {
                val byteCount = data[2].toInt() and BYTE_MASK
                if (byteCount == TELEMETRY_BYTE_COUNT) {
                    parseTelemetryFrame(data)
                } else if (byteCount == CONFIG_BYTE_COUNT) {
                    parseConfigFrame(data)
                }
            }
            MODBUS_FC_STATUS -> parseShortStatusFrame(data)
        }
    }

    private fun parseProprietaryFrame(payload: ByteArray) {
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
                ProprietaryFrame.HEARTBEAT -> onHeartbeat?.invoke()
                ProprietaryFrame.STATUS_RESPONSE -> parseStatusResponse(payload, length)
                ProprietaryFrame.SCALE_SEARCH_ECHO -> if (length >= 1) {
                    val active = payload[FrameOffsets.PROP_DATA_START].toInt() and BYTE_MASK == FrameOffsets.SCALE_ACTIVE_FLAG
                    Logger.withTag(TAG).i { "Scale search state (echo): ${if (active) "ON" else "OFF"}" }
                    onStateUpdate { copy(smartScaleEnabled = active) }
                }
                ProprietaryFrame.SERIAL_NUMBER -> Logger.withTag(TAG).d { "Serial number: $asciiString" }
                ProprietaryFrame.SCALE_FOUND -> parseScaleFound(asciiString)
                ProprietaryFrame.SCALE_LIST_RESPONSE -> parseScaleListResponse(payload, length)
                ProprietaryFrame.SCALE_ACTIVE -> parseScaleActive(asciiString)
                ProprietaryFrame.SCALE_CONNECTED -> parseScaleConnected(asciiString)
                ProprietaryFrame.SCALE_DISCONNECTED -> parseScaleDisconnected(asciiString)
            }
        } catch (e: IndexOutOfBoundsException) {
            Logger.withTag(TAG).e(e) { "Error parsing proprietary frame" }
        }
    }

    private fun parseStatusResponse(payload: ByteArray, length: Int) {
        if (length < 2 || payload.size < FrameOffsets.PROP_DATA_START + 2) return
        val slotIndex = payload[FrameOffsets.PROP_DATA_START].toInt() and BYTE_MASK
        val connectionStatus = payload[FrameOffsets.PROP_DATA_START + 1].toInt() and BYTE_MASK
        val nameLen = length - 2
        if (connectionStatus != 1 || nameLen <= 0 || payload.size < FrameOffsets.PROP_DATA_START + 2 + nameLen) return
        val nameStart = FrameOffsets.PROP_DATA_START + 2
        val name = payload.decodeToString(startIndex = nameStart, endIndex = nameStart + nameLen).trim()
        if (name.length <= 2) return
        Logger.withTag(TAG).i { "Scale status [slot $slotIndex]: $name (connected)" }
        val scale = SmartScale(name, isConnected = true)
        onScaleFound?.invoke(scale)
        onStateUpdate { copy(smartScale = scale) }
    }

    private fun parseScaleFound(asciiString: String) {
        val name = asciiString.trim().replace(Regex("[^\\x20-\\x7E]"), "")
        if (name.isEmpty() || name.length <= 2) return
        Logger.withTag(TAG).i { "Smart scale found: $name" }
        onScaleFound?.invoke(SmartScale(name, isConnected = false))
    }

    private fun parseScaleListResponse(payload: ByteArray, length: Int) {
        if (length < 2 || payload.size < FrameOffsets.PROP_DATA_START + 2) return
        val slotIndex = payload[FrameOffsets.PROP_DATA_START].toInt() and BYTE_MASK
        val nameLen = payload[FrameOffsets.PROP_DATA_START + 1].toInt() and BYTE_MASK
        if (nameLen <= 0 || payload.size < FrameOffsets.PROP_DATA_START + 2 + nameLen) return
        val nameStart = FrameOffsets.PROP_DATA_START + 2
        val name = payload.decodeToString(startIndex = nameStart, endIndex = nameStart + nameLen).trim()
        if (name.length <= 2) return
        Logger.withTag(TAG).i { "Scale list [slot $slotIndex]: $name" }
        onScaleFound?.invoke(SmartScale(name, isConnected = false))
    }

    private fun parseScaleActive(asciiString: String) {
        val name = asciiString.trim().replace(Regex("[^\\x20-\\x7E]"), "")
        if (name.isEmpty() || name.length <= 2) return
        Logger.withTag(TAG).i { "Smart scale connected: $name" }
        val scale = SmartScale(name, isConnected = true)
        onScaleFound?.invoke(scale)
        onStateUpdate { copy(smartScale = scale) }
    }

    private fun parseScaleConnected(asciiString: String) {
        val name = asciiString.trim().replace(Regex("[^\\x20-\\x7E]"), "")
        if (name.isEmpty() || name.length <= 2) return
        Logger.withTag(TAG).i { "Smart scale in active slot: $name" }
        onScaleFound?.invoke(SmartScale(name, isConnected = false))
    }

    private fun parseScaleDisconnected(asciiString: String) {
        val name = asciiString.trim().replace(Regex("[^\\x20-\\x7E]"), "")
        Logger.withTag(TAG).i { "Smart scale disconnected: $name" }
        if (name.isNotEmpty() && name.length > 2) {
            onScaleFound?.invoke(SmartScale(name, isConnected = false))
        }
        onStateUpdate { copy(smartScale = null) }
    }

    private fun parseConfigFrame(payload: ByteArray) {
        try {
            if (payload.size < ConfigFrame.MIN_HEADER_SIZE) return

            fun dataU16be(off: Int) = u16be(payload, ConfigFrame.DATA_START + off)

            val cleaningTimeSec = dataU16be(ConfigFrame.CLEANING_TIME) / SENSOR_SCALE_FACTOR
            val cleaningStandbySec = dataU16be(ConfigFrame.CLEANING_STANDBY) / SENSOR_SCALE_FACTOR
            val cleaningCount = dataU16be(ConfigFrame.CLEANING_COUNT)

            val isSteamBoilerEnabled = dataU16be(ConfigFrame.STEAM_BOILER_ENABLED) == 0
            val isBrewBoilerEnabled = dataU16be(ConfigFrame.BREW_BOILER_ENABLED) == 0

            val targetSteam = dataU16be(ConfigFrame.TARGET_STEAM_TEMP).toFloat()
            val targetBrew = dataU16be(ConfigFrame.TARGET_BREW_TEMP).toFloat()

            val manualBrewTimeSec = dataU16be(ConfigFrame.MANUAL_BREW_TIME) / SENSOR_SCALE_FACTOR
            val manualBrewPressure = dataU16be(ConfigFrame.MANUAL_BREW_PRESSURE) / SENSOR_SCALE_FACTOR

            val isFullSpeedHeating = dataU16be(ConfigFrame.HEATING_MODE) == 1
            val waterAlarm = dataU16be(ConfigFrame.WATER_ALARM) == 1

            Logger.withTag(TAG).i {
                "Config: brew=${targetBrew.toInt()}°C steam=${targetSteam.toInt()}°C" +
                    " | boilers: brew=${if (isBrewBoilerEnabled) "ON" else "OFF"} " +
                    "steam=${if (isSteamBoilerEnabled) "ON" else "OFF"}" +
                    " | heating=${if (isFullSpeedHeating) "FullSpeed" else "Pulse"}" +
                    " | manual=${manualBrewTimeSec}s@${manualBrewPressure}bar" +
                    " | cleaning=${cleaningTimeSec}s×${cleaningStandbySec}s×$cleaningCount" +
                    " | waterAlarm=$waterAlarm"
            }

            onStateUpdate {
                copy(
                    config = DeviceState.Config(
                        targetSteamTemp = targetSteam,
                        targetBrewTemp = targetBrew,
                        steamBoilerEnabled = isSteamBoilerEnabled,
                        brewBoilerEnabled = isBrewBoilerEnabled,
                        manualBrewTimeSec = manualBrewTimeSec,
                        manualBrewPressure = manualBrewPressure,
                        heatingMode = if (isFullSpeedHeating) HeatingMode.FullSpeed else HeatingMode.Pulse,
                        cleaningTimeSec = cleaningTimeSec,
                        cleaningStandbySec = cleaningStandbySec,
                        cleaningCount = cleaningCount,
                        waterAlarmEnabled = waterAlarm,
                    ),
                )
            }
        } catch (e: IndexOutOfBoundsException) {
            Logger.withTag(TAG).e(e) { "Config frame parser error" }
        }
    }

    private fun parseShortStatusFrame(payload: ByteArray) {
        if (payload.size < FrameOffsets.STATUS_MIN_SIZE) return
        val statusByte = payload[FrameOffsets.STATUS_BYTE_OFFSET].toInt() and BYTE_MASK
        val isProfile = (statusByte and STATUS_MASK_PROFILE) != 0
        val isManual = (statusByte and STATUS_MASK_MANUAL) != 0
        val isCleaning = (statusByte and STATUS_MASK_CLEANING) != 0

        val brewStatus = when {
            isManual -> BrewStatus.Manual
            isProfile -> BrewStatus.Profile
            isCleaning -> BrewStatus.Cleaning
            else -> BrewStatus.Idle
        }
        onStateUpdate { copy(brewStatus = brewStatus) }
    }

    private fun parseTelemetryFrame(payload: ByteArray) {
        try {
            if (payload.size < TelemetryFrame.MIN_HEADER_SIZE) return

            fun dataU16be(off: Int) = u16be(payload, TelemetryFrame.DATA_START + off)

            val pressure = dataU16be(TelemetryFrame.PRESSURE) / SENSOR_SCALE_FACTOR
            val weight = dataU16be(TelemetryFrame.WEIGHT) / SENSOR_SCALE_FACTOR
            val steamActual = dataU16be(TelemetryFrame.STEAM_TEMP) / SENSOR_SCALE_FACTOR
            val brewActual = dataU16be(TelemetryFrame.BREW_TEMP) / SENSOR_SCALE_FACTOR
            val weightRate = dataU16be(TelemetryFrame.WEIGHT_RATE) / SENSOR_SCALE_FACTOR
            val volume = dataU16be(TelemetryFrame.VOLUME).toFloat()
            val flowRate = dataU16be(TelemetryFrame.FLOW_RATE).toFloat()
            val time = dataU16be(TelemetryFrame.TIME)
            val waterLevelAlarm = dataU16be(TelemetryFrame.WATER_LEVEL_ALARM) != 0

            if (shouldLogPolling()) {
                Logger.withTag(TAG).i {
                    "Brew: $brewActual°C | Steam: $steamActual°C | Pressure: ${pressure}bar" +
                        " | Weight: ${weight}g (${weightRate}g/s) | Volume: ${volume}ml (${flowRate}ml/s) | " +
                        "Time: ${time}s | Water alarm: $waterLevelAlarm"
                }
            }

            onStateUpdate {
                copy(
                    steamBoilerTemp = steamActual,
                    brewBoilerTemp = brewActual,
                    pressure = pressure,
                    time = time,
                    volume = volume,
                    flowRate = flowRate,
                    weight = weight,
                    weightRate = weightRate,
                    waterLevelAlarm = waterLevelAlarm,
                )
            }
        } catch (e: IndexOutOfBoundsException) {
            Logger.withTag(TAG).e(e) { "Modbus telemetry parser error" }
        }
    }

    private object ProprietaryFrame {
        const val HEARTBEAT = 0x83
        const val STATUS_RESPONSE = 0x8B
        const val SCALE_SEARCH_ECHO = 0x9A
        const val SERIAL_NUMBER = 0x04
        const val SCALE_FOUND = 0x81
        const val SCALE_LIST_RESPONSE = 0x8C
        const val SCALE_ACTIVE = 0x80
        const val SCALE_CONNECTED = 0x86
        const val SCALE_DISCONNECTED = 0x88
    }

    private object ConfigFrame {
        const val DATA_START = 3
        const val CLEANING_TIME = 0
        const val CLEANING_STANDBY = 2
        const val CLEANING_COUNT = 4
        const val STEAM_BOILER_ENABLED = 12
        const val BREW_BOILER_ENABLED = 14
        const val TARGET_STEAM_TEMP = 16
        const val TARGET_BREW_TEMP = 18
        const val MANUAL_BREW_TIME = 34
        const val MANUAL_BREW_PRESSURE = 38
        const val HEATING_MODE = 44
        const val WATER_ALARM = 52
        const val MIN_HEADER_SIZE = 3 + 74 + 2
    }

    private object TelemetryFrame {
        const val DATA_START = 3
        const val TIME = 2
        const val WATER_LEVEL_ALARM = 4
        const val STEAM_TEMP = 8
        const val BREW_TEMP = 10
        const val PRESSURE = 12
        const val VOLUME = 14
        const val WEIGHT = 16
        const val FLOW_RATE = 36
        const val WEIGHT_RATE = 38
        const val MIN_HEADER_SIZE = 3 + 40 + 2
    }
}
