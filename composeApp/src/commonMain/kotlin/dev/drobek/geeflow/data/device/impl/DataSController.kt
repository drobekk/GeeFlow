@file:OptIn(ExperimentalUuidApi::class)

package dev.drobek.geeflow.data.device.impl

import co.touchlab.kermit.Logger
import dev.bluefalcon.AdvertisementDataRetrievalKeys
import dev.bluefalcon.BlueFalcon
import dev.bluefalcon.BlueFalconDelegate
import dev.bluefalcon.BluetoothCharacteristic
import dev.bluefalcon.BluetoothPeripheral
import dev.drobek.geeflow.data.device.api.DeviceController
import dev.drobek.geeflow.domain.brew.model.BrewProfile
import dev.drobek.geeflow.domain.brew.model.Condition
import dev.drobek.geeflow.domain.brew.model.ProfileMode
import dev.drobek.geeflow.domain.brew.model.ProfileStep
import dev.drobek.geeflow.domain.device.model.MachineState
import dev.drobek.geeflow.domain.device.model.MachineState.BoilerType
import dev.drobek.geeflow.domain.device.model.MachineState.BrewStatus
import dev.drobek.geeflow.domain.device.model.MachineState.ConnectionStatus
import dev.drobek.geeflow.domain.device.model.MachineState.HeatingMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.koin.core.annotation.Singleton
import kotlin.uuid.ExperimentalUuidApi

@Singleton
class DataSController(
    private val scope: CoroutineScope,
    private val blueFalcon: BlueFalcon
) : DeviceController, BlueFalconDelegate {

    private val _machineState = MutableStateFlow(MachineState())
    override val machineState: StateFlow<MachineState> = _machineState.asStateFlow()

    private val incomingFrames = MutableSharedFlow<ByteArray>(extraBufferCapacity = 20)
    private val bleMutex = Mutex()

    private var targetMacAddress: String? = null
    private var connectedPeripheral: BluetoothPeripheral? = null
    private var isConnectingToGatt = false
    private var pollingJob: Job? = null

    private var writeChar: BluetoothCharacteristic? = null
    private var notifyChar: BluetoothCharacteristic? = null

    private var connectionTimeoutJob: Job? = null

    companion object {
        private const val TAG = "WendougeeController"
        private val hexPattern = Regex("([a-fA-F0-9]{12})$")

        private const val REG_STEAM_STATE = 0x0006
        private const val REG_BREW_STATE = 0x0007

        // Init and configuration
        private val CMD_INIT_HANDSHAKE = "ff55ffff9a000104f2".decodeHex()
        private val CMD_READ_CONFIG_LONG = "0103000000258411".decodeHex()

        // Telemetry polling
        private val CMD_POLLING_LONG = "0103057C001484D1".decodeHex()
        private val CMD_POLLING_SHORT = "010100B600079C2E".decodeHex()

        private val CMD_MANUAL_ON = "0105009aff00ac15".decodeHex()
        private val CMD_MANUAL_OFF = "0105009a0000ede5".decodeHex()

        private val CMD_SHORT_PRESS_ON = "01050096ff006c16".decodeHex()
        private val CMD_SHORT_PRESS_OFF = "0105009600002de6".decodeHex()

        private val CMD_CLEANING_ON = "0105009bff00fdd5".decodeHex()
        private val CMD_CLEANING_OFF = "0105009b0000bc25".decodeHex()

        private const val DATA_UUID_SUFFIX = "2b10"
        private const val CTRL_UUID_SUFFIX = "2c10"

        private const val CONNECTION_TIMEOUT_MS = 10000L
        private const val POLLING_INTERVAL_MS = 200L
        private const val BREW_PULSE_MS = 100L

        private fun String.decodeHex(): ByteArray {
            check(length % 2 == 0) { "Must have an even length" }
            return chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
        }
    }

    init {
        blueFalcon.delegates.add(this)
    }

    private fun startDeviceInitialization(peripheral: BluetoothPeripheral) {
        Logger.withTag(TAG).i { "Starting device initialization sequence" }
        pollingJob?.cancel()

        pollingJob = scope.launch {
            Logger.withTag(TAG).d { "Negotiating MTU (512)" }
            blueFalcon.changeMTU(peripheral, 512)
            delay(500)

            try {
                notifyChar?.let { ctrlChar ->
                    Logger.withTag(TAG).d { "Sending proprietary INIT handshake" }
                    bleMutex.withLock {
                        blueFalcon.writeCharacteristicWithoutEncoding(peripheral, ctrlChar, CMD_INIT_HANDSHAKE, 2)
                    }
                }

                delay(300)

                writeChar?.let { dataChar ->
                    Logger.withTag(TAG).d { "Requesting static configuration" }
                    bleMutex.withLock {
                        blueFalcon.writeCharacteristicWithoutEncoding(peripheral, dataChar, CMD_READ_CONFIG_LONG, 2)
                    }
                }

                delay(500)

            } catch (e: Exception) {
                Logger.withTag(TAG).e(e) { "Initialization sequence failed, proceeding to telemetry loop" }
            }

            while (isActive) {
                pollData(peripheral)
                delay(POLLING_INTERVAL_MS)
            }
        }
    }

    private suspend fun pollData(peripheral: BluetoothPeripheral) {
        writeChar?.let { char ->
            bleMutex.withLock {
                try {
                    withTimeout(400) {
                        val ackDeferred = async {
                            incomingFrames.first { data ->
                                data.size >= 3 && data[1] == 0x03.toByte() && data[2] == 0x28.toByte()
                            }
                        }
                        yield()
                        logFrame(Direction.TX, char, CMD_POLLING_LONG, note = "poll-long")
                        blueFalcon.writeCharacteristicWithoutEncoding(peripheral, char, CMD_POLLING_LONG, 2)
                        ackDeferred.await()
                    }
                } catch (e: Exception) {
                    Logger.withTag(TAG).v { "Poll Long missed (timeout or error)" }
                }
            }
            delay(30)
            bleMutex.withLock {
                try {
                    withTimeout(400) {
                        val ackDeferred = async {
                            incomingFrames.first { data -> data.size >= 2 && data[1] == 0x01.toByte() }
                        }
                        yield()
                        logFrame(Direction.TX, char, CMD_POLLING_SHORT, note = "poll-short")
                        blueFalcon.writeCharacteristicWithoutEncoding(peripheral, char, CMD_POLLING_SHORT, 2)
                        ackDeferred.await()
                    }
                } catch (e: Exception) {
                    Logger.withTag(TAG).v { "Poll Short missed (timeout or error)" }
                }
            }
            delay(20)
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") {
        it.toInt().and(0xFF).toString(16).padStart(2, '0').lowercase()
    }

    /**
     * Sends a command safely wrapped in a Mutex, and suspends until the exact Modbus ACK is received.
     */
    private suspend fun writeAndAwaitModbus(
        char: BluetoothCharacteristic,
        payload: ByteArray,
        expectedFc: Byte,
        expectedRegHi: Byte,
        expectedRegLo: Byte,
        timeoutMs: Long = 1000L
    ): Boolean {
        val peripheral = connectedPeripheral ?: return false
        return bleMutex.withLock {
            try {
                withTimeout(timeoutMs) {
                    val ackDeferred = async {
                        incomingFrames.first { data ->
                            data.size >= 4 &&
                                    data[1] == expectedFc &&
                                    data[2] == expectedRegHi &&
                                    data[3] == expectedRegLo
                        }
                    }

                    yield()

                    blueFalcon.writeCharacteristicWithoutEncoding(peripheral, char, payload, 2)

                    ackDeferred.await()
                    true
                }
            } catch (e: TimeoutCancellationException) {
                Logger.withTag(TAG).w { "Timeout waiting for ACK: FC=$expectedFc Reg=$expectedRegHi$expectedRegLo" }
                false
            } catch (e: Exception) {
                Logger.withTag(TAG).e(e) { "Error sending command" }
                false
            }
        }
    }

    override fun connect(macAddress: String) {
        if (_machineState.value.connectionStatus != ConnectionStatus.Disconnected) {
            Logger.withTag(TAG).w { "Already connected or connecting, ignoring request for $macAddress" }
            return
        }
        isConnectingToGatt = false
        targetMacAddress = macAddress
        Logger.withTag(TAG).i { "Starting scan for target device: $macAddress" }

        _machineState.update { it.copy(connectionStatus = ConnectionStatus.Connecting) }

        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = scope.launch {
            delay(CONNECTION_TIMEOUT_MS)
            if (_machineState.value.connectionStatus == ConnectionStatus.Connecting) {
                Logger.withTag(TAG).w { "Connection timeout reached (10s)" }
                blueFalcon.stopScanning()
                connectedPeripheral?.let { blueFalcon.disconnect(it) }
                _machineState.update { it.copy(connectionStatus = ConnectionStatus.Disconnected) }
            }
        }

        blueFalcon.scan()
    }

    override fun disconnect() {
        connectionTimeoutJob?.cancel()
        connectedPeripheral?.let {
            Logger.withTag(TAG).i { "Initiating disconnect from ${it.uuid}" }
            blueFalcon.disconnect(it)
        }
    }

    override suspend fun setBoilerState(boilerType: BoilerType, enabled: Boolean) {
        val char = writeChar ?: return

        val targetRegister = if (boilerType == BoilerType.Steam) REG_STEAM_STATE else REG_BREW_STATE
        val stateValue = if (enabled) 0x00 else 0x01 // Active-Low logic: 0x00 = ON, 0x01 = OFF

        val header = byteArrayOf(
            0x01, 0x10,
            (targetRegister ushr 8).toByte(), (targetRegister and 0xFF).toByte(),
            0x00, 0x01,
            0x02,
            0x00, stateValue.toByte()
        )
        val fullModbus = header + calculateCRC(header)

        try {
            val success = writeAndAwaitModbus(
                char, fullModbus,
                expectedFc = 0x10,
                expectedRegHi = (targetRegister ushr 8).toByte(),
                expectedRegLo = (targetRegister and 0xFF).toByte()
            )

            if (success) {
                Logger.withTag(TAG)
                    .i { "Boiler ${if (boilerType == BoilerType.Steam) "Steam" else "Brew"} set to $enabled confirmed" }
                _machineState.update { currentState ->
                    val currentConfig = currentState.config ?: return@update currentState
                    val newConfig = if (boilerType == BoilerType.Steam) {
                        currentConfig.copy(steamBoilerEnabled = enabled)
                    } else {
                        currentConfig.copy(brewBoilerEnabled = enabled)
                    }
                    currentState.copy(config = newConfig)
                }
            } else {
                Logger.withTag(TAG).e { "Failed to receive Modbus ACK for Boiler State change" }
            }
        } catch (e: Exception) {
            Logger.withTag(TAG).e(e) { "Error toggling boiler state" }
        }
    }

    override suspend fun setSteamTemperature(temp: Int) {
        val char = writeChar ?: return

        if (temp !in 0..140) {
            Logger.withTag(TAG).e { "Temperature $temp out of safe range!" }
            return
        }

        val header = byteArrayOf(
            0x01,
            0x10,
            0x00, 0x08,
            0x00, 0x01,
            0x02,
            (temp ushr 8).toByte(),
            (temp and 0xFF).toByte()
        )

        val fullCommand = header + calculateCRC(header)

        val success = writeAndAwaitModbus(char, fullCommand, 0x10, 0x00, 0x08)
        if (success) {
            Logger.withTag(TAG).i { "Steam temperature set to $temp°C confirmed" }

            _machineState.update { currentState ->
                val currentConfig = currentState.config ?: return@update currentState
                currentState.copy(config = currentConfig.copy(targetSteamTemp = temp.toFloat()))
            }
        }
    }

    override suspend fun setBrewTemperature(temp: Int) {
        val char = writeChar ?: return

        if (temp !in 0..110) {
            Logger.withTag(TAG).e { "Brew temperature $temp out of range!" }
            return
        }

        val header = byteArrayOf(
            0x01,
            0x10,
            0x00, 0x09,
            0x00, 0x01,
            0x02,
            (temp ushr 8).toByte(),
            (temp and 0xFF).toByte()
        )

        val fullCommand = header + calculateCRC(header)

        val success = writeAndAwaitModbus(char, fullCommand, 0x10, 0x00, 0x09)
        if (success) {
            Logger.withTag(TAG).i { "Brew temperature set to $temp°C confirmed" }

            _machineState.update { currentState ->
                val currentConfig = currentState.config ?: return@update currentState
                currentState.copy(config = currentConfig.copy(targetBrewTemp = temp.toFloat()))
            }
        }
    }

    override suspend fun startManualBrewing() {
        if (_machineState.value.brewStatus == BrewStatus.Idle) {
            Logger.withTag(TAG).i { "Starting manual brew cycle..." }
            sendModbusPulse(
                onCommand = CMD_MANUAL_ON,
                offCommand = CMD_MANUAL_OFF,
                label = "Manual Brew",
                regHi = 0x00,
                regLo = 0x9A.toByte()
            )
        } else {
            Logger.withTag(TAG).w { "Machine is not idle, ignoring manual brew request." }
        }
    }

    override suspend fun stopManualBrewing() {
        if (_machineState.value.brewStatus == BrewStatus.Manual) {
            Logger.withTag(TAG).i { "Stopping manual brew cycle..." }
            sendModbusPulse(
                onCommand = CMD_MANUAL_ON,
                offCommand = CMD_MANUAL_OFF,
                label = "Manual Brew",
                regHi = 0x00,
                regLo = 0x9A.toByte()
            )
        } else {
            Logger.withTag(TAG).w { "Machine is not in manual brew, ignoring stop request." }
        }
    }

    override suspend fun triggerShortPress() {
        sendModbusPulse(
            onCommand = CMD_SHORT_PRESS_ON,
            offCommand = CMD_SHORT_PRESS_OFF,
            label = "Short Press",
            regHi = 0x00,
            regLo = 0x96.toByte()
        )
    }

    override suspend fun startCleaning() {
        if (_machineState.value.brewStatus == BrewStatus.Idle) {
            Logger.withTag(TAG).i { "Sending start signal for cleaning..." }
            sendModbusPulse(
                onCommand = CMD_CLEANING_ON,
                offCommand = CMD_CLEANING_OFF,
                label = "Cleaning Procedure",
                regHi = 0x00,
                regLo = 0x9B.toByte()
            )
        } else {
            Logger.withTag(TAG).w { "Machine is idle, ignoring start request." }
        }
    }

    override suspend fun stopCleaning() {
        if (_machineState.value.brewStatus == BrewStatus.Cleaning) {
            Logger.withTag(TAG).i { "Sending stop signal for cleaning..." }
            sendModbusPulse(
                onCommand = CMD_CLEANING_ON,
                offCommand = CMD_CLEANING_OFF,
                label = "Stop Cleaning",
                regHi = 0x00,
                regLo = 0x9B.toByte()
            )
        } else {
            Logger.withTag(TAG).w { "Machine is not cleaning, ignoring stop request." }
        }
    }

    override suspend fun setHeatingMode(heatingMode: HeatingMode) {
        val char = writeChar ?: return

        val regAddressHi: Byte = 0x00
        val regAddressLo: Byte = 0x16

        val modeValue = if (heatingMode == HeatingMode.FullSpeed) 0x01 else 0x00

        val header = byteArrayOf(
            0x01,
            0x10,
            regAddressHi, regAddressLo,
            0x00, 0x01,
            0x02,
            0x00, modeValue.toByte()
        )

        val fullCommand = header + calculateCRC(header)

        val success = writeAndAwaitModbus(char, fullCommand, 0x10, regAddressHi, regAddressLo)
        if (success) {
            Logger.withTag(TAG).i { "Heating mode set to $heatingMode confirmed" }

            _machineState.update { it.copy(config = it.config?.copy(heatingMode = heatingMode)) }
        }
    }

    private suspend fun sendModbusPulse(onCommand: ByteArray, offCommand: ByteArray, label: String, regHi: Byte, regLo: Byte) {
        val char = writeChar ?: return

        try {
            writeAndAwaitModbus(char, onCommand, 0x05, regHi, regLo)
            delay(BREW_PULSE_MS)
            writeAndAwaitModbus(char, offCommand, 0x05, regHi, regLo)

            Logger.withTag(TAG).i { "$label pulse completed and confirmed" }
        } catch (e: Exception) {
            Logger.withTag(TAG).e(e) { "Failed to send $label pulse" }
        }
    }

    override fun didDiscoverDevice(
        bluetoothPeripheral: BluetoothPeripheral,
        advertisementData: Map<AdvertisementDataRetrievalKeys, Any>
    ) {
        val target = targetMacAddress ?: return
        val normalizedTarget = target.filter { it.isLetterOrDigit() }.uppercase()

        val deviceName = advertisementData[AdvertisementDataRetrievalKeys.LocalName] as? String
            ?: bluetoothPeripheral.name ?: ""

        val macInName = hexPattern.find(deviceName)?.value?.uppercase()
        val isOurDevice = if (macInName != null) {
            macInName == normalizedTarget
        } else {
            bluetoothPeripheral.uuid.filter { it.isLetterOrDigit() }.uppercase() == normalizedTarget
        }

        if (isOurDevice && _machineState.value.connectionStatus == ConnectionStatus.Connecting && !isConnectingToGatt) {
            isConnectingToGatt = true
            Logger.withTag(TAG).i { "Target device discovered ($deviceName). Stopping scan and connecting..." }
            blueFalcon.stopScanning()
            scope.launch {
                delay(200)
                blueFalcon.connect(bluetoothPeripheral, false)
            }
        }
    }

    override fun didConnect(bluetoothPeripheral: BluetoothPeripheral) {
        Logger.withTag(TAG).i { "Successfully connected to ${bluetoothPeripheral.uuid}" }
        connectionTimeoutJob?.cancel()
        connectedPeripheral = bluetoothPeripheral
        _machineState.update { it.copy(connectionStatus = ConnectionStatus.Connected) }
        blueFalcon.discoverServices(bluetoothPeripheral)
    }

    override fun didDisconnect(bluetoothPeripheral: BluetoothPeripheral) {
        Logger.withTag(TAG).i { "Disconnected from ${bluetoothPeripheral.uuid}" }
        connectionTimeoutJob?.cancel()
        pollingJob?.cancel()
        writeChar = null
        notifyChar = null
        connectedPeripheral = null
        isConnectingToGatt = false
        _machineState.update {
            it.copy(
                connectionStatus = ConnectionStatus.Disconnected,
                pressure = null,
                steamBoilerTemp = null,
                brewBoilerTemp = null
            )
        }
    }

    override fun didDiscoverServices(bluetoothPeripheral: BluetoothPeripheral) {
        bluetoothPeripheral.services.forEach { service ->
            blueFalcon.discoverCharacteristics(bluetoothPeripheral, service.value)
        }
    }

    override fun didDiscoverCharacteristics(bluetoothPeripheral: BluetoothPeripheral) {
        bluetoothPeripheral.services.values
            .flatMap { it.characteristics }
            .forEach { characteristic ->
                val uuid = characteristic.name?.lowercase() ?: ""

                if (uuid.contains(DATA_UUID_SUFFIX)) {
                    writeChar = characteristic
                    Logger.withTag(TAG).d { "DATA characteristic ready (2b10)" }
                    blueFalcon.notifyCharacteristic(bluetoothPeripheral, characteristic, true)
                }
                if (uuid.contains(CTRL_UUID_SUFFIX)) {
                    notifyChar = characteristic
                    Logger.withTag(TAG).d { "CTRL characteristic ready (2c10)" }
                    blueFalcon.notifyCharacteristic(bluetoothPeripheral, characteristic, true)
                }
            }
    }

    override fun didUpdateNotificationStateFor(
        bluetoothPeripheral: BluetoothPeripheral,
        bluetoothCharacteristic: BluetoothCharacteristic
    ) {
        val uuid = bluetoothCharacteristic.name?.lowercase() ?: ""
        Logger.withTag(TAG).d { "Notification state updated for: $uuid" }

        if (writeChar != null && notifyChar != null) {
            if (pollingJob == null || !pollingJob!!.isActive) {
                startDeviceInitialization(bluetoothPeripheral)
            }
        }
    }

    override fun didCharacteristcValueChanged(
        bluetoothPeripheral: BluetoothPeripheral,
        bluetoothCharacteristic: BluetoothCharacteristic
    ) {
        val data = bluetoothCharacteristic.value ?: return
        logFrame(Direction.RX, bluetoothCharacteristic, data)

        // Route raw byte array to any awaiters
        incomingFrames.tryEmit(data)

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
            0x05, 0x10 -> {
                // Confirmations handled by suspended functions
            }
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
            } else if (asciiString.contains("BOOKOO")) {
                Logger.withTag(TAG).i { "Connected Smart Scale recognized: $asciiString" }
            }
        } catch (e: Exception) {
            Logger.withTag(TAG).e(e) { "Error parsing proprietary frame" }
        }
    }

    private fun parseConfigFrame(payload: ByteArray) {
        try {
            if (payload.size < ConfigFrame.MIN_HEADER_SIZE) return

            fun dataU16be(off: Int) = payload.u16be(ConfigFrame.DATA_START + off)

            val cleaningTimeSec = dataU16be(0) / 10f
            val cleaningStandbySec = dataU16be(2) / 10f
            val cleaningCount = dataU16be(4)

            val isSteamBoilerEnabled = dataU16be(12) == 0
            val isBrewBoilerEnabled = dataU16be(14) == 0

            // Target temperatures are whole values (not scaled by 10)
            val targetSteam = dataU16be(16).toFloat()
            val targetBrew = dataU16be(18).toFloat()

            val manualBrewTimeSec = dataU16be(34) / 10f
            val unknownReg18 = dataU16be(36).toFloat()
            val manualBrewPressure = dataU16be(38) / 10f

            val isFullSpeedHeating = dataU16be(44) == 1
            val waterAlarm = dataU16be(52) == 1

            Logger.withTag(TAG).i {
                """Config parsed: 
                | Target Brew=${targetBrew}°C, Target Steam=${targetSteam}°C 
                | Steam Boiler ON=${isSteamBoilerEnabled}, Brew Boiler ON=${isBrewBoilerEnabled}
                | Manual M: Time=${manualBrewTimeSec}s, UnknownReg18=${unknownReg18}, Pressure=${manualBrewPressure}bar
                | Heating Mode: ${if (isFullSpeedHeating) "Full Speed" else "Pulse"}
                | Water Alarm: ${if (waterAlarm) "NO WATER" else "WATER"}
                | Cleaning: Time=${cleaningTimeSec}s, Standby=${cleaningStandbySec}s, Count=${cleaningCount}
                """.trimMargin()
            }

            _machineState.update {
                it.copy(
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
        _machineState.update { it.copy(brewStatus = brewStatus) }
    }

    private fun parseTelemetryFrame(payload: ByteArray) {
        try {
            if (payload.size < TelemetryFrame.MIN_HEADER_SIZE) return

            fun dataU8(off: Int) = payload.u8(TelemetryFrame.DATA_START + off)
            fun dataU16be(off: Int) = payload.u16be(TelemetryFrame.DATA_START + off)

            val pressure = dataU16be(TelemetryFrame.PRESSURE) / 10f
            val weight = dataU16be(TelemetryFrame.WEIGHT) / 10f
            val steamActual = dataU16be(TelemetryFrame.STEAM_TEMP) / 10f
            val brewActual = dataU16be(TelemetryFrame.BREW_TEMP) / 10f
            val weightRate = dataU16be(TelemetryFrame.WEIGHT_RATE) / 10f

            val volume = dataU16be(TelemetryFrame.VOLUME).toFloat()
            val flowRate = dataU16be(TelemetryFrame.FLOW_RATE).toFloat()
            val time = dataU16be(TelemetryFrame.TIME)

            _machineState.update {
                it.copy(
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

    override suspend fun startProfileBrewing(profile: BrewProfile) {
        val char = writeChar ?: return
        Logger.withTag(TAG).i { "Starting profile brew: ${profile.name}" }

        // 1. Upload profile data
        val commands = buildProfileUploadCommands(profile)
        for (cmd in commands) {
            val success = writeAndAwaitModbus(
                char, cmd.payload,
                expectedFc = if (cmd.payload[1] == 0x10.toByte()) 0x10 else 0x06,
                expectedRegHi = cmd.regHi,
                expectedRegLo = cmd.regLo
            )
            if (!success) {
                Logger.withTag(TAG).e { "Failed to upload profile part at register ${cmd.regHi.toInt() shl 8 or cmd.regLo.toInt()}" }
                return
            }
        }

        // 2. Start brewing pulse
        Logger.withTag(TAG).i { "Profile uploaded, triggering brew pulse" }
        if (profile.mode == ProfileMode.FreeVariable) {
            sendModbusPulse(
                onCommand = "0105009eff00ec14".decodeHex(),
                offCommand = "0105009e0000ad24".decodeHex(),
                label = "Profile Brew (Free)",
                regHi = 0x00,
                regLo = 0x9E.toByte()
            )
        } else {
            triggerShortPress()
        }
    }

    /**
     * Binds the provided [profile] to the machine's physical button (Shortcut Slot 1).
     *
     * This method uploads the profile data and sends a persistent save command
     * to register 30, making it the default profile for physical button triggers.
     *
     * @param profile The [BrewProfile] to be bound.
     */
    override suspend fun bindProfile(profile: BrewProfile) {
        val char = writeChar ?: return
        Logger.withTag(TAG).i { "Binding profile to button: ${profile.name}" }

        // 1. Upload profile data
        val commands = buildProfileUploadCommands(profile)
        for (cmd in commands) {
            val success = writeAndAwaitModbus(
                char, cmd.payload,
                expectedFc = if (cmd.payload[1] == 0x10.toByte()) 0x10 else 0x06,
                expectedRegHi = cmd.regHi,
                expectedRegLo = cmd.regLo
            )
            if (!success) {
                Logger.withTag(TAG).e { "Failed to upload profile part for binding" }
                return
            }
        }

        // 2. Send Bind command (Write 1 to register 30)
        Logger.withTag(TAG).i { "Profile uploaded, sending bind command to register 30" }
        val bindCmd = buildWriteMultiple(30, listOf(1))
        writeAndAwaitModbus(
            char, bindCmd.payload,
            expectedFc = 0x10,
            expectedRegHi = bindCmd.regHi,
            expectedRegLo = bindCmd.regLo
        )
    }

    private class ModbusCommand(val payload: ByteArray, val regHi: Byte, val regLo: Byte)

    /**
     * Translates a [BrewProfile] into a sequence of Modbus register write commands.
     *
     * This method handles two distinct mapping logics based on the [BrewProfile.mode]:
     *
     * 1. **Free Variable Mode ([ProfileMode.FreeVariable]):**
     *    The profile is resampled into a high-resolution 128-point curve (0.1s interval).
     *    The data is split across 8 distinct memory regions (registers 760-1080 and 1500-1564),
     *    covering pressure, flow, and weight targets.
     *
     * 2. **Constant/Variable Modes ([ProfileMode.ConstantPressure], [ProfileMode.VariablePressure]):**
     *    The profile is sent as a structured header followed by discrete multi-step definitions
     *    starting from register 2048. Each step includes duration, pressure/flow targets,
     *    and priority flags.
     *
     * @param profile The source profile containing steps and brewing metadata.
     * @return A list of [ModbusCommand] objects ready to be transmitted to the machine.
     */
    private fun buildProfileUploadCommands(profile: BrewProfile): List<ModbusCommand> {
        val commands = mutableListOf<ModbusCommand>()
        val isWeight = profile.finishCondition is Condition.Weight
        val targetValue = when (val cond = profile.finishCondition) {
            is Condition.Weight -> cond.target
            is Condition.Volume -> cond.target
        }

        // Map ProfileMode to Machine's real_mode based on logs (Variable = 2)
        val realMode = when (profile.mode) {
            ProfileMode.VariablePressure -> 2
            ProfileMode.ConstantPressure -> 1
            ProfileMode.FreeVariable -> 4
        }

        if (profile.mode == ProfileMode.FreeVariable) {
            // Mode 3 (Free Variable) logic from ble.txt
            val points = resampleSteps(profile.steps, 128, 0.1f)
            
            val registers = listOf(760, 824, 888, 952, 1016, 1080, 1500, 1564)
            val dataTypes = listOf("bar", "bar", "rflow", "rflow", "weight", "weight", "flow", "flow")

            for (k in 0 until 8) {
                val reg = registers[k]
                val type = dataTypes[k]
                val isSecondHalf = k % 2 != 0
                val startIndex = if (isSecondHalf) 64 else 0
                
                val values = (0 until 64).map { i ->
                    val point = points.getOrNull(startIndex + i)
                    when (type) {
                        "bar" -> ((point?.pressure ?: 0f) * 10).toInt()
                        "flow" -> ((point?.flow ?: 0f) * 10).toInt()
                        "rflow" -> ((point?.flow ?: 0f) * 10).toInt() 
                        "weight" -> 0 
                        else -> 0
                    }
                }
                commands.add(buildWriteMultiple(reg, values))
            }

            commands.add(buildWriteMultiple(79, listOf(if (isWeight) 0 else 1)))
            commands.add(buildWriteSingle(87, realMode))
            commands.add(buildWriteSingle(362, 0))
            commands.add(buildWriteSingle(358, targetValue.toInt()))
            commands.add(buildWriteSingle(366, if (profile.autoLinkOpen) 1 else 0))

        } else {
            // Mode 1/2 logic from logs
            val startReg = 2048 
            
            // Header (7 registers)
            val headerValues = listOf(
                if (isWeight) 0 else 1, // callswitch (Reg 2048)
                if (profile.mode == ProfileMode.VariablePressure) 1 else 0, // pressure mode (Reg 2049)
                1, // direct (Reg 2050)
                1, // changeswitch (Reg 2051)
                if (!isWeight) targetValue.toInt() else 0, // total water (Reg 2052)
                if (isWeight) targetValue.toInt() else 0, // total weight (Reg 2053)
                if (profile.autoLinkOpen) 1 else 0 // auto link (Reg 2054)
            )
            commands.add(buildWriteMultiple(startReg, headerValues))

            // Logic to merge Wait steps into awaitTime of previous steps
            data class MachineStep(
                val time: Int,
                val bar: Float,
                val flow: Float,
                var awaitTime: Int = 0,
                val isFlowPriority: Boolean
            )

            val machineSteps = mutableListOf<MachineStep>()
            profile.steps.forEach { step ->
                when (step) {
                    is ProfileStep.Wait -> {
                        machineSteps.lastOrNull()?.let { it.awaitTime = step.time }
                    }
                    is ProfileStep.Pressure -> machineSteps.add(MachineStep(step.time, step.pressure, 0f, 0, false))
                    is ProfileStep.Flow -> machineSteps.add(MachineStep(step.time, 0f, step.flow, 0, true))
                }
            }

            var currentReg = startReg + 8
            machineSteps.forEachIndexed { index, step ->
                val isLast = index == machineSteps.size - 1
                val stepValues = listOf(
                    step.time, // Time (raw seconds from logs)
                    (step.bar * 10).toInt(),
                    (step.flow * 10).toInt(),
                    step.awaitTime,
                    if (isLast) 1 else 0,
                    if (step.isFlowPriority) 1 else 0
                )
                commands.add(buildWriteMultiple(currentReg, stepValues))
                currentReg += 9 // Steps are 9 registers apart in logs (2056 -> 2065)
            }
            
            // Set mode (Reg 87)
            commands.add(buildWriteSingle(87, realMode))
        }

        return commands
    }

    private fun buildWriteSingle(reg: Int, value: Int): ModbusCommand {
        val regHi = (reg ushr 8).toByte()
        val regLo = (reg and 0xFF).toByte()
        val header = byteArrayOf(
            0x01, 0x06,
            regHi, regLo,
            (value ushr 8).toByte(), (value and 0xFF).toByte()
        )
        return ModbusCommand(header + calculateCRC(header), regHi, regLo)
    }

    private fun buildWriteMultiple(reg: Int, values: List<Int>): ModbusCommand {
        val regHi = (reg ushr 8).toByte()
        val regLo = (reg and 0xFF).toByte()
        val num = values.size
        val byteCount = num * 2
        
        val header = byteArrayOf(
            0x01, 0x10,
            regHi, regLo,
            (num ushr 8).toByte(), (num and 0xFF).toByte(),
            byteCount.toByte()
        )
        
        val data = ByteArray(byteCount)
        values.forEachIndexed { i, v ->
            data[i * 2] = (v ushr 8).toByte()
            data[i * 2 + 1] = (v and 0xFF).toByte()
        }
        
        val payload = header + data
        return ModbusCommand(payload + calculateCRC(payload), regHi, regLo)
    }

    private data class ResampledPoint(val pressure: Float, val flow: Float)

    private fun resampleSteps(steps: List<ProfileStep>, count: Int, interval: Float): List<ResampledPoint> {
        val result = mutableListOf<ResampledPoint>()
        var currentPressure = 0f
        var currentFlow = 0f
        
        val timeline = mutableListOf<ResampledPoint>()
        steps.forEach { step ->
            val durationTicks = (step.time / interval).toInt()
            when (step) {
                is ProfileStep.Pressure -> currentPressure = step.pressure
                is ProfileStep.Flow -> currentFlow = step.flow
                is ProfileStep.Wait -> {
                    currentPressure = 0f
                    currentFlow = 0f
                }
            }
            repeat(durationTicks) {
                timeline.add(ResampledPoint(currentPressure, currentFlow))
            }
        }
        
        repeat(count) { i ->
            result.add(timeline.getOrNull(i) ?: ResampledPoint(0f, 0f))
        }
        return result
    }

    private fun ByteArray.u8(i: Int): Int = this[i].toInt() and 0xFF
    private fun ByteArray.u16be(i: Int): Int = (u8(i) shl 8) or u8(i + 1)

    private fun calculateCRC(bytes: ByteArray): ByteArray {
        var crc = 0xFFFF
        for (b in bytes) {
            crc = crc xor (b.toInt() and 0xFF)
            repeat(8) {
                crc = if (crc and 0x0001 != 0) {
                    (crc ushr 1) xor 0xA001
                } else {
                    crc ushr 1
                }
            }
        }
        return byteArrayOf(
            (crc and 0xFF).toByte(),
            ((crc ushr 8) and 0xFF).toByte()
        )
    }

    private enum class Direction { TX, RX }

    private fun logFrame(
        dir: Direction,
        characteristic: BluetoothCharacteristic,
        bytes: ByteArray,
        note: String? = null
    ) {
        val uuid = characteristic.name?.lowercase() ?: "unknown-uuid"
        val hex = bytes.toHex()

        val isProprietary = bytes.size >= 4 && bytes[0] == 0xFF.toByte() && bytes[1] == 0x55.toByte()
        val fc = bytes.getOrNull(1)?.toInt()?.and(0xFF)

        val fcName = if (isProprietary) {
            "Proprietary FF55"
        } else {
            when (fc) {
                0x01 -> "ReadCoils"
                0x03 -> "ReadHoldingRegisters"
                0x05 -> "WriteSingleCoil"
                0x06 -> "WriteSingleRegister"
                0x10 -> "WriteMultipleRegisters"
                else -> null
            }
        }

        Logger.withTag(TAG).d {
            buildString {
                append("$dir uuid=$uuid len=${bytes.size}")
                append(" fc=")
                if (isProprietary) append(fcName)
                else if (fc != null) {
                    append("0x${fc.toString(16)}")
                    fcName?.let { append("($it)") }
                } else append("?")

                if (note != null) append(" note=$note")
                append(" hex=$hex")
            }
        }
    }
}
