package dev.drobek.geeflow.data.device.impl.controller

import co.touchlab.kermit.Logger
import dev.drobek.geeflow.domain.device.model.MachineState
import dev.drobek.geeflow.domain.device.model.MachineState.BrewStatus
import dev.drobek.geeflow.domain.device.model.MachineState.HeatingMode
import dev.drobek.geeflow.domain.device.model.SmartScale

class WendougeeFrameParser(
    private val onStateUpdate: (MachineState.() -> MachineState) -> Unit,
    private val onScaleFound: ((SmartScale) -> Unit)? = null
) {
    companion object {
        private const val TAG = "WendougeeFrameParser"
    }

    fun handleIncomingFrame(data: ByteArray) {
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

            if (command == 0x04) {
                Logger.withTag(TAG).i { "Received Serial Number: $asciiString" }
            } else if (command == 0x8C || command == 0x81) {
                // 0x8C = Scale found during search, 0x81 = Scale connected
                val name = asciiString.trim().replace(Regex("[^\\x20-\\x7E]"), "")
                if (name.isNotEmpty() && name.length > 2) {
                    val scale = SmartScale(name, isConnected = command == 0x81)
                    if (command == 0x81) {
                        Logger.withTag(TAG).i { "Smart scale connected: $name" }
                        onStateUpdate { copy(connectedScale = scale) }
                    } else {
                        Logger.withTag(TAG).i { "Found Smart Scale: $name" }
                        onScaleFound?.invoke(scale)
                    }
                }
            } else if (command == 0x8B) {
                 if (payload.size >= 10 && payload[7] == 0x02.toByte() && payload[8] == 0x00.toByte() && payload[9] == 0x00.toByte()) {
                     // Disconnect or Search Off ACK
                     onStateUpdate { copy(connectedScale = null) }
                 }
            } else if (asciiString.contains("BOOKOO")) {
                Logger.withTag(TAG).i { "Connected Smart Scale recognized (legacy/other): $asciiString" }
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

            onStateUpdate {
                copy(
                    config = MachineState.Config(
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
                        waterAlarm = waterAlarm
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

            onStateUpdate {
                copy(
                    steamBoilerTemp = steamActual,
                    brewBoilerTemp = brewActual,
                    pressure = pressure,
                    time = time,
                    volume = volume,
                    flowRate = flowRate,
                    weight = weight,
                    weightRate = weightRate
                )
            }
        } catch (e: Exception) {
            Logger.withTag(TAG).e(e) { "Modbus telemetry parser error" }
        }
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
        const val STEAM_TEMP = 8
        const val BREW_TEMP = 10
        const val PRESSURE = 12

        const val VOLUME = 14
        const val WEIGHT = 16

        const val FLOW_RATE = 36
        const val WEIGHT_RATE = 38

        const val MIN_HEADER_SIZE = 3 + 40 + 2
    }

    private fun ByteArray.u8(i: Int): Int = this[i].toInt() and 0xFF
    private fun ByteArray.u16be(i: Int): Int = (u8(i) shl 8) or u8(i + 1)
}
