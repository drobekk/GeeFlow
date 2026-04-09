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
    private val shouldLogPolling: () -> Boolean = { false }
) {
    companion object {
        private const val TAG = "WendougeeController"
        private const val BLE_TRACE_TAG = "WendougeeBle"
    }

    fun handleIncomingFrame(data: ByteArray, channel: String) {
        val isPolling = data.isPollingFrame()
        if (!isPolling || shouldLogPolling()) {
            Logger.withTag(BLE_TRACE_TAG).v { "← [$channel] ${data.toHexString()} (${data.size}B)" }
        }
        // Proprietary Wendougee frame prefix (FF 55 FF FF) used for non-Modbus messages like Serial Number or Smart Scale
        if (data.size >= 4 && data[0] == 0xFF.toByte() && data[1] == 0x55.toByte() && data[2] == 0xFF.toByte() && data[3] == 0xFF.toByte()) {
            parseProprietaryFrame(data)
            return
        }

        if (data.size < 3) return
        val functionCode = data[1].toInt() and 0xFF

        when (functionCode) {
            0x03 -> {
                val byteCount = data[2].toInt() and 0xFF
                if (byteCount == 0x28) {
                    parseTelemetryFrame(data)
                } else if (byteCount == 0x4A) {
                    parseConfigFrame(data)
                }
            }

            0x01 -> parseShortStatusFrame(data)
        }
    }

    private fun parseProprietaryFrame(payload: ByteArray) {
        try {
            if (payload.size < 8) return
            val command = payload[4].toInt() and 0xFF
            val length = (payload[5].toInt() and 0xFF shl 8) or (payload[6].toInt() and 0xFF)

            if (payload.size < 7 + length) return

            val safeEndIndex = minOf(7 + length, payload.size)
            val asciiString = payload.decodeToString(startIndex = 7, endIndex = safeEndIndex)

            when (command) {
                ProprietaryFrame.HEARTBEAT -> onHeartbeat?.invoke()
                ProprietaryFrame.STATUS_RESPONSE -> {
                    // Status response: [slot_index, connection_status, name(len-2 bytes)]
                    // connection_status=1 means scale is actively BLE-connected
                    if (length >= 2 && payload.size >= 9) {
                        val slotIndex = payload[7].toInt() and 0xFF
                        val connectionStatus = payload[8].toInt() and 0xFF
                        val nameLen = length - 2
                        if (connectionStatus == 1 && nameLen > 0 && payload.size >= 9 + nameLen) {
                            val name = payload.decodeToString(startIndex = 9, endIndex = 9 + nameLen).trim()
                            if (name.length > 2) {
                                Logger.withTag(TAG).i { "Scale status [slot $slotIndex]: $name (connected)" }
                                val scale = SmartScale(name, isConnected = true)
                                onScaleFound?.invoke(scale)
                                onStateUpdate { copy(smartScale = scale) }
                            }
                        }
                    }
                }

                ProprietaryFrame.SCALE_SEARCH_ECHO -> {
                    if (length >= 1) {
                        val active = payload[7].toInt() and 0xFF == 0x04
                        Logger.withTag(TAG).i { "Scale search state (echo): ${if (active) "ON" else "OFF"}" }
                        onStateUpdate { copy(smartScaleEnabled = active) }
                    }
                }

                ProprietaryFrame.SERIAL_NUMBER -> Logger.withTag(TAG).d { "Serial number: $asciiString" }
                ProprietaryFrame.SCALE_FOUND -> {
                    // Scale found during active BLE scan
                    val name = asciiString.trim().replace(Regex("[^\\x20-\\x7E]"), "")
                    if (name.isNotEmpty() && name.length > 2) {
                        Logger.withTag(TAG).i { "Smart scale found: $name" }
                        onScaleFound?.invoke(SmartScale(name, isConnected = false))
                    }
                }

                ProprietaryFrame.SCALE_LIST_RESPONSE -> {
                    // List response: [slot_index, name_len, name_bytes]
                    // Slot contains remembered scales — connection state comes from 0x80
                    if (length >= 2 && payload.size >= 9) {
                        val slotIndex = payload[7].toInt() and 0xFF
                        val nameLen = payload[8].toInt() and 0xFF
                        if (nameLen > 0 && payload.size >= 9 + nameLen) {
                            val name = payload.decodeToString(startIndex = 9, endIndex = 9 + nameLen).trim()
                            if (name.length > 2) {
                                Logger.withTag(TAG).i { "Scale list [slot $slotIndex]: $name" }
                                onScaleFound?.invoke(SmartScale(name, isConnected = false))
                            }
                        }
                    }
                }

                ProprietaryFrame.SCALE_ACTIVE -> {
                    // Scale is actively BLE-connected to the machine
                    val name = asciiString.trim().replace(Regex("[^\\x20-\\x7E]"), "")
                    if (name.isNotEmpty() && name.length > 2) {
                        Logger.withTag(TAG).i { "Smart scale connected: $name" }
                        val scale = SmartScale(name, isConnected = true)
                        onScaleFound?.invoke(scale)
                        onStateUpdate { copy(smartScale = scale) }
                    }
                }

                ProprietaryFrame.SCALE_CONNECTED -> {
                    // Scale is in the machine's active slot (remembered, not necessarily connected)
                    val name = asciiString.trim().replace(Regex("[^\\x20-\\x7E]"), "")
                    if (name.isNotEmpty() && name.length > 2) {
                        Logger.withTag(TAG).i { "Smart scale in active slot: $name" }
                        onScaleFound?.invoke(SmartScale(name, isConnected = false))
                    }
                }

                ProprietaryFrame.SCALE_DISCONNECTED -> {
                    val name = asciiString.trim().replace(Regex("[^\\x20-\\x7E]"), "")
                    Logger.withTag(TAG).i { "Smart scale disconnected: $name" }
                    if (name.isNotEmpty() && name.length > 2) {
                        onScaleFound?.invoke(SmartScale(name, isConnected = false))
                    }
                    onStateUpdate { copy(smartScale = null) }
                }
            }
        } catch (e: Exception) {
            Logger.withTag(TAG).e(e) { "Error parsing proprietary frame" }
        }
    }

    private fun parseConfigFrame(payload: ByteArray) {
        try {
            if (payload.size < ConfigFrame.MIN_HEADER_SIZE) return

            fun dataU16be(off: Int) = payload.u16be(ConfigFrame.DATA_START + off)

            val cleaningTimeSec = dataU16be(ConfigFrame.CLEANING_TIME) / 10f
            val cleaningStandbySec = dataU16be(ConfigFrame.CLEANING_STANDBY) / 10f
            val cleaningCount = dataU16be(ConfigFrame.CLEANING_COUNT)

            val isSteamBoilerEnabled = dataU16be(ConfigFrame.STEAM_BOILER_ENABLED) == 0
            val isBrewBoilerEnabled = dataU16be(ConfigFrame.BREW_BOILER_ENABLED) == 0

            val targetSteam = dataU16be(ConfigFrame.TARGET_STEAM_TEMP).toFloat()
            val targetBrew = dataU16be(ConfigFrame.TARGET_BREW_TEMP).toFloat()

            val manualBrewTimeSec = dataU16be(ConfigFrame.MANUAL_BREW_TIME) / 10f
            val manualBrewPressure = dataU16be(ConfigFrame.MANUAL_BREW_PRESSURE) / 10f

            val isFullSpeedHeating = dataU16be(ConfigFrame.HEATING_MODE) == 1
            val waterAlarm = dataU16be(ConfigFrame.WATER_ALARM) == 1

            Logger.withTag(TAG).i {
                "Config: brew=${targetBrew.toInt()}°C steam=${targetSteam.toInt()}°C" +
                        " | boilers: brew=${if (isBrewBoilerEnabled) "ON" else "OFF"} steam=${if (isSteamBoilerEnabled) "ON" else "OFF"}" +
                        " | heating=${if (isFullSpeedHeating) "FullSpeed" else "Pulse"}" +
                        " | manual=${manualBrewTimeSec}s@${manualBrewPressure}bar" +
                        " | cleaning=${cleaningTimeSec}s×${cleaningStandbySec}s×${cleaningCount}" +
                        " | waterAlarm=${waterAlarm}"
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
                        waterAlarmEnabled = waterAlarm
                    )
                )
            }
        } catch (e: Exception) {
            Logger.withTag(TAG).e(e) { "Config frame parser error" }
        }
    }

    private fun parseShortStatusFrame(payload: ByteArray) {
        if (payload.size < 4) return
        val statusByte = payload[3].toInt() and 0xFF
        val isProfile = (statusByte and 0x03) != 0
        val isManual = (statusByte and 0x10) != 0
        val isCleaning = (statusByte and 0x20) != 0

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

            fun dataU16be(off: Int) = payload.u16be(TelemetryFrame.DATA_START + off)

            val pressure = dataU16be(TelemetryFrame.PRESSURE) / 10f
            val weight = dataU16be(TelemetryFrame.WEIGHT) / 10f
            val steamActual = dataU16be(TelemetryFrame.STEAM_TEMP) / 10f
            val brewActual = dataU16be(TelemetryFrame.BREW_TEMP) / 10f
            val weightRate = dataU16be(TelemetryFrame.WEIGHT_RATE) / 10f
            val volume = dataU16be(TelemetryFrame.VOLUME).toFloat()
            val flowRate = dataU16be(TelemetryFrame.FLOW_RATE).toFloat()
            val time = dataU16be(TelemetryFrame.TIME)
            val waterLevelAlarm = dataU16be(TelemetryFrame.WATER_LEVEL_ALARM) != 0

            if (shouldLogPolling()) {
                Logger.withTag(TAG).i {
                    "Brew: ${brewActual}°C | Steam: ${steamActual}°C | Pressure: ${pressure}bar" +
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
                    waterLevelAlarm = waterLevelAlarm
                )
            }
        } catch (e: Exception) {
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

    private fun ByteArray.isPollingFrame(): Boolean {
        if (size < 3) return false
        if (this[0] == 0x01.toByte()) {
            return this[1] == 0x01.toByte() || (this[1] == 0x03.toByte() && this[2] == 0x28.toByte())
        }
        if (size >= 5 && this[0] == 0xFF.toByte() && this[1] == 0x55.toByte()) {
            val cmd = this[4].toInt() and 0xFF
            return cmd == ProprietaryFrame.HEARTBEAT
        }
        return false
    }

    private fun ByteArray.toHexString() = joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    private fun ByteArray.u8(i: Int): Int = this[i].toInt() and 0xFF
    private fun ByteArray.u16be(i: Int): Int = (u8(i) shl 8) or u8(i + 1)
}
