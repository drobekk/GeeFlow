@file:Suppress("TooManyFunctions")
@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package app.geeflow.data.device.impl.controller

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.LiveBrewSession
import app.geeflow.data.device.ble.modbus.ModbusPlugin
import app.geeflow.data.device.ble.modbus.ModbusSession
import app.geeflow.data.device.impl.controller.WendougeeCommands.CMD_CLEANING_OFF
import app.geeflow.data.device.impl.controller.WendougeeCommands.CMD_CLEANING_ON
import app.geeflow.data.device.impl.controller.WendougeeCommands.CMD_FREE_VAR_OFF
import app.geeflow.data.device.impl.controller.WendougeeCommands.CMD_FREE_VAR_ON
import app.geeflow.data.device.impl.controller.WendougeeCommands.CMD_MANUAL_OFF
import app.geeflow.data.device.impl.controller.WendougeeCommands.CMD_MANUAL_ON
import app.geeflow.data.device.impl.controller.WendougeeCommands.CMD_POLLING_LONG
import app.geeflow.data.device.impl.controller.WendougeeCommands.CMD_POLLING_SHORT
import app.geeflow.data.device.impl.controller.WendougeeCommands.CMD_READ_CONFIG_LONG
import app.geeflow.data.device.impl.controller.WendougeeCommands.CMD_SCALE_LIST_REQUEST
import app.geeflow.data.device.impl.controller.WendougeeCommands.CMD_SCALE_SEARCH_OFF
import app.geeflow.data.device.impl.controller.WendougeeCommands.CMD_SCALE_SEARCH_ON
import app.geeflow.data.device.impl.controller.WendougeeCommands.CMD_SCALE_SEARCH_QUERY
import app.geeflow.data.device.impl.controller.WendougeeCommands.CMD_SCALE_STATUS_REQUEST
import app.geeflow.data.device.impl.controller.WendougeeCommands.CMD_SHORT_PRESS_OFF
import app.geeflow.data.device.impl.controller.WendougeeCommands.CMD_SHORT_PRESS_ON
import app.geeflow.data.device.impl.controller.WendougeeCommands.CMD_START_STREAMING
import app.geeflow.data.device.impl.discovery.BleAdvertisement
import app.geeflow.data.device.impl.discovery.WendougeeBleDeviceDiscoverer
import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceCapability
import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.data.device.model.DeviceConstraints
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.DeviceState.BoilerType
import app.geeflow.data.device.model.DeviceState.BrewStatus
import app.geeflow.data.device.model.DeviceState.ConnectionStatus
import app.geeflow.data.device.model.DeviceState.HeatingMode
import app.geeflow.data.device.model.SmartScale
import app.geeflow.data.device.model.pumpTelemetry
import co.touchlab.kermit.Logger
import dev.bluefalcon.core.BlueFalcon
import dev.bluefalcon.core.BluetoothCharacteristic
import dev.bluefalcon.core.BluetoothPeripheral
import dev.bluefalcon.core.BluetoothPeripheralState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
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
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class WendougeeDataSController(
    private val scope: CoroutineScope,
    private val blueFalcon: BlueFalcon,
    private val modbusPlugin: ModbusPlugin,
    private val discoverer: WendougeeBleDeviceDiscoverer,
) : DeviceController {
    override suspend fun stopLiveSession() = stopFreeVariableBrewing()
    override fun isLiveSessionActive(state: DeviceState) = state.brewStatus == DeviceState.BrewStatus.FreeVariable
    override val profilingCapabilities = WendougeeProfiling.capabilities
    override fun assessNativeProfile(profile: BrewProfile) = WendougeeProfiling.assessNative(profile)
    override fun telemetry() = deviceState.value.pumpTelemetry()
    override suspend fun openLiveSession(initial: PhaseControl): LiveBrewSession {
        startFreeVariableBrewing(isFlow = false)
        return WendougeePressureSession(controller = this)
    }

    private val _deviceState = MutableStateFlow(DeviceState())
    override val deviceState: StateFlow<DeviceState> = _deviceState.asStateFlow()

    private val _foundScales = MutableStateFlow<List<SmartScale>>(emptyList())
    override val foundScales: StateFlow<List<SmartScale>> = _foundScales.asStateFlow()

    private val _resolvedConnection = MutableSharedFlow<DeviceConnection>(extraBufferCapacity = 1)
    override val resolvedConnection: SharedFlow<DeviceConnection> = _resolvedConnection.asSharedFlow()

    override val constraints = wendougeeConstraints
    override val capabilities = wendougeeCapabilities

    var logPolling: Boolean = false

    private val frameParser = WendougeeFrameParser(
        onStateUpdate = { update ->
            _deviceState.update { currentState -> currentState.update() }
        },
        onScaleFound = { scale ->
            _foundScales.update { current ->
                if (current.any { it.name == scale.name }) {
                    current.map { if (it.name == scale.name) scale else it }
                } else {
                    current + scale
                }
            }
        },
        onHeartbeat = { onHeartbeatReceived() },
        shouldLogPolling = { logPolling },
    )

    private var connectJob: Job? = null
    private var watchdogJob: Job? = null
    private var notificationJob: Job? = null
    private var heartbeatTimeoutJob: Job? = null

    private var session: Session? = null

    private class Session(
        val peripheral: BluetoothPeripheral,
        val dataChar: BluetoothCharacteristic,
        val ctrlChar: BluetoothCharacteristic,
        val modbus: ModbusSession,
    )

    private fun onHeartbeatReceived() {
        if (!_deviceState.value.smartScaleEnabled) return
        _deviceState.update { it.copy(smartScaleSearchActive = true) }
        heartbeatTimeoutJob?.cancel()
        heartbeatTimeoutJob = scope.launch {
            delay(HEARTBEAT_TIMEOUT_MS)
            _deviceState.update { it.copy(smartScaleSearchActive = false) }
        }
    }

    @Suppress("LongMethod")
    override fun connect(device: Device) {
        val ble = device.connection as? DeviceConnection.Ble
            ?: error("WendougeeDataSController requires DeviceConnection.Ble")

        connectJob?.cancel()
        connectJob = scope.launch {
            try {
                _deviceState.update { it.copy(connectionStatus = ConnectionStatus.Connecting) }

                val peripheral = resolvePeripheral(ble)
                if (peripheral.uuid != ble.peripheralId) {
                    Logger.withTag(TAG).i { "peripheralId changed: ${ble.peripheralId} → ${peripheral.uuid}" }
                    _resolvedConnection.emit(ble.copy(peripheralId = peripheral.uuid))
                }

                // Flush any stale GATT from a prior session. Without this, Android's addGatt()
                // guard silently drops the new handle if the old onConnectionStateChange(DISCONNECTED)
                // hasn't fired yet — subsequent writes go to the dead GATT and no-op.
                runCatching { blueFalcon.disconnect(peripheral) }
                delay(STALE_GATT_SETTLE_MS)

                blueFalcon.connect(peripheral)

                if (!awaitConnected(peripheral)) {
                    Logger.withTag(TAG).e { "Connection did not reach Connected state — aborting" }
                    runCatching { blueFalcon.disconnect(peripheral) }
                    resetToDisconnected()
                    return@launch
                }

                val (dataChar, ctrlChar) = awaitCharacteristics(peripheral)
                    ?: run {
                        Logger.withTag(TAG).e { "Characteristics not ready — aborting connect" }
                        blueFalcon.disconnect(peripheral)
                        resetToDisconnected()
                        return@launch
                    }

                tryChangeMtu(peripheral)

                // Disable notifications before enabling to guarantee the CCCD 0x0000 → 0x0001
                // write reaches the device on every connection regardless of cached BLE stack state.
                blueFalcon.notifyCharacteristic(peripheral, dataChar, notify = false)
                blueFalcon.notifyCharacteristic(peripheral, ctrlChar, notify = false)
                delay(NOTIFY_RESET_DELAY_MS)
                blueFalcon.notifyCharacteristic(peripheral, dataChar, notify = true)
                blueFalcon.notifyCharacteristic(peripheral, ctrlChar, notify = true)

                val modbus = modbusPlugin.session(
                    peripheral = peripheral,
                    requestCharacteristic = dataChar,
                    responseCharacteristic = dataChar,
                    unitId = MODBUS_UNIT_ID,
                )
                val active = Session(peripheral, dataChar, ctrlChar, modbus)
                session = active

                startObservingNotifications(active)
                startWatchdog(peripheral)

                _deviceState.update { it.copy(connectionStatus = ConnectionStatus.Synchronizing) }

                sendInitCommands(active)

                _deviceState.update { it.copy(connectionStatus = ConnectionStatus.Connected) }
                Logger.withTag(TAG).i { "Connected and ready" }

                runPollingLoop(active)
            } catch (e: TimeoutCancellationException) {
                Logger.withTag(TAG).e { "Connect timed out during ${_deviceState.value.connectionStatus}" }
                session?.peripheral?.let { runCatching { blueFalcon.disconnect(it) } }
                resetToDisconnected()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.withTag(TAG).e(e) { "Connect flow failed" }
                session?.peripheral?.let { runCatching { blueFalcon.disconnect(it) } }
                resetToDisconnected()
            }
        }
    }

    /**
     * On Android, BlueFalcon's engine only populates service/characteristic data on peripherals
     * already present in its `peripherals` StateFlow. `retrievePeripheral()` hands back a detached
     * instance, so callbacks silently drop updates — we must use a scan-cached instance instead.
     * 1) Reuse the cached peripheral if still present (e.g. from pairing's scan or a prior session).
     * 2) Otherwise scan for the MAC (which registers the peripheral in the engine cache).
     */
    private suspend fun resolvePeripheral(ble: DeviceConnection.Ble): BluetoothPeripheral {
        blueFalcon.peripherals.value.firstOrNull { it.uuid == ble.peripheralId }?.let { return it }
        Logger.withTag(TAG).w {
            "Peripheral ${ble.peripheralId} not in cache; scanning for MAC=${ble.macAddress}"
        }
        return scanForMac(ble.macAddress, timeoutMs = SCAN_TIMEOUT_MS)
            ?: error("Device with MAC ${ble.macAddress} not found via scan")
    }

    private suspend fun scanForMac(targetMac: String, timeoutMs: Long): BluetoothPeripheral? {
        val scanJob = scope.launch { blueFalcon.scan() }
        return try {
            withTimeoutOrNull(timeoutMs) {
                blueFalcon.peripherals
                    .mapNotNull { set ->
                        set.firstOrNull { p ->
                            discoverer.macAddressOf(BleAdvertisement(p.uuid, p.name)) == targetMac
                        }
                    }
                    .first()
            }
        } finally {
            scanJob.cancel()
            blueFalcon.stopScanning()
        }
    }

    private suspend fun awaitConnected(peripheral: BluetoothPeripheral): Boolean =
        withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
            while (isActive) {
                if (blueFalcon.connectionState(peripheral) == BluetoothPeripheralState.Connected) {
                    return@withTimeoutOrNull true
                }
                delay(CONNECT_RETRY_DELAY_MS)
            }
            false
        } ?: false

    private suspend fun awaitCharacteristics(
        peripheral: BluetoothPeripheral,
    ): Pair<BluetoothCharacteristic, BluetoothCharacteristic>? = withTimeoutOrNull(CHARS_READY_TIMEOUT_MS) {
        delay(POST_CONNECT_SETTLE_MS)
        Logger.withTag(TAG).d { "Triggering discoverServices" }
        runCatching { blueFalcon.discoverServices(peripheral) }
            .onFailure { Logger.withTag(TAG).w(it) { "discoverServices threw" } }
        val requested = mutableSetOf<String>()
        var lastServiceCount = -1
        var lastCharCount = -1
        while (isActive) {
            for (service in peripheral.services) {
                if (requested.add(service.uuid.toString())) {
                    Logger.withTag(TAG).d { "discoverCharacteristics for ${service.uuid}" }
                    runCatching { blueFalcon.discoverCharacteristics(peripheral, service) }
                        .onFailure { Logger.withTag(TAG).w(it) { "discoverCharacteristics threw" } }
                }
            }
            val services = peripheral.services.size
            val chars = peripheral.characteristics.size
            if (services != lastServiceCount || chars != lastCharCount) {
                Logger.withTag(TAG).d { "services=$services chars=$chars" }
                lastServiceCount = services
                lastCharCount = chars
            }
            val data = peripheral.findBySuffix(DATA_UUID_SUFFIX)
            val ctrl = peripheral.findBySuffix(CTRL_UUID_SUFFIX)
            if (data != null && ctrl != null) return@withTimeoutOrNull data to ctrl
            delay(CHARS_RETRY_DELAY_MS)
        }
        null
    }

    private suspend fun tryChangeMtu(peripheral: BluetoothPeripheral) {
        val result = withTimeoutOrNull(MTU_TIMEOUT_MS) {
            runCatching { blueFalcon.changeMTU(peripheral, MTU_SIZE) }
        }
        if (result == null || result.isFailure) {
            Logger.withTag(TAG).w { "MTU negotiation failed or timed out" }
        }
        delay(POST_MTU_DELAY_MS)
    }

    private fun startObservingNotifications(active: Session) {
        notificationJob?.cancel()
        val dataUuid = active.dataChar.uuid
        val ctrlUuid = active.ctrlChar.uuid
        notificationJob = scope.launch {
            // Route by characteristic UUID via the engine's global notification bus rather than
            // the instance-keyed BluetoothCharacteristic.notifications SharedFlow. This survives
            // a late-firing onServicesDiscovered that replaces _servicesFlow with new instances.
            blueFalcon.engine.characteristicNotifications.collect { notification ->
                if (notification.peripheral.uuid != active.peripheral.uuid) return@collect
                val bytes = notification.value
                if (bytes.isEmpty()) return@collect
                val channel = when (notification.characteristic.uuid) {
                    ctrlUuid -> "CTRL"
                    dataUuid -> "DATA"
                    else -> return@collect
                }
                frameParser.handleIncomingFrame(bytes, channel)
            }
        }
    }

    private fun startWatchdog(peripheral: BluetoothPeripheral) {
        watchdogJob?.cancel()
        watchdogJob = scope.launch {
            while (isActive) {
                delay(WATCHDOG_INTERVAL_MS)
                if (blueFalcon.connectionState(peripheral) == BluetoothPeripheralState.Disconnected) {
                    val ours = _deviceState.value.connectionStatus
                    if (ours != ConnectionStatus.Disconnected) {
                        Logger.withTag(TAG).w { "BLE dropped unexpectedly during $ours — resetting" }
                        connectJob?.cancel()
                        resetToDisconnected()
                    }
                    break
                }
            }
        }
    }

    private suspend fun sendInitCommands(active: Session) {
        delay(CCCD_SETTLE_DELAY_MS)
        write(active, active.dataChar, CMD_READ_CONFIG_LONG)
        delay(INIT_CONFIG_DELAY_MS)
        readWaterAlarmRegister(active)
        write(active, active.ctrlChar, CMD_SCALE_SEARCH_QUERY)
        delay(INIT_SCALE_DELAY_MS)
        write(active, active.ctrlChar, CMD_START_STREAMING)
        if (_deviceState.value.smartScaleEnabled) {
            delay(SCALE_STATUS_DELAY_MS)
            write(active, active.ctrlChar, CMD_SCALE_STATUS_REQUEST)
            delay(SCALE_LIST_DELAY_MS)
            write(active, active.ctrlChar, CMD_SCALE_LIST_REQUEST)
        }
        delay(POST_INIT_DELAY_MS)

        pollLongOnce(active, timeout = INIT_POLL_TIMEOUT_MS)
        pollShortOnce(active, timeout = INIT_POLL_TIMEOUT_MS)
    }

    private suspend fun readWaterAlarmRegister(active: Session) {
        val enabled = runCatching {
            active.modbus.readHoldingRegisters(WendougeeRegisters.WATER_ALARM, 1).first() != 0
        }.getOrNull() ?: return
        Logger.withTag(TAG).i { "Water alarm enabled=$enabled" }
        _deviceState.update { state ->
            val config = state.config ?: return@update state
            state.copy(config = config.copy(waterAlarmEnabled = enabled))
        }
    }

    private suspend fun runPollingLoop(active: Session) {
        while (currentCoroutineIsActive()) {
            runCatching { pollLongOnce(active) }
                .onFailure { Logger.withTag(TAG).w(it) { "Poll long missed" } }
            delay(POLL_BETWEEN_DELAY_MS)
            runCatching { pollShortOnce(active) }
                .onFailure { Logger.withTag(TAG).w(it) { "Poll short missed" } }
            delay(POLLING_INTERVAL_MS)
        }
    }

    override fun disconnect() {
        Logger.withTag(TAG).i { "Disconnecting" }
        connectJob?.cancel()
        val p = session?.peripheral
        scope.launch {
            if (p != null) runCatching { blueFalcon.disconnect(p) }
            resetToDisconnected()
        }
    }

    private fun resetToDisconnected() {
        heartbeatTimeoutJob?.cancel()
        watchdogJob?.cancel()
        notificationJob?.cancel()
        session = null
        _deviceState.update {
            it.copy(
                connectionStatus = ConnectionStatus.Disconnected,
                config = null,
                pressure = null,
                steamBoilerTemp = null,
                brewBoilerTemp = null,
                smartScale = null,
                smartScaleSearchActive = false,
                waterLevelAlarm = false,
            )
        }
        _foundScales.value = emptyList()
    }

    private suspend fun pollLongOnce(active: Session, timeout: Long = POLL_TIMEOUT_MS) {
        active.modbus.sendAndAwaitPrefix(
            payload = CMD_POLLING_LONG,
            expectedPrefix = POLL_LONG_PREFIX,
            timeout = timeout.milliseconds,
        )
    }

    private suspend fun pollShortOnce(active: Session, timeout: Long = POLL_TIMEOUT_MS) {
        active.modbus.sendAndAwaitPrefix(
            payload = CMD_POLLING_SHORT,
            expectedPrefix = POLL_SHORT_PREFIX,
            timeout = timeout.milliseconds,
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

        requireSession().modbus.writeSingleRegister(targetRegister, stateValue)

        Logger.withTag(
            TAG,
        ).i { "Boiler ${if (boilerType == BoilerType.Steam) "Steam" else "Brew"} set to $enabled confirmed" }
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
        if (temp !in 0..STEAM_MAX_TEMP) {
            Logger.withTag(TAG).e { "Temperature $temp out of safe range!" }
            return
        }
        if (_deviceState.value.config?.targetSteamTemp?.toInt() == temp) return

        requireSession().modbus.writeSingleRegister(WendougeeRegisters.STEAM_TEMPERATURE, temp)
        Logger.withTag(TAG).i { "Steam temperature set to $temp°C confirmed" }
        _deviceState.update { currentState ->
            val config = currentState.config ?: return@update currentState
            currentState.copy(config = config.copy(targetSteamTemp = temp.toFloat()))
        }
    }

    override suspend fun setBrewTemperature(temp: Int) {
        if (temp !in 0..BREW_MAX_TEMP) {
            Logger.withTag(TAG).e { "Brew temperature $temp out of range!" }
            return
        }
        if (_deviceState.value.config?.targetBrewTemp?.toInt() == temp) return

        requireSession().modbus.writeSingleRegister(WendougeeRegisters.BREW_TEMPERATURE, temp)
        Logger.withTag(TAG).i { "Brew temperature set to $temp°C confirmed" }
        _deviceState.update { currentState ->
            val config = currentState.config ?: return@update currentState
            currentState.copy(config = config.copy(targetBrewTemp = temp.toFloat()))
        }
    }

    override suspend fun startManualBrewing() {
        if (_deviceState.value.brewStatus == BrewStatus.Idle) {
            Logger.withTag(TAG).i { "Starting manual brew cycle..." }
            sendModbusPulse(CMD_MANUAL_ON, CMD_MANUAL_OFF, "Manual Brew", 0x00, COIL_MANUAL_BREW.toByte())
        }
    }

    override suspend fun stopManualBrewing() {
        if (_deviceState.value.brewStatus == BrewStatus.Manual) {
            Logger.withTag(TAG).i { "Stopping manual brew cycle..." }
            sendModbusPulse(CMD_MANUAL_ON, CMD_MANUAL_OFF, "Manual Brew Stop", 0x00, COIL_MANUAL_BREW.toByte())
        }
    }

    override suspend fun startFreeVariableBrewing(isFlow: Boolean) {
        if (_deviceState.value.brewStatus == BrewStatus.Idle) {
            Logger.withTag(TAG).i { "Starting free variable brew (isFlow=$isFlow)..." }
            _deviceState.update {
                it.copy(
                    freeHandControlMode = if (isFlow) FreeHandControlMode.Flow else FreeHandControlMode.Pressure,
                    freeHandStopRequested = false,
                )
            }
            requireSession().modbus.writeSingleRegister(WendougeeRegisters.FREE_VAR_PREPARE, FREE_VAR_PREPARE_VALUE)
            requireSession().modbus.writeSingleRegister(WendougeeRegisters.FREE_VAR_MODE, if (isFlow) 1 else 0,)
            sendModbusPulse(CMD_FREE_VAR_ON, CMD_FREE_VAR_OFF, "Free Variable Brew", 0x00, COIL_FREE_VAR_BREW.toByte())
            requireSession().modbus.writeMultipleRegisters(WendougeeRegisters.FREE_VAR_TARGET_BASE, listOf(0, 0))
        }
    }

    override suspend fun stopFreeVariableBrewing() {
        if (_deviceState.value.brewStatus == BrewStatus.FreeVariable) {
            _deviceState.update { it.copy(freeHandStopRequested = true) }
            Logger.withTag(TAG).i { "Stopping free variable brew..." }
            requireSession().modbus.writeSingleRegister(
                WendougeeRegisters.FREE_VAR_MODE,
                if (_deviceState.value.freeHandControlMode == FreeHandControlMode.Flow) 1 else 0,
            )
            sendModbusPulse(
                CMD_FREE_VAR_ON,
                CMD_FREE_VAR_OFF,
                "Free Variable Brew Stop",
                0x00,
                COIL_FREE_VAR_BREW.toByte()
            )
        }
    }

    override suspend fun setFreeBrewPressureTarget(pressure: Float) {
        requireSession().modbus.writeMultipleRegisters(
            WendougeeRegisters.FREE_VAR_TARGET_BASE,
            listOf((pressure * SENSOR_SCALE_FACTOR).toInt(), 0),
        )
        Logger.withTag(TAG).d { "Free variable pressure target set to $pressure bar" }
    }

    override suspend fun setFreeBrewFlowTarget(flow: Float) {
        requireSession().modbus.writeMultipleRegisters(
            WendougeeRegisters.FREE_VAR_TARGET_BASE,
            listOf(0, (flow * SENSOR_SCALE_FACTOR).toInt()),
        )
        Logger.withTag(TAG).d { "Free variable flow target set to $flow ml/s" }
    }

    override suspend fun stopProfileBrewing() {
        if (_deviceState.value.brewStatus == BrewStatus.Profile) {
            Logger.withTag(TAG).i { "Stopping profile brew cycle..." }
            sendModbusPulse(CMD_SHORT_PRESS_ON, CMD_SHORT_PRESS_OFF, "Short Press", 0x00, COIL_SHORT_PRESS.toByte())
        }
    }

    override suspend fun startCleaning() {
        if (_deviceState.value.brewStatus == BrewStatus.Idle) {
            Logger.withTag(TAG).i { "Sending start signal for cleaning..." }
            sendModbusPulse(CMD_CLEANING_ON, CMD_CLEANING_OFF, "Cleaning Procedure", 0x00, COIL_CLEANING.toByte())
        }
    }

    override suspend fun stopCleaning() {
        if (_deviceState.value.brewStatus == BrewStatus.Cleaning) {
            Logger.withTag(TAG).i { "Sending stop signal for cleaning..." }
            sendModbusPulse(CMD_CLEANING_ON, CMD_CLEANING_OFF, "Stop Cleaning", 0x00, COIL_CLEANING.toByte())
        }
    }

    override suspend fun setHeatingMode(heatingMode: HeatingMode) {
        if (_deviceState.value.config?.heatingMode == heatingMode) return

        val modeValue = if (heatingMode == HeatingMode.FullSpeed) 0x01 else 0x00
        requireSession().modbus.writeSingleRegister(WendougeeRegisters.HEATING_MODE, modeValue)
        Logger.withTag(TAG).i { "Heating mode set to $heatingMode confirmed" }
        _deviceState.update { it.copy(config = it.config?.copy(heatingMode = heatingMode)) }
    }

    override suspend fun setManualBrewPressure(pressure: Float) {
        if (_deviceState.value.config?.manualBrewPressure == pressure) return

        requireSession().modbus.writeMultipleRegisters(
            WendougeeRegisters.MANUAL_BREW_PRESSURE,
            listOf((pressure * SENSOR_SCALE_FACTOR).toInt()),
        )
        Logger.withTag(TAG).d { "Manual brew pressure set to $pressure bar confirmed" }
        _deviceState.update { state ->
            val config = state.config ?: return@update state
            state.copy(config = config.copy(manualBrewPressure = pressure))
        }
    }

    override suspend fun setManualBrewTime(timeSec: Float) {
        if (_deviceState.value.config?.manualBrewTimeSec == timeSec) return

        requireSession().modbus.writeMultipleRegisters(
            WendougeeRegisters.MANUAL_BREW_TIME,
            listOf((timeSec * SENSOR_SCALE_FACTOR).toInt()),
        )
        Logger.withTag(TAG).d { "Manual brew time set to $timeSec s confirmed" }
        _deviceState.update { state ->
            val config = state.config ?: return@update state
            state.copy(config = config.copy(manualBrewTimeSec = timeSec))
        }
    }

    override suspend fun setCleaningSettings(timeSec: Float, standbySec: Float, count: Int) {
        val modbus = requireSession().modbus
        modbus.writeMultipleRegisters(WendougeeRegisters.CLEANING_TIME, listOf((timeSec * SENSOR_SCALE_FACTOR).toInt()))
        modbus.writeMultipleRegisters(
            WendougeeRegisters.CLEANING_STANDBY_TIME,
            listOf((standbySec * SENSOR_SCALE_FACTOR).toInt()),
        )
        modbus.writeMultipleRegisters(WendougeeRegisters.CLEANING_COUNT, listOf(count))
        Logger.withTag(TAG).i { "Cleaning settings: time=${timeSec}s standby=${standbySec}s count=$count" }
        _deviceState.update { state ->
            val config = state.config ?: return@update state
            state.copy(
                config = config.copy(cleaningTimeSec = timeSec, cleaningStandbySec = standbySec, cleaningCount = count),
            )
        }
    }

    override suspend fun setWaterAlarm(enabled: Boolean) {
        if (_deviceState.value.config?.waterAlarmEnabled == enabled) return
        requireSession().modbus.writeMultipleRegisters(WendougeeRegisters.WATER_ALARM, listOf(if (enabled) 1 else 0))
        Logger.withTag(TAG).d { "Water alarm set to $enabled" }
        _deviceState.update { state ->
            val config = state.config ?: return@update state
            state.copy(config = config.copy(waterAlarmEnabled = enabled))
        }
    }

    private suspend fun sendModbusPulse(
        onCommand: ByteArray,
        offCommand: ByteArray,
        label: String,
        regHi: Byte,
        regLo: Byte,
    ) {
        val modbus = requireSession().modbus
        val matcher = ModbusSession.AddressMatcher(hi = regHi, lo = regLo)
        modbus.sendAndAwaitFc(onCommand, FC_COIL_WRITE, matcher)
        delay(BREW_PULSE_MS)
        modbus.sendAndAwaitFc(offCommand, FC_COIL_WRITE, matcher)
        Logger.withTag(TAG).d { "$label pulse completed and confirmed" }
    }

    override suspend fun startProfileBrewing(profile: BrewProfile) {
        Logger.withTag(TAG).i { "Starting profile brew: ${profile.name}" }

        requireSession().modbus.uploadProfile(profile, isBinding = false)

        Logger.withTag(TAG).d { "Profile uploaded, triggering brew pulse" }
        if (profile.recording != null) {
            val modbus = requireSession().modbus
            modbus.writeSingleCoil(COIL_PROFILE_FREE, true)
            delay(BREW_PULSE_MS)
            modbus.writeSingleCoil(COIL_PROFILE_FREE, false)
            sendModbusPulse(CMD_SHORT_PRESS_ON, CMD_SHORT_PRESS_OFF, "Short Press", 0x00, COIL_SHORT_PRESS.toByte())
        } else {
            sendModbusPulse(CMD_SHORT_PRESS_ON, CMD_SHORT_PRESS_OFF, "Short Press", 0x00, COIL_SHORT_PRESS.toByte())
        }
    }

    override suspend fun bindProfile(profile: BrewProfile) {
        Logger.withTag(TAG).i { "Binding profile to button: ${profile.name}" }

        requireSession().modbus.uploadProfile(profile, isBinding = true)

        Logger.withTag(
            TAG,
        ).d { "Profile uploaded, sending bind command to register ${WendougeeRegisters.BIND_PROFILE}" }
        requireSession().modbus.writeMultipleRegisters(WendougeeRegisters.BIND_PROFILE, listOf(1))
    }

    override suspend fun setSmartScaleConnectivity(enabled: Boolean) {
        Logger.withTag(TAG).i { "Smart scale connectivity: $enabled" }
        _deviceState.update { it.copy(smartScaleEnabled = enabled) }
        val active = requireSession()
        if (enabled) {
            _foundScales.value = emptyList()
            write(active, active.ctrlChar, CMD_SCALE_SEARCH_ON)
            delay(SCALE_SEARCH_AFTER_ENABLE_DELAY_MS)
            write(active, active.ctrlChar, CMD_SCALE_LIST_REQUEST)
        } else {
            write(active, active.ctrlChar, CMD_SCALE_SEARCH_OFF)
        }
    }

    override suspend fun requestSmartScaleList() {
        Logger.withTag(TAG).d { "Requesting smart scale list..." }
        val active = requireSession()
        write(active, active.ctrlChar, CMD_SCALE_LIST_REQUEST)
    }

    override suspend fun connectSmartScale(name: String) {
        Logger.withTag(TAG).i { "Connecting smart scale: $name" }
        val active = requireSession()
        write(active, active.ctrlChar, buildScaleFrame(SCALE_CMD_CONNECT, name))
    }

    override suspend fun disconnectSmartScale() {
        val name = _deviceState.value.smartScale?.name ?: run {
            Logger.withTag(TAG).w { "Disconnect called but no scale is connected" }
            return
        }
        Logger.withTag(TAG).i { "Disconnecting smart scale: $name" }
        val active = requireSession()
        write(active, active.ctrlChar, buildScaleFrame(SCALE_CMD_DISCONNECT, name))
    }

    private suspend fun write(
        active: Session,
        characteristic: BluetoothCharacteristic,
        data: ByteArray,
    ) {
        val channel = if (characteristic === active.ctrlChar) "CTRL" else "DATA"
        Logger.withTag(BLE_TRACE_TAG).v { ">> [$channel] ${toHexString(data)} (${data.size}B)" }
        blueFalcon.writeCharacteristic(active.peripheral, characteristic, data, WRITE_TYPE_NO_RESPONSE)
    }

    private fun requireSession(): Session =
        session ?: error("WendougeeDataSController: no active BLE session (not connected)")

    private fun BluetoothPeripheral.findBySuffix(suffix: String): BluetoothCharacteristic? =
        characteristics.firstOrNull { it.uuid.toString().lowercase().contains(suffix) }

    private suspend fun currentCoroutineIsActive(): Boolean {
        val job = currentCoroutineContext()[Job]
        return job?.isActive ?: true
    }
}

private const val TAG = "WendougeeController"
private const val BLE_TRACE_TAG = "WendougeeBle"

private const val DATA_UUID_SUFFIX = "2b10"
private const val CTRL_UUID_SUFFIX = "2c10"

private const val POLLING_INTERVAL_MS = 200L
private const val BREW_PULSE_MS = 100L
private const val HEARTBEAT_TIMEOUT_MS = 10_000L

private const val MTU_SIZE = 512
private const val MTU_TIMEOUT_MS = 4_000L
private const val POST_MTU_DELAY_MS = 500L
private const val CHARS_READY_TIMEOUT_MS = 10_000L
private const val CONNECT_TIMEOUT_MS = 10_000L
private const val CONNECT_RETRY_DELAY_MS = 100L
private const val POST_CONNECT_SETTLE_MS = 500L
private const val CHARS_RETRY_DELAY_MS = 200L
private const val STALE_GATT_SETTLE_MS = 300L
private const val CCCD_SETTLE_DELAY_MS = 200L
private const val INIT_CONFIG_DELAY_MS = 300L
private const val INIT_SCALE_DELAY_MS = 400L
private const val SCALE_STATUS_DELAY_MS = 100L
private const val SCALE_LIST_DELAY_MS = 200L
private const val POST_INIT_DELAY_MS = 300L
private const val NOTIFY_RESET_DELAY_MS = 100L
private const val POLL_BETWEEN_DELAY_MS = 30L
private const val POLL_TIMEOUT_MS = 800L
private const val INIT_POLL_TIMEOUT_MS = 3000L
private const val SCAN_TIMEOUT_MS = 12_000L
private const val WATCHDOG_INTERVAL_MS = 1_000L
private const val FC_COIL_WRITE: Byte = 0x05
private const val MODBUS_UNIT_ID: Byte = 0x01
private const val SENSOR_SCALE_FACTOR = 10
private const val BREW_MAX_TEMP = 110
private const val STEAM_MAX_TEMP = 140
private const val PADDLE_PRESSURE_MAX_INT = 120
private const val PADDLE_TIME_MAX = 60
private const val FREE_BREW_PRESSURE_MAX = 12f
private const val FREE_BREW_FLOW_MAX = 8f
private const val CLEANING_TIME_MAX = 60
private const val CLEANING_REST_MAX = 60
private const val CLEANING_COUNT_MAX = 10
private const val SCALE_SEARCH_AFTER_ENABLE_DELAY_MS = 200L
private const val COIL_MANUAL_BREW = 0x9A
private const val COIL_FREE_VAR_BREW = 0x9D
private const val FREE_VAR_PREPARE_VALUE = 4
private const val COIL_SHORT_PRESS = 0x96
private const val COIL_CLEANING = 0x9B
private const val COIL_PROFILE_FREE = 0x9E
private const val BYTE_SHIFT = 8
private const val BYTE_MASK = 0xFF
private const val HEX_RADIX = 16
private const val SCALE_CMD_CONNECT = 0x80
private const val SCALE_CMD_DISCONNECT = 0x87

private const val WRITE_TYPE_NO_RESPONSE = 1

private val POLL_LONG_PREFIX = byteArrayOf(0x01, 0x03, 0x28)
private val POLL_SHORT_PREFIX = byteArrayOf(0x01, 0x01)

private fun toHexString(arr: ByteArray): String = arr.joinToString("") {
    (it.toInt() and BYTE_MASK).toString(HEX_RADIX).padStart(2, '0')
}

private val wendougeeConstraints: DeviceConstraints = DeviceConstraints(
    brewTempRange = 0..BREW_MAX_TEMP,
    steamTempRange = 0..STEAM_MAX_TEMP,
    manualBrewPressureRange = 1..PADDLE_PRESSURE_MAX_INT,
    manualBrewTimeRange = 0..PADDLE_TIME_MAX,
    cleaningTimeRange = 1..CLEANING_TIME_MAX,
    cleaningRestRange = 1..CLEANING_REST_MAX,
    cleaningCountRange = 1..CLEANING_COUNT_MAX,
    pressureRange = 0f..FREE_BREW_PRESSURE_MAX,
    flowRange = 0f..FREE_BREW_FLOW_MAX,
)

private val wendougeeCapabilities: Set<DeviceCapability> = setOf(
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
    DeviceCapability.FlowProfiling,
)
