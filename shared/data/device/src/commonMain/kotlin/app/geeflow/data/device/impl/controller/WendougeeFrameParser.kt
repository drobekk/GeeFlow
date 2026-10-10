package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.ble.modbus.ModbusNotifications
import app.geeflow.data.device.model.DeviceError
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.DeviceState.BrewStatus
import app.geeflow.data.device.model.DeviceState.HeatingMode
import app.geeflow.data.device.model.SingleDoseGrinder
import app.geeflow.data.device.model.SmartScale
import co.touchlab.kermit.Logger
import kotlin.time.Clock

class WendougeeFrameParser(
    private val onStateUpdate: (DeviceState.() -> DeviceState) -> Unit,
    onGrinderFound: ((SingleDoseGrinder) -> Unit)? = null,
    onScaleFound: ((SmartScale) -> Unit)? = null,
    onScanStatus: ((Boolean) -> Unit)? = null,
    onIntegrationFlags: ((Int) -> Unit)? = null,
    private val shouldLogPolling: () -> Boolean = { false },
) {
    private val grinderParser = WendougeeSingleDoseParser(onStateUpdate, onGrinderFound)
    private val controlFrames = WendougeeControlFrames()
    private val modbusFrames = ModbusNotifications()
    private val scaleParser = WendougeeScaleParser(onStateUpdate, onScaleFound, onScanStatus, onIntegrationFlags)

    fun reset() {
        controlFrames.reset()
        modbusFrames.reset()
    }

    companion object {
        private const val TAG = "WendougeeController"
        private const val BLE_TRACE_TAG = "WendougeeBle"
        private const val BYTE_MASK = 0xFF
        private const val BYTE_SHIFT = 8
        private const val MODBUS_MIN_FRAME_SIZE = 3
        private const val MODBUS_FC_READ = 0x03
        private const val MODBUS_FC_STATUS = 0x01
        private const val TELEMETRY_BYTE_COUNT = 0x28
        private const val EXTENDED_TELEMETRY_BYTE_COUNT = 0x2C
        private const val CONFIG_BYTE_COUNT = 0x4A
        private const val STATUS_MASK_PROFILE_BYTE0 = 0x0F
        private const val STATUS_MASK_PROFILE_BYTE1 = 0x08
        private const val STATUS_MASK_MANUAL = 0x10
        private const val STATUS_MASK_CLEANING = 0x20
        private const val STATUS_MASK_WATER_FLOW = 0x40
        private const val STATUS_MASK_CLEANING_HIGH = 0x80
        private const val MAX_WEIGHT_RATE = 12f
        private const val MAX_STATUS_BYTES = 3
        private const val SENSOR_SCALE_FACTOR = 10f
        private const val SCAN_STATUS_COMMAND = 0x83
        private const val HEX_RADIX = 16
        private val PROPRIETARY_PREFIX = byteArrayOf(0xFF.toByte(), 0x55.toByte())

        fun isProprietaryFrame(data: ByteArray): Boolean =
            data.size >= PROPRIETARY_PREFIX.size &&
                data.sliceArray(PROPRIETARY_PREFIX.indices).contentEquals(PROPRIETARY_PREFIX)

        fun isPollingFrame(data: ByteArray): Boolean {
            if (data.size < MODBUS_MIN_FRAME_SIZE) return false
            if (data[0] == 0x01.toByte()) {
                val fc = data[1].toInt() and BYTE_MASK
                val byteCount = data[2].toInt() and BYTE_MASK
                val telemetry = byteCount == TELEMETRY_BYTE_COUNT || byteCount == EXTENDED_TELEMETRY_BYTE_COUNT
                return fc == MODBUS_FC_STATUS || (fc == MODBUS_FC_READ && telemetry)
            }
            if (data.size >= FrameOffsets.POLLING_MIN_SIZE && isProprietaryFrame(data)) {
                return data[FrameOffsets.PROP_CMD].toInt() and BYTE_MASK == SCAN_STATUS_COMMAND
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
        const val POLLING_MIN_SIZE = 5
        const val STATUS_BYTE_OFFSET = 3
        const val STATUS_BYTE2_OFFSET = 4
    }

    fun handleIncomingFrame(data: ByteArray, channel: String) {
        val polling = isPollingFrame(data)
        if (!polling || shouldLogPolling()) {
            Logger.withTag(BLE_TRACE_TAG).v { "<< [$channel] ${toHexString(data)} (${data.size}B)" }
        }
        if (channel == "CTRL") {
            controlFrames.receive(data).forEach { frame ->
                grinderParser.parse(frame)
                scaleParser.parse(frame)
            }
            return
        }

        modbusFrames.receive(data).forEach(::parseModbusFrame)
    }

    private fun parseModbusFrame(data: ByteArray) {
        if (u8(data, 0) != 1) return
        val functionCode = u8(data, 1)

        when (functionCode) {
            MODBUS_FC_READ -> {
                val byteCount = data[2].toInt() and BYTE_MASK
                if (byteCount == TELEMETRY_BYTE_COUNT || byteCount == EXTENDED_TELEMETRY_BYTE_COUNT) {
                    parseTelemetryFrame(data)
                } else if (byteCount == CONFIG_BYTE_COUNT) {
                    parseConfigFrame(data)
                }
            }

            MODBUS_FC_STATUS -> parseShortStatusFrame(data)
        }
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

            Logger.withTag(TAG).i {
                "Config: brew=${targetBrew.toInt()}°C steam=${targetSteam.toInt()}°C" +
                    " | boilers: brew=${if (isBrewBoilerEnabled) "ON" else "OFF"} " +
                    "steam=${if (isSteamBoilerEnabled) "ON" else "OFF"}" +
                    " | heating=${if (isFullSpeedHeating) "FullSpeed" else "Pulse"}" +
                    " | manual=${manualBrewTimeSec}s@${manualBrewPressure}bar" +
                    " | cleaning=${cleaningTimeSec}s×${cleaningStandbySec}s×$cleaningCount"
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
                        waterAlarmEnabled = config?.waterAlarmEnabled ?: false, // Read separately from register 396
                    ),
                )
            }
        } catch (e: IndexOutOfBoundsException) {
            Logger.withTag(TAG).e(e) { "Config frame parser error" }
        }
    }

    private fun parseShortStatusFrame(payload: ByteArray) {
        val byteCount = u8(payload, 2)
        if (byteCount !in 1..MAX_STATUS_BYTES) return
        val statusByte0 = payload[FrameOffsets.STATUS_BYTE_OFFSET].toInt() and BYTE_MASK
        val statusByte1 = if (byteCount >= 2) u8(payload, FrameOffsets.STATUS_BYTE2_OFFSET) else 0
        val isManual = (statusByte0 and STATUS_MASK_MANUAL) != 0
        val isCleaning = (statusByte0 and STATUS_MASK_CLEANING) != 0
        val isProfile = (statusByte0 and STATUS_MASK_PROFILE_BYTE0) != 0
        val isFreeVariable = (statusByte1 and STATUS_MASK_PROFILE_BYTE1) != 0
        val isWaterFlow = (statusByte0 and STATUS_MASK_WATER_FLOW) != 0
        val isCleaningHigh = (statusByte0 and STATUS_MASK_CLEANING_HIGH) != 0

        val brewStatus = when {
            // The extended status identifies free hand even when a general activity bit is also set.
            isFreeVariable -> BrewStatus.FreeVariable
            isProfile -> BrewStatus.Profile
            isManual -> BrewStatus.Manual
            isCleaning -> BrewStatus.Cleaning
            isWaterFlow -> BrewStatus.WaterFlow
            isCleaningHigh -> BrewStatus.Cleaning
            else -> BrewStatus.Idle
        }
        onStateUpdate { copy(brewStatus = brewStatus, statusTime = Clock.System.now()) }
    }

    private fun parseTelemetryFrame(payload: ByteArray) {
        try {
            if (payload.size < TelemetryFrame.MIN_HEADER_SIZE) return

            fun dataU16be(off: Int) = u16be(payload, TelemetryFrame.DATA_START + off)

            val pressure = dataU16be(TelemetryFrame.PRESSURE) / SENSOR_SCALE_FACTOR
            val weight = dataU16be(TelemetryFrame.WEIGHT) / SENSOR_SCALE_FACTOR
            val steamActual = dataU16be(TelemetryFrame.STEAM_TEMP) / SENSOR_SCALE_FACTOR
            val brewActual = dataU16be(TelemetryFrame.BREW_TEMP) / SENSOR_SCALE_FACTOR
            val weightRate = (dataU16be(TelemetryFrame.WEIGHT_RATE) / SENSOR_SCALE_FACTOR).coerceAtMost(MAX_WEIGHT_RATE)
            val volume = dataU16be(TelemetryFrame.VOLUME).toFloat()
            val flowRate = dataU16be(TelemetryFrame.FLOW_RATE).toFloat()
            val time = dataU16be(TelemetryFrame.TIME)
            val error = DeviceError.fromCode(dataU16be(TelemetryFrame.ERROR_CODE))
            val waterLevelAlarm = error?.isWaterAlarm == true

            if (shouldLogPolling()) {
                Logger.withTag(TAG).i {
                    "Brew: $brewActual°C | Steam: $steamActual°C | Pressure: ${pressure}bar" +
                        " | Weight: ${weight}g (${weightRate}g/s) | Volume: ${volume}ml (${flowRate}ml/s) | " +
                        "Time: ${time}s | Error code: ${error?.code ?: 0}"
                }
            }

            onStateUpdate {
                copy(
                    telemetryTime = Clock.System.now(),
                    steamBoilerTemp = steamActual,
                    brewBoilerTemp = brewActual,
                    pressure = pressure,
                    time = time,
                    volume = volume,
                    flowRate = flowRate,
                    weight = weight,
                    weightRate = weightRate,
                    waterLevelAlarm = waterLevelAlarm,
                    error = error,
                )
            }
        } catch (e: IndexOutOfBoundsException) {
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
        const val MIN_HEADER_SIZE = 3 + 74 + 2
    }

    private object TelemetryFrame {
        const val DATA_START = 3
        const val TIME = 2
        const val ERROR_CODE = 4 // Register 1406, index 2 of telemetry starting at 1404
        const val STEAM_TEMP = 8
        const val BREW_TEMP = 10
        const val PRESSURE = 12
        const val VOLUME = 14
        const val WEIGHT = 22 // Register 1415, index 11 of telemetry starting at 1404
        const val FLOW_RATE = 36
        const val WEIGHT_RATE = 38
        const val MIN_HEADER_SIZE = 3 + 40 + 2
    }
}
