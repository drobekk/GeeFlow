package dev.drobek.geeflow.data.device.impl.controller

import co.touchlab.kermit.Logger
import dev.drobek.geeflow.data.brew.model.BrewProfile
import dev.drobek.geeflow.data.brew.model.ProfileMode
import dev.drobek.geeflow.data.device.DeviceController
import dev.drobek.geeflow.data.device.ble.BleClient
import dev.drobek.geeflow.data.device.ble.BleConnectException
import dev.drobek.geeflow.data.device.ble.BleConnectionState
import dev.drobek.geeflow.data.device.ble.ModbusBleClient
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.CMD_CLEANING_OFF
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.CMD_CLEANING_ON
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.CMD_MANUAL_OFF
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.CMD_MANUAL_ON
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.CMD_POLLING_LONG
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.CMD_POLLING_SHORT
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.CMD_READ_CONFIG_LONG
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.CMD_SCALE_LIST_REQUEST
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.CMD_SCALE_SEARCH_OFF
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.CMD_SCALE_SEARCH_ON
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.CMD_SCALE_SEARCH_QUERY
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.CMD_SCALE_STATUS_REQUEST
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.CMD_SHORT_PRESS_OFF
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.CMD_SHORT_PRESS_ON
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.CMD_START_STREAMING
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.decodeHex
import dev.drobek.geeflow.data.device.impl.discovery.BleAdvertisement
import dev.drobek.geeflow.data.device.impl.discovery.WendougeeBleDeviceDiscoverer
import dev.drobek.geeflow.data.device.model.Device
import dev.drobek.geeflow.data.device.model.DeviceCapability
import dev.drobek.geeflow.data.device.model.DeviceConnection
import dev.drobek.geeflow.data.device.model.DeviceState
import dev.drobek.geeflow.data.device.model.DeviceState.BoilerType
import dev.drobek.geeflow.data.device.model.DeviceState.BrewStatus
import dev.drobek.geeflow.data.device.model.DeviceState.ConnectionStatus
import dev.drobek.geeflow.data.device.model.DeviceState.HeatingMode
import dev.drobek.geeflow.data.device.model.SmartScale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.Singleton

@Singleton
class WendougeeDataSController(
    private val scope: CoroutineScope,
    private val bleClient: BleClient,
    private val discoverer: WendougeeBleDeviceDiscoverer,
) : DeviceController {

    private val _deviceState = MutableStateFlow(DeviceState())
    override val deviceState: StateFlow<DeviceState> = _deviceState.asStateFlow()

    private val _foundScales = MutableStateFlow<List<SmartScale>>(emptyList())
    override val foundScales: StateFlow<List<SmartScale>> = _foundScales.asStateFlow()

    private val _resolvedConnection = MutableSharedFlow<DeviceConnection>(extraBufferCapacity = 1)
    override val resolvedConnection: SharedFlow<DeviceConnection> = _resolvedConnection.asSharedFlow()

    override val capabilities: Set<DeviceCapability> = setOf(
        DeviceCapability.SteamBoiler,
        DeviceCapability.BrewBoiler,
        DeviceCapability.WaterAlarm,
        DeviceCapability.CleaningSettings,
        DeviceCapability.ManualBrewing,
        DeviceCapability.ProfileBrewing,
        DeviceCapability.CleaningMode,
        DeviceCapability.HeatingMode,
        DeviceCapability.SmartScaleConnectivity,
        DeviceCapability.SingleDoseGrinderConnectivity,
        DeviceCapability.CommercialGrinderConnectivity,
        DeviceCapability.PressureProfiling,
        DeviceCapability.FlowProfiling
    )

    var logPolling: Boolean = false

    private val modbus = ModbusBleClient(bleClient, DATA_UUID_SUFFIX, DATA_UUID_SUFFIX)
    private val frameParser = WendougeeFrameParser(
        onStateUpdate = { update -> _deviceState.update { it.update() } },
        onScaleFound = { scale ->
            _foundScales.update { current ->
                if (current.any { it.name == scale.name }) current.map { if (it.name == scale.name) scale else it }
                else current + scale
            }
        },
        onHeartbeat = { onHeartbeatReceived() },
        shouldLogPolling = { logPolling }
    )
    private val profileCompiler = WendougeeProfileCompiler()

    private var connectJob: Job? = null
    private var watchdogJob: Job? = null
    private var heartbeatTimeoutJob: Job? = null

    companion object {
        private const val TAG = "WendougeeController"
        private const val BLE_TRACE_TAG = "WendougeeBle"

        const val DATA_UUID_SUFFIX = "2b10"
        const val CTRL_UUID_SUFFIX = "2c10"

        private const val POLLING_INTERVAL_MS = 200L
        private const val BREW_PULSE_MS = 100L
        private const val HEARTBEAT_TIMEOUT_MS = 10_000L
    }

    init {
        observeIncomingData()
        observeBleDisconnects()
    }

    private fun onHeartbeatReceived() {
        if (!_deviceState.value.smartScaleEnabled) return
        _deviceState.update { it.copy(smartScaleSearchActive = true) }
        heartbeatTimeoutJob?.cancel()
        heartbeatTimeoutJob = scope.launch {
            delay(HEARTBEAT_TIMEOUT_MS)
            _deviceState.update { it.copy(smartScaleSearchActive = false) }
        }
    }

    private fun observeIncomingData() {
        scope.launch {
            bleClient.incomingData.collect { cd ->
                if (cd.value.isNotEmpty()) {
                    val channel = if (cd.uuid.contains(CTRL_UUID_SUFFIX)) "CTRL" else "DATA"
                    frameParser.handleIncomingFrame(cd.value, channel)
                }
            }
        }
    }

    /**
     * Watchdog: if BLE drops unexpectedly while connecting or connected, cancel the
     * connect job and reset state. Does NOT start initialization — that's driven
     * imperatively from [connect].
     */
    private fun observeBleDisconnects() {
        watchdogJob = scope.launch {
            bleClient.connectionState.collect { state ->
                if (state is BleConnectionState.Disconnected) {
                    val ours = _deviceState.value.connectionStatus
                    if (ours == ConnectionStatus.Connecting ||
                        ours == ConnectionStatus.Synchronizing ||
                        ours == ConnectionStatus.Connected
                    ) {
                        Logger.withTag(TAG).w { "BLE dropped unexpectedly during $ours — resetting" }
                        connectJob?.cancel()
                        resetToDisconnected()
                    }
                }
            }
        }
    }

    override fun connect(device: Device) {
        val ble = device.connection as? DeviceConnection.Ble
            ?: error("WendougeeDataSController requires DeviceConnection.Ble")

        connectJob?.cancel()
        connectJob = scope.launch {
            try {
                _deviceState.update { it.copy(connectionStatus = ConnectionStatus.Connecting) }

                val resolvedId = connectViaBle(ble)
                if (resolvedId != ble.peripheralId) {
                    Logger.withTag(TAG).i { "peripheralId changed: ${ble.peripheralId} → $resolvedId" }
                    _resolvedConnection.emit(ble.copy(peripheralId = resolvedId))
                }

                _deviceState.update { it.copy(connectionStatus = ConnectionStatus.Synchronizing) }

                // MTU negotiation
                val mtuSet = withTimeoutOrNull(4_000) {
                    while (isActive) {
                        if (bleClient.changeMTU(512)) break
                        delay(200)
                    }
                    true
                }
                if (mtuSet == null) Logger.withTag(TAG).w { "MTU negotiation timed out" }
                delay(500)

                // Enable notifications
                val charsReady = withTimeoutOrNull(10_000) {
                    while (isActive) {
                        val dataReady = bleClient.notifyCharacteristic(DATA_UUID_SUFFIX, true)
                        val ctrlReady = bleClient.notifyCharacteristic(CTRL_UUID_SUFFIX, true)
                        if (dataReady && ctrlReady) return@withTimeoutOrNull true
                        delay(200)
                    }
                    false
                }
                if (charsReady != true) {
                    Logger.withTag(TAG).e { "Characteristics not ready — aborting connect" }
                    bleClient.disconnect()
                    resetToDisconnected()
                    return@launch
                }
                delay(300)

                // Send init commands
                write(DATA_UUID_SUFFIX, CMD_READ_CONFIG_LONG)
                delay(300)
                write(CTRL_UUID_SUFFIX, CMD_SCALE_SEARCH_QUERY)
                delay(400)
                write(CTRL_UUID_SUFFIX, CMD_START_STREAMING)
                if (_deviceState.value.smartScaleEnabled) {
                    delay(100)
                    write(CTRL_UUID_SUFFIX, CMD_SCALE_STATUS_REQUEST)
                    delay(200)
                    write(CTRL_UUID_SUFFIX, CMD_SCALE_LIST_REQUEST)
                }
                delay(300)

                // Two pollers must succeed before we mark Connected
                pollLongOnce()
                pollShortOnce()

                _deviceState.update { it.copy(connectionStatus = ConnectionStatus.Connected) }
                Logger.withTag(TAG).i { "Connected and ready" }

                // Steady-state polling loop
                while (isActive) {
                    runCatching { pollLongOnce() }
                        .onFailure { Logger.withTag(TAG).w(it) { "Poll long missed" } }
                    delay(30)
                    runCatching { pollShortOnce() }
                        .onFailure { Logger.withTag(TAG).w(it) { "Poll short missed" } }
                    delay(POLLING_INTERVAL_MS)
                }
            } catch (t: CancellationException) {
                throw t
            } catch (t: Throwable) {
                Logger.withTag(TAG).e(t) { "Connect flow failed" }
                bleClient.disconnect()
                resetToDisconnected()
            }
        }
    }

    /**
     * 1) Try direct connect by saved [ble.peripheralId].
     * 2) On failure, scan for a peripheral whose MAC (parsed from advertisement name
     *    by [WendougeeBleDeviceDiscoverer]) matches [ble.macAddress].
     *
     * Returns the peripheralId that was actually used to connect.
     */
    private suspend fun connectViaBle(ble: DeviceConnection.Ble): String {
        runCatching { bleClient.connect(ble.peripheralId) }
            .onSuccess { return ble.peripheralId }
            .onFailure {
                Logger.withTag(TAG).w(it) {
                    "Direct connect by saved peripheralId=${ble.peripheralId} failed; scanning for MAC=${ble.macAddress}"
                }
            }

        val newId = scanForMacInName(ble.macAddress, timeoutMs = 12_000L)
            ?: throw BleConnectException("Device with MAC ${ble.macAddress} not found via scan")
        bleClient.connect(newId)
        return newId
    }

    private suspend fun scanForMacInName(targetMac: String, timeoutMs: Long): String? {
        bleClient.startScan()
        return try {
            withTimeoutOrNull(timeoutMs) {
                bleClient.discoveredDevices
                    .mapNotNull { set ->
                        set.firstOrNull { ble ->
                            discoverer.macAddressOf(BleAdvertisement(ble.peripheralId, ble.name)) == targetMac
                        }?.peripheralId
                    }
                    .first()
            }
        } finally {
            bleClient.stopScan()
        }
    }

    override fun disconnect() {
        Logger.withTag(TAG).i { "Disconnecting" }
        connectJob?.cancel()
        bleClient.disconnect()
        // resetToDisconnected will be called by the watchdog when BLE state changes to Disconnected
    }

    private fun resetToDisconnected() {
        heartbeatTimeoutJob?.cancel()
        _deviceState.update {
            it.copy(
                connectionStatus = ConnectionStatus.Disconnected,
                pressure = null,
                steamBoilerTemp = null,
                brewBoilerTemp = null,
                smartScale = null,
                smartScaleSearchActive = false
            )
        }
        _foundScales.value = emptyList()
    }

    private suspend fun pollLongOnce() {
        modbus.sendCustomCommandAndWaitForPrefix(
            payload = CMD_POLLING_LONG,
            expectedPrefix = byteArrayOf(0x01, 0x03, 0x28),
            timeoutMs = 800
        )
    }

    private suspend fun pollShortOnce() {
        modbus.sendCustomCommandAndWaitForPrefix(
            payload = CMD_POLLING_SHORT,
            expectedPrefix = byteArrayOf(0x01, 0x01),
            timeoutMs = 800
        )
    }

    override suspend fun setBoilerState(boilerType: BoilerType, enabled: Boolean) {
        val currentConfig = _deviceState.value.config
        val isAlreadySet = when (boilerType) {
            BoilerType.Steam -> currentConfig?.steamBoilerEnabled == enabled
            BoilerType.Brew -> currentConfig?.brewBoilerEnabled == enabled
        }
        if (isAlreadySet) return

        val targetRegister = when (boilerType) {
            BoilerType.Steam -> WendougeeRegisters.STEAM_BOILER_STATE
            BoilerType.Brew -> WendougeeRegisters.BREW_BOILER_STATE
        }
        val stateValue = if (enabled) 0x00 else 0x01

        modbus.writeSingleRegister(targetRegister, stateValue)

        Logger.withTag(TAG).i { "Boiler ${if (boilerType == BoilerType.Steam) "Steam" else "Brew"} set to $enabled confirmed" }
        _deviceState.update { currentState ->
            val config = currentState.config ?: return@update currentState
            val newConfig = if (boilerType == BoilerType.Steam) {
                config.copy(steamBoilerEnabled = enabled)
            } else {
                config.copy(brewBoilerEnabled = enabled)
            }
            currentState.copy(config = newConfig)
        }
    }

    override suspend fun setSteamTemperature(temp: Int) {
        if (temp !in 0..140) {
            Logger.withTag(TAG).e { "Temperature $temp out of safe range!" }
            return
        }

        if (_deviceState.value.config?.targetSteamTemp?.toInt() == temp) return

        modbus.writeSingleRegister(WendougeeRegisters.STEAM_TEMPERATURE, temp)
        Logger.withTag(TAG).i { "Steam temperature set to $temp°C confirmed" }
        _deviceState.update { currentState ->
            val config = currentState.config ?: return@update currentState
            currentState.copy(config = config.copy(targetSteamTemp = temp.toFloat()))
        }
    }

    override suspend fun setBrewTemperature(temp: Int) {
        if (temp !in 0..110) {
            Logger.withTag(TAG).e { "Brew temperature $temp out of range!" }
            return
        }
        if (_deviceState.value.config?.targetBrewTemp?.toInt() == temp) return

        modbus.writeSingleRegister(WendougeeRegisters.BREW_TEMPERATURE, temp)
        Logger.withTag(TAG).i { "Brew temperature set to $temp°C confirmed" }
        _deviceState.update { currentState ->
            val config = currentState.config ?: return@update currentState
            currentState.copy(config = config.copy(targetBrewTemp = temp.toFloat()))
        }
    }

    override suspend fun startManualBrewing() {
        if (_deviceState.value.brewStatus == BrewStatus.Idle) {
            Logger.withTag(TAG).i { "Starting manual brew cycle..." }
            sendModbusPulse(CMD_MANUAL_ON, CMD_MANUAL_OFF, "Manual Brew", 0x00, 0x9A.toByte())
        }
    }

    override suspend fun stopManualBrewing() {
        if (_deviceState.value.brewStatus == BrewStatus.Manual) {
            Logger.withTag(TAG).i { "Stopping manual brew cycle..." }
            sendModbusPulse(CMD_MANUAL_ON, CMD_MANUAL_OFF, "Manual Brew Stop", 0x00, 0x9A.toByte())
        }
    }

    override suspend fun stopProfileBrewing() {
        if (_deviceState.value.brewStatus == BrewStatus.Profile) {
            Logger.withTag(TAG).i { "Stopping profile brew cycle..." }
            triggerShortPress()
        }
    }

    private suspend fun triggerShortPress() {
        sendModbusPulse(CMD_SHORT_PRESS_ON, CMD_SHORT_PRESS_OFF, "Short Press", 0x00, 0x96.toByte())
    }

    override suspend fun startCleaning() {
        if (_deviceState.value.brewStatus == BrewStatus.Idle) {
            Logger.withTag(TAG).i { "Sending start signal for cleaning..." }
            sendModbusPulse(CMD_CLEANING_ON, CMD_CLEANING_OFF, "Cleaning Procedure", 0x00, 0x9B.toByte())
        }
    }

    override suspend fun stopCleaning() {
        if (_deviceState.value.brewStatus == BrewStatus.Cleaning) {
            Logger.withTag(TAG).i { "Sending stop signal for cleaning..." }
            sendModbusPulse(CMD_CLEANING_ON, CMD_CLEANING_OFF, "Stop Cleaning", 0x00, 0x9B.toByte())
        }
    }

    override suspend fun setHeatingMode(heatingMode: HeatingMode) {
        if (_deviceState.value.config?.heatingMode == heatingMode) return

        val modeValue = if (heatingMode == HeatingMode.FullSpeed) 0x01 else 0x00
        modbus.writeSingleRegister(WendougeeRegisters.HEATING_MODE, modeValue)
        Logger.withTag(TAG).i { "Heating mode set to $heatingMode confirmed" }
        _deviceState.update { it.copy(config = it.config?.copy(heatingMode = heatingMode)) }
    }

    override suspend fun setManualBrewPressure(pressure: Float) {
        if (_deviceState.value.config?.manualBrewPressure == pressure) return

        modbus.writeMultipleRegisters(WendougeeRegisters.MANUAL_BREW_PRESSURE, listOf((pressure * 10).toInt()))
        Logger.withTag(TAG).d { "Manual brew pressure set to $pressure bar confirmed" }
        _deviceState.update { state ->
            val config = state.config ?: return@update state
            state.copy(config = config.copy(manualBrewPressure = pressure))
        }
    }

    override suspend fun setManualBrewTime(timeSec: Float) {
        if (_deviceState.value.config?.manualBrewTimeSec == timeSec) return

        modbus.writeMultipleRegisters(WendougeeRegisters.MANUAL_BREW_TIME, listOf((timeSec * 10).toInt()))
        Logger.withTag(TAG).d { "Manual brew time set to $timeSec s confirmed" }
        _deviceState.update { state ->
            val config = state.config ?: return@update state
            state.copy(config = config.copy(manualBrewTimeSec = timeSec))
        }
    }

    override suspend fun setCleaningSettings(timeSec: Float, standbySec: Float, count: Int) {
        modbus.writeMultipleRegisters(WendougeeRegisters.CLEANING_TIME, listOf((timeSec * 10).toInt()))
        modbus.writeMultipleRegisters(WendougeeRegisters.CLEANING_STANDBY_TIME, listOf((standbySec * 10).toInt()))
        modbus.writeMultipleRegisters(WendougeeRegisters.CLEANING_COUNT, listOf(count))
        Logger.withTag(TAG).i { "Cleaning settings: time=${timeSec}s standby=${standbySec}s count=$count" }
        _deviceState.update { state ->
            val config = state.config ?: return@update state
            state.copy(config = config.copy(cleaningTimeSec = timeSec, cleaningStandbySec = standbySec, cleaningCount = count))
        }
    }

    override suspend fun setWaterAlarm(enabled: Boolean) {
        if (_deviceState.value.config?.waterAlarmEnabled == enabled) return
        modbus.writeMultipleRegisters(WendougeeRegisters.WATER_ALARM, listOf(if (enabled) 1 else 0))
        Logger.withTag(TAG).d { "Water alarm set to $enabled" }
        _deviceState.update { state ->
            val config = state.config ?: return@update state
            state.copy(config = config.copy(waterAlarmEnabled = enabled))
        }
    }

    private suspend fun sendModbusPulse(onCommand: ByteArray, offCommand: ByteArray, label: String, regHi: Byte, regLo: Byte) {
        modbus.writeAndAwaitModbus(onCommand, 0x05, regHi, regLo)
        delay(BREW_PULSE_MS)
        modbus.writeAndAwaitModbus(offCommand, 0x05, regHi, regLo)
        Logger.withTag(TAG).d { "$label pulse completed and confirmed" }
    }

    override suspend fun startProfileBrewing(profile: BrewProfile) {
        Logger.withTag(TAG).i { "Starting profile brew: ${profile.name}" }

        val commands = profileCompiler.buildProfileUploadCommands(profile)
        for (cmd in commands) {
            modbus.writeAndAwaitModbus(
                cmd.payload,
                expectedFc = cmd.expectedFc,
                expectedRegHi = cmd.regHi,
                expectedRegLo = cmd.regLo
            )
        }

        Logger.withTag(TAG).d { "Profile uploaded, triggering brew pulse" }
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

    override suspend fun bindProfile(profile: BrewProfile) {
        Logger.withTag(TAG).i { "Binding profile to button: ${profile.name}" }

        val commands = profileCompiler.buildProfileUploadCommands(profile, isBinding = true)
        for (cmd in commands) {
            modbus.writeAndAwaitModbus(
                cmd.payload,
                expectedFc = cmd.expectedFc,
                expectedRegHi = cmd.regHi,
                expectedRegLo = cmd.regLo
            )
        }

        Logger.withTag(TAG).d { "Profile uploaded, sending bind command to register ${WendougeeRegisters.BIND_PROFILE}" }
        modbus.writeMultipleRegisters(WendougeeRegisters.BIND_PROFILE, listOf(1))
    }

    override suspend fun setSmartScaleConnectivity(enabled: Boolean) {
        Logger.withTag(TAG).i { "Smart scale connectivity: $enabled" }
        _deviceState.update { it.copy(smartScaleEnabled = enabled) }
        if (enabled) {
            _foundScales.value = emptyList()
            write(CTRL_UUID_SUFFIX, CMD_SCALE_SEARCH_ON)
            delay(200)
            write(CTRL_UUID_SUFFIX, CMD_SCALE_LIST_REQUEST)
        } else {
            write(CTRL_UUID_SUFFIX, CMD_SCALE_SEARCH_OFF)
        }
    }

    override suspend fun requestSmartScaleList() {
        Logger.withTag(TAG).d { "Requesting smart scale list..." }
        write(CTRL_UUID_SUFFIX, CMD_SCALE_LIST_REQUEST)
    }

    override suspend fun connectSmartScale(name: String) {
        Logger.withTag(TAG).i { "Connecting smart scale: $name" }
        write(CTRL_UUID_SUFFIX, buildScaleFrame(0x80, name))
    }

    override suspend fun disconnectSmartScale() {
        val name = _deviceState.value.smartScale?.name ?: run {
            Logger.withTag(TAG).w { "Disconnect called but no scale is connected" }
            return
        }
        Logger.withTag(TAG).i { "Disconnecting smart scale: $name" }
        write(CTRL_UUID_SUFFIX, buildScaleFrame(0x87, name))
    }

    private suspend fun write(uuid: String, data: ByteArray) {
        val channel = if (uuid.contains(CTRL_UUID_SUFFIX)) "CTRL" else "DATA"
        Logger.withTag(BLE_TRACE_TAG).v { "→ [$channel] ${data.toHexTrace()} (${data.size}B)" }
        bleClient.writeCharacteristic(uuid, data)
    }

    private fun ByteArray.toHexTrace() = joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    private fun buildScaleFrame(cmd: Int, name: String): ByteArray {
        val nameBytes = name.encodeToByteArray()
        val len = nameBytes.size
        val buffer = ByteArray(7 + len)
        buffer[0] = 0xFF.toByte()
        buffer[1] = 0x55.toByte()
        buffer[2] = 0xFF.toByte()
        buffer[3] = 0xFF.toByte()
        buffer[4] = cmd.toByte()
        buffer[5] = (len ushr 8).toByte()
        buffer[6] = (len and 0xFF).toByte()
        nameBytes.copyInto(buffer, destinationOffset = 7)
        var sum = 0
        for (i in 4 until buffer.size) sum += buffer[i].toInt() and 0xFF
        return buffer + ((sum + 0x53) and 0xFF).toByte()
    }
}
