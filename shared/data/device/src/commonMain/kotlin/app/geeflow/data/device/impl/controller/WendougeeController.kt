@file:Suppress("TooGenericExceptionCaught", "TooManyFunctions", "LargeClass")

package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.DeviceAccessoryControl
import app.geeflow.data.device.DeviceBrewingControl
import app.geeflow.data.device.DeviceConnectionControl
import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceMaintenanceControl
import app.geeflow.data.device.DeviceProfileControl
import app.geeflow.data.device.DeviceSettingsControl
import app.geeflow.data.device.TimedPumpProfileSupport
import app.geeflow.data.device.ble.modbus.ModbusSession
import app.geeflow.data.device.impl.controller.WendougeeCommands.CMD_SCALE_LIST_REQUEST
import app.geeflow.data.device.impl.controller.WendougeeCommands.CMD_SCALE_SEARCH_QUERY
import app.geeflow.data.device.impl.controller.WendougeeCommands.CMD_SCALE_STATUS_REQUEST
import app.geeflow.data.device.impl.controller.WendougeeCommands.CMD_SET_LANGUAGE
import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.DeviceState.AccessorySyncStatus
import app.geeflow.data.device.model.DeviceState.ConnectionStatus
import app.geeflow.data.device.model.SingleDoseGrinder
import app.geeflow.data.device.model.SmartScale
import app.geeflow.data.device.model.SupportedDevice
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

internal class WendougeeController(
    val model: SupportedDevice,
    private val scope: CoroutineScope,
    private val transport: WendougeeBleTransport,
) : DeviceController, DeviceConnectionControl {
    override var connectionGeneration: Long = 0
        private set
    override val connection: DeviceConnectionControl get() = this
    override val settings: DeviceSettingsControl = WendougeeSettingsControl(this)
    override val brewing: DeviceBrewingControl = WendougeeBrewingControl(this)
    override val maintenance: DeviceMaintenanceControl = WendougeeMaintenanceControl(this)
    override val profiles: DeviceProfileControl = WendougeeProfileControl(this)
    override val accessories: DeviceAccessoryControl = WendougeeAccessoryControl(this)

    override val profilingCapabilities = TimedPumpProfileSupport.capabilities

    override val constraints = wendougeeConstraints

    override val capabilities = wendougeeCapabilities

    private val stateStore = WendougeeStateStore()
    override val deviceState = stateStore.machine
    override val foundScales = stateStore.scales
    override val foundSingleDoseGrinders = stateStore.grinders
    private val _deviceState get() = stateStore.machine
    private val integrationFlags get() = stateStore.flags
    internal val mutableState get() = stateStore.machine
    internal val accessoryFlags get() = stateStore.flags
    internal val accessorySettingsMutex = Mutex()
    internal val operationMutex = Mutex()
    internal val accessoryRefreshCoordinator = AccessoryRefreshCoordinator()
    internal fun updateState(update: (DeviceState) -> DeviceState) = stateStore.updateMachine(update)
    internal fun updateScales(scales: List<SmartScale>) = stateStore.setScales(scales)
    internal fun updateGrinders(grinders: List<SingleDoseGrinder>) = stateStore.setGrinders(grinders)
    internal fun updateFlags(flags: Int?) = stateStore.setFlags(flags)
    private val lifecycleMutex = Mutex()
    private val accessoryStatusMutex = Mutex()
    private var accessorySyncJob: Job? = null

    private var connectJob: Job? = null
    private var watchdogJob: Job? = null
    private var notificationJob: Job? = null
    private var heartbeatTimeoutJob: Job? = null

    private var session: WendougeeSession? = null

    private fun onScanStatusReceived(active: Boolean) {
        heartbeatTimeoutJob?.cancel()
        if (!active) return
        heartbeatTimeoutJob = scope.launch {
            val currentSession = session
            if (currentSession != null) {
                runCatching { writeControl(currentSession, CMD_SCALE_STATUS_REQUEST) }
                    .onFailure { Logger.withTag(TAG).w(it) { "Scale status request failed" } }
            }
            delay(HEARTBEAT_TIMEOUT_MS)
            stateStore.updateMachine { it.copy(smartScaleSearchActive = false, singleDoseGrinderSearchActive = false) }
        }
    }

    @Suppress("LongMethod")
    override suspend fun connect(device: Device): DeviceConnection = lifecycleMutex.withLock {
        val ble = device.connection as? DeviceConnection.Ble
            ?: error("WendougeeController requires DeviceConnection.Ble")

        disconnectSession()
        val ready = CompletableDeferred<DeviceConnection>()
        connectJob = scope.launch {
            var connectingLink: WendougeeBleConnection? = null
            try {
                stateStore.updateMachine { it.copy(connectionStatus = ConnectionStatus.Connecting) }

                val link = transport.open(ble)
                connectingLink = link
                val responses = MutableSharedFlow<ByteArray>()
                val modbus = ModbusSession(
                    notifications = responses,
                    write = {
                        check(session?.responses === responses) { "Expired BLE session" }
                        transport.writeData(link, it)
                    },
                    unitId = MODBUS_UNIT_ID,
                    defaultTimeout = 1000.milliseconds,
                    completeFrames = true,
                    sessionJob = currentCoroutineContext()[Job],
                )
                val active = WendougeeSession(link, modbus, responses)
                session = active

                startObservingNotifications(active)
                startWatchdog(link)

                stateStore.updateMachine { it.copy(connectionStatus = ConnectionStatus.Synchronizing) }

                sendInitCommands(active)

                stateStore.updateMachine { it.copy(connectionStatus = ConnectionStatus.Connected) }
                Logger.withTag(TAG).i { "Connected and ready" }

                ready.complete(ble.copy(peripheralId = link.peripheralId))
                accessorySyncJob = scope.launch { synchronizeAccessories(active) }
                runPollingLoop(active)
            } catch (failure: Throwable) {
                ready.completeExceptionally(failure)
                if (failure is CancellationException) throw failure
                Logger.withTag(TAG).e(failure) { "Connection failed" }
            } finally {
                withContext(NonCancellable) {
                    connectingLink?.let { runCatching { transport.close(it) } }
                    accessorySyncJob?.cancelAndJoin()
                    resetToDisconnected()
                }
                if (!ready.isCompleted) ready.cancel()
            }
        }
        try {
            ready.await()
        } catch (failure: Throwable) {
            withContext(NonCancellable) { disconnectSession() }
            throw failure
        }
    }

    private fun startObservingNotifications(active: WendougeeSession) {
        notificationJob?.cancel()
        notificationJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            transport.notifications(active.link).collect { notification ->
                val bytes = notification.bytes
                if (session !== active) return@collect
                if (!notification.control) {
                    active.frames.receive(bytes).forEach { frame ->
                        stateStore.receive(frame, control = false)
                        active.responses.emit(frame)
                    }
                } else {
                    active.controlFrames.receive(bytes).forEach { frame ->
                        stateStore.receive(frame, control = true)?.let(::onScanStatusReceived)
                        active.controlResponses.emit(frame)
                    }
                }
            }
        }
    }

    private fun startWatchdog(link: WendougeeBleConnection) {
        watchdogJob?.cancel()
        watchdogJob = scope.launch {
            while (isActive) {
                delay(WATCHDOG_INTERVAL_MS)
                if (transport.isDisconnected(link)) {
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

    private suspend fun sendInitCommands(active: WendougeeSession) {
        delay(CCCD_SETTLE_DELAY_MS)
        active.modbus.readHoldingRegisters(0, CONFIG_REGISTER_COUNT, INIT_POLL_TIMEOUT_MS.milliseconds)
        withTimeout(INIT_POLL_TIMEOUT_MS) { deviceState.first { it.config != null } }
        readWaterAlarmRegister(active)
        val firmware = readWendougeeFirmware(active.modbus)
        stateStore.updateMachine {
            it.copy(firmwareVersion = firmware, agileRingSupported = supportsAgileRing(model, firmware))
        }
        writeControl(active, CMD_SET_LANGUAGE)
        delay(POST_INIT_DELAY_MS)

        pollLongOnce(active, timeout = INIT_POLL_TIMEOUT_MS)
        pollShortOnce(active, timeout = INIT_POLL_TIMEOUT_MS)
    }

    private suspend fun readWaterAlarmRegister(active: WendougeeSession) {
        val enabled = active.modbus.readHoldingRegisters(WendougeeRegisters.WATER_ALARM, 1).first() != 0
        Logger.withTag(TAG).i { "Water alarm enabled=$enabled" }
        stateStore.updateMachine { state ->
            val config = state.config ?: return@updateMachine state
            state.copy(config = config.copy(waterAlarmEnabled = enabled))
        }
    }

    private suspend fun runPollingLoop(active: WendougeeSession) {
        while (currentCoroutineIsActive()) {
            runCatching { pollLongOnce(active) }
                .onFailure { Logger.withTag(TAG).w(it) { "Poll long missed" } }
            delay(POLL_BETWEEN_DELAY_MS)
            runCatching { pollShortOnce(active) }
                .onFailure { Logger.withTag(TAG).w(it) { "Poll short missed" } }
            val interval = if (_deviceState.value.brewStatus == DeviceState.BrewStatus.FreeVariable) {
                LIVE_POLLING_INTERVAL_MS
            } else {
                POLLING_INTERVAL_MS
            }
            delay(interval)
        }
    }

    override suspend fun disconnect() = lifecycleMutex.withLock { disconnectSession() }

    private suspend fun disconnectSession() {
        connectJob?.cancelAndJoin()
        connectJob = null
        accessorySyncJob?.cancelAndJoin()
        notificationJob?.cancelAndJoin()
        watchdogJob?.cancelAndJoin()
        heartbeatTimeoutJob?.cancelAndJoin()
        resetToDisconnected()
    }

    private fun resetToDisconnected() {
        connectionGeneration++
        stateStore.reset()
        heartbeatTimeoutJob?.cancel()
        watchdogJob?.cancel()
        notificationJob?.cancel()
        session = null
    }

    private suspend fun pollLongOnce(active: WendougeeSession, timeout: Long = POLL_TIMEOUT_MS) {
        active.modbus.readHoldingRegisters(
            address = TELEMETRY_REGISTER_BASE,
            count = TELEMETRY_REGISTER_COUNT,
            timeout = timeout.milliseconds,
        )
    }

    private suspend fun pollShortOnce(active: WendougeeSession, timeout: Long = POLL_TIMEOUT_MS) {
        active.modbus.readCoils(
            address = STATUS_COIL_BASE,
            count = STATUS_COIL_COUNT,
            timeout = timeout.milliseconds,
        )
    }

    internal suspend fun refreshBrewStatus(active: WendougeeSession) {
        pollShortOnce(active)
    }

    internal suspend fun sendModbusPulse(address: Int, label: String) {
        requireSession().modbus.pulseCoil(address)
        Logger.withTag(TAG).d { "$label pulse completed and confirmed" }
    }

    internal suspend fun readIntegrationFlags(active: WendougeeSession) {
        if (integrationFlags.value != null) return
        writeControl(active, CMD_SCALE_SEARCH_QUERY)
        withTimeout(SCALE_SETTINGS_TIMEOUT_MS) { integrationFlags.mapNotNull { it }.first() }
    }

    internal suspend fun requestAccessoryStatus(active: WendougeeSession) = accessoryStatusMutex.withLock {
        requestControlReply(active, CMD_SCALE_STATUS_REQUEST)
        delay(SCALE_LIST_DELAY_MS)
        requestControlReply(active, CMD_SCALE_LIST_REQUEST)
    }

    private suspend fun requestControlReply(active: WendougeeSession, command: ByteArray) = coroutineScope {
        val response = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(SCALE_SETTINGS_TIMEOUT_MS) {
                active.controlResponses.first { it[CONTROL_COMMAND_OFFSET] == command[CONTROL_COMMAND_OFFSET] }
            }
        }
        writeControl(active, command)
        response.await()
        Unit
    }

    private suspend fun synchronizeAccessories(active: WendougeeSession) {
        updateState { it.copy(accessorySyncStatus = AccessorySyncStatus.Syncing) }
        for (backoff in ACCESSORY_RETRY_DELAYS) {
            delay(backoff)
            try {
                readIntegrationFlags(active)
                if (_deviceState.value.smartScaleEnabled || _deviceState.value.singleDoseGrinderEnabled) {
                    requestAccessoryStatus(active)
                }
                updateState { it.copy(accessorySyncStatus = AccessorySyncStatus.Ready) }
                return
            } catch (timeout: TimeoutCancellationException) {
                Logger.withTag(TAG).w(timeout) { "Accessory synchronization timed out" }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Logger.withTag(TAG).w(failure) { "Accessory synchronization failed" }
            }
        }
        if (session === active) updateState { it.copy(accessorySyncStatus = AccessorySyncStatus.Failed) }
    }

    internal suspend fun writeControl(active: WendougeeSession, data: ByteArray) {
        check(session === active) { "Expired BLE session" }
        Logger.withTag(BLE_TRACE_TAG).v { ">> [CTRL] ${toHexString(data)} (${data.size}B)" }
        transport.writeControl(active.link, data)
    }

    internal fun requireSession(): WendougeeSession =
        session ?: error("WendougeeController: no active BLE session (not connected)")

    private suspend fun currentCoroutineIsActive(): Boolean {
        val job = currentCoroutineContext()[Job]
        return job?.isActive ?: true
    }
}

private val ACCESSORY_RETRY_DELAYS = listOf(0L, 1000L, 3000L)

private const val CONTROL_COMMAND_OFFSET = 4
