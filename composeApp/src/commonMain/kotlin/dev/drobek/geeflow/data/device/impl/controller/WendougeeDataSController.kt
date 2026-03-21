@file:OptIn(ExperimentalUuidApi::class)

package dev.drobek.geeflow.data.device.impl.controller

import co.touchlab.kermit.Logger
import dev.drobek.geeflow.data.device.api.DeviceController
import dev.drobek.geeflow.data.device.ble.BleClient
import dev.drobek.geeflow.data.device.ble.ModbusBleClient
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.CMD_CLEANING_OFF
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.CMD_CLEANING_ON
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.CMD_INIT_HANDSHAKE
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.CMD_MANUAL_OFF
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.CMD_MANUAL_ON
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.CMD_POLLING_LONG
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.CMD_POLLING_SHORT
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.CMD_READ_CONFIG_LONG
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.CMD_SHORT_PRESS_OFF
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.CMD_SHORT_PRESS_ON
import dev.drobek.geeflow.data.device.impl.controller.WendougeeCommands.decodeHex
import dev.drobek.geeflow.domain.brew.model.BrewProfile
import dev.drobek.geeflow.domain.brew.model.ProfileMode
import dev.drobek.geeflow.domain.device.model.DeviceCapability
import dev.drobek.geeflow.domain.device.model.MachineState
import dev.drobek.geeflow.domain.device.model.MachineState.BoilerType
import dev.drobek.geeflow.domain.device.model.MachineState.BrewStatus
import dev.drobek.geeflow.domain.device.model.MachineState.ConnectionStatus
import dev.drobek.geeflow.domain.device.model.MachineState.HeatingMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.Singleton
import kotlin.uuid.ExperimentalUuidApi

@Singleton
class WendougeeDataSController(
    private val scope: CoroutineScope,
    private val bleClient: BleClient
) : DeviceController {

    private val _machineState = MutableStateFlow(MachineState())
    override val machineState: StateFlow<MachineState> = _machineState.asStateFlow()

    override val capabilities: Set<DeviceCapability> = setOf(
        DeviceCapability.SteamBoiler,
        DeviceCapability.BrewBoiler,
        DeviceCapability.SteamTemp,
        DeviceCapability.BrewTemp,
        DeviceCapability.ManualBrewing,
        DeviceCapability.ProfileBrewing,
        DeviceCapability.CleaningMode,
        DeviceCapability.HeatingModeControl,
        DeviceCapability.SmartScaleConnectivity,
        DeviceCapability.PressureProfiling,
        DeviceCapability.FlowProfiling
    )

    private val modbus = ModbusBleClient(bleClient, DATA_UUID_SUFFIX, DATA_UUID_SUFFIX)
    private val frameParser = WendougeeFrameParser { update -> _machineState.update { it.update() } }
    private val profileCompiler = WendougeeProfileCompiler()

    private var pollingJob: Job? = null
    private var connectionStateJob: Job? = null

    companion object {
        private const val TAG = "WendougeeController"

        const val DATA_UUID_SUFFIX = "2b10"
        const val CTRL_UUID_SUFFIX = "2c10"

        private const val POLLING_INTERVAL_MS = 200L
        private const val BREW_PULSE_MS = 100L
    }

    init {
        observeConnectionState()
        observeIncomingData()
    }

    private fun observeConnectionState() {
        connectionStateJob = scope.launch {
            bleClient.connectionState.collect { state ->
                val status = when (state) {
                    BleClient.ConnectionState.Disconnected -> {
                        pollingJob?.cancel()
                        _machineState.update {
                            it.copy(
                                connectionStatus = ConnectionStatus.Disconnected,
                                pressure = null,
                                steamBoilerTemp = null,
                                brewBoilerTemp = null
                            )
                        }
                        ConnectionStatus.Disconnected
                    }

                    BleClient.ConnectionState.Connecting -> ConnectionStatus.Connecting
                    BleClient.ConnectionState.Connected -> {
                        startDeviceInitialization()
                        ConnectionStatus.Connected
                    }
                }
                _machineState.update { it.copy(connectionStatus = status) }
            }
        }
    }

    private fun observeIncomingData() {
        scope.launch {
            bleClient.incomingData.collect { (_, data) ->
                if (data.isNotEmpty()) {
                    frameParser.handleIncomingFrame(data)
                }
            }
        }
    }

    private fun startDeviceInitialization() {
        if (pollingJob?.isActive == true) return
        Logger.withTag(TAG).i { "Starting device initialization sequence" }

        pollingJob = scope.launch {
            Logger.withTag(TAG).d { "Negotiating MTU (512)" }
            val mtuSet = withTimeoutOrNull(4000) {
                while (!bleClient.changeMTU(512)) {
                    delay(200)
                }
                true
            }
            if (mtuSet == null) Logger.withTag(TAG).w { "Failed to set MTU" }
            delay(500)

            Logger.withTag(TAG).d { "Waiting for characteristics..." }
            val charsReady = withTimeoutOrNull(10000) {
                while (isActive) {
                    val dataReady = bleClient.notifyCharacteristic(DATA_UUID_SUFFIX, true)
                    val ctrlReady = bleClient.notifyCharacteristic(CTRL_UUID_SUFFIX, true)
                    if (dataReady && ctrlReady) return@withTimeoutOrNull true
                    delay(200)
                }
                false
            }

            if (charsReady != true) {
                Logger.withTag(TAG).e { "Characteristics not found, initialization aborted. Disconnecting to reset state." }
                bleClient.disconnect()
                return@launch
            }

            delay(300)

            Logger.withTag(TAG).d { "Sending proprietary INIT handshake" }
            bleClient.writeCharacteristic(CTRL_UUID_SUFFIX, CMD_INIT_HANDSHAKE)

            delay(300)

            Logger.withTag(TAG).d { "Requesting static configuration" }
            bleClient.writeCharacteristic(DATA_UUID_SUFFIX, CMD_READ_CONFIG_LONG)

            delay(500)

            while (isActive) {
                pollData()
                delay(POLLING_INTERVAL_MS)
            }
        }
    }

    private suspend fun pollData() {
        try {
            modbus.sendCustomCommandAndWaitForPrefix(
                payload = CMD_POLLING_LONG,
                expectedPrefix = byteArrayOf(0x01, 0x03, 0x28),
                timeoutMs = 400
            )
        } catch (exception: Exception) {
            Logger.withTag(TAG).e(exception) { "Poll Long missed (timeout or error)" }
        }
        delay(30)
        try {
            modbus.sendCustomCommandAndWaitForPrefix(
                payload = CMD_POLLING_SHORT,
                expectedPrefix = byteArrayOf(0x01, 0x01),
                timeoutMs = 400
            )
        } catch (exception: Exception) {
            Logger.withTag(TAG).e(exception) { "Poll Short missed (timeout or error)" }
        }
        delay(20)
    }

    override fun connect(macAddress: String) {
        bleClient.connect(macAddress)
    }

    override fun disconnect() {
        bleClient.disconnect()
    }

    override suspend fun setBoilerState(boilerType: BoilerType, enabled: Boolean) {
        val targetRegister =
            if (boilerType == BoilerType.Steam) WendougeeRegisters.STEAM_BOILER_STATE else WendougeeRegisters.BREW_BOILER_STATE
        val stateValue = if (enabled) 0x00 else 0x01

        modbus.writeSingleRegister(targetRegister, stateValue)

        Logger.withTag(TAG).i { "Boiler ${if (boilerType == BoilerType.Steam) "Steam" else "Brew"} set to $enabled confirmed" }
        _machineState.update { currentState ->
            val currentConfig = currentState.config ?: return@update currentState
            val newConfig = if (boilerType == BoilerType.Steam) {
                currentConfig.copy(steamBoilerEnabled = enabled)
            } else {
                currentConfig.copy(brewBoilerEnabled = enabled)
            }
            currentState.copy(config = newConfig)
        }
    }

    override suspend fun setSteamTemperature(temp: Int) {
        if (temp !in 0..140) {
            Logger.withTag(TAG).e { "Temperature $temp out of safe range!" }
            return
        }

        modbus.writeSingleRegister(WendougeeRegisters.STEAM_TEMPERATURE, temp)
        Logger.withTag(TAG).i { "Steam temperature set to $temp°C confirmed" }
        _machineState.update { currentState ->
            val currentConfig = currentState.config ?: return@update currentState
            currentState.copy(config = currentConfig.copy(targetSteamTemp = temp.toFloat()))
        }
    }

    override suspend fun setBrewTemperature(temp: Int) {
        if (temp !in 0..110) {
            Logger.withTag(TAG).e { "Brew temperature $temp out of range!" }
            return
        }

        modbus.writeSingleRegister(WendougeeRegisters.BREW_TEMPERATURE, temp)
        Logger.withTag(TAG).i { "Brew temperature set to $temp°C confirmed" }
        _machineState.update { currentState ->
            val currentConfig = currentState.config ?: return@update currentState
            currentState.copy(config = currentConfig.copy(targetBrewTemp = temp.toFloat()))
        }
    }

    override suspend fun startManualBrewing() {
        if (_machineState.value.brewStatus == BrewStatus.Idle) {
            Logger.withTag(TAG).i { "Starting manual brew cycle..." }
            sendModbusPulse(CMD_MANUAL_ON, CMD_MANUAL_OFF, "Manual Brew", 0x00, 0x9A.toByte())
        }
    }

    override suspend fun stopManualBrewing() {
        if (_machineState.value.brewStatus == BrewStatus.Manual) {
            Logger.withTag(TAG).i { "Stopping manual brew cycle..." }
            sendModbusPulse(CMD_MANUAL_ON, CMD_MANUAL_OFF, "Manual Brew Stop", 0x00, 0x9A.toByte())
        }
    }

    override suspend fun triggerShortPress() {
        sendModbusPulse(CMD_SHORT_PRESS_ON, CMD_SHORT_PRESS_OFF, "Short Press", 0x00, 0x96.toByte())
    }

    override suspend fun startCleaning() {
        if (_machineState.value.brewStatus == BrewStatus.Idle) {
            Logger.withTag(TAG).i { "Sending start signal for cleaning..." }
            sendModbusPulse(CMD_CLEANING_ON, CMD_CLEANING_OFF, "Cleaning Procedure", 0x00, 0x9B.toByte())
        }
    }

    override suspend fun stopCleaning() {
        if (_machineState.value.brewStatus == BrewStatus.Cleaning) {
            Logger.withTag(TAG).i { "Sending stop signal for cleaning..." }
            sendModbusPulse(CMD_CLEANING_ON, CMD_CLEANING_OFF, "Stop Cleaning", 0x00, 0x9B.toByte())
        }
    }

    override suspend fun setHeatingMode(heatingMode: HeatingMode) {
        val modeValue = if (heatingMode == HeatingMode.FullSpeed) 0x01 else 0x00
        modbus.writeSingleRegister(WendougeeRegisters.HEATING_MODE, modeValue)
        Logger.withTag(TAG).i { "Heating mode set to $heatingMode confirmed" }
        _machineState.update { it.copy(config = it.config?.copy(heatingMode = heatingMode)) }
    }

    private suspend fun sendModbusPulse(onCommand: ByteArray, offCommand: ByteArray, label: String, regHi: Byte, regLo: Byte) {
        modbus.writeAndAwaitModbus(onCommand, 0x05, regHi, regLo)
        delay(BREW_PULSE_MS)
        modbus.writeAndAwaitModbus(offCommand, 0x05, regHi, regLo)
        Logger.withTag(TAG).i { "$label pulse completed and confirmed" }
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

    override suspend fun bindProfile(profile: BrewProfile) {
        Logger.withTag(TAG).i { "Binding profile to button: ${profile.name}" }

        val commands = profileCompiler.buildProfileUploadCommands(profile)
        for (cmd in commands) {
            modbus.writeAndAwaitModbus(
                cmd.payload,
                expectedFc = cmd.expectedFc,
                expectedRegHi = cmd.regHi,
                expectedRegLo = cmd.regLo
            )
        }

        Logger.withTag(TAG).i { "Profile uploaded, sending bind command to register ${WendougeeRegisters.BIND_PROFILE}" }
        modbus.writeMultipleRegisters(WendougeeRegisters.BIND_PROFILE, listOf(1))
    }
}
