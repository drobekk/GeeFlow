@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.ble.BleScanCoordinator
import app.geeflow.data.device.ble.modbus.ModbusSession
import app.geeflow.data.device.ble.modbus.ModbusSessionDiagnosticSink
import app.geeflow.data.device.impl.discovery.BleAdvertisement
import app.geeflow.data.device.impl.discovery.WendougeeBleDeviceDiscoverer
import app.geeflow.data.device.model.DeviceConnection
import dev.bluefalcon.core.BlueFalcon
import dev.bluefalcon.core.BluetoothPeripheral
import dev.bluefalcon.core.BluetoothPeripheralState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Read-only diagnostic entry point for the existing GeeFlow BLE path.
 * It intentionally avoids [WendougeeController.connect], whose startup sequence sends
 * additional FF55 and Modbus requests that are outside a connection-only comparison.
 */
class GeeFlowBleDiagnosticProbe(
    private val blueFalcon: BlueFalcon,
    private val discoverer: WendougeeBleDeviceDiscoverer,
    private val scanCoordinator: BleScanCoordinator,
    private val sink: ConnectionDiagnosticSink,
) {
    private val transport = WendougeeBleTransport(blueFalcon, discoverer, scanCoordinator)
    private var lastSuccessfulPeripheralId: String? = null

    suspend fun run(
        serialNumber: String,
        fallbackMac: String,
        attemptBudgetMs: Long = TARGET_ATTEMPT_BUDGET_MS,
    ): Boolean {
        lastSuccessfulPeripheralId = null
        val methodClock = TimeSource.Monotonic.markNow()
        var readSucceeded = false
        sink.event(
            "method_started",
            mapOf("method" to "GeeFlow BlueFalcon", "targetAttemptBudgetMs" to attemptBudgetMs.toString(), "maxAttempts" to "2"),
        )
        try {
            readSucceeded = runTargetAttempt("typed_serial", attemptBudgetMs) { attemptClock, onValidRead ->
                val serialPeripheral = scanForSerial(serialNumber, remaining(attemptClock, attemptBudgetMs), serialNumber)
                if (serialPeripheral == null) {
                    sink.event("target_match_not_found", mapOf("source" to "typed_serial", "serial" to serialNumber))
                    false
                } else {
                    sink.event("target_selected", targetFields("typed_serial", serialNumber, serialPeripheral))
                    tryCandidate(serialPeripheral, macFor(serialPeripheral, fallbackMac), attemptClock, attemptBudgetMs, onValidRead)
                }
            }
            if (!readSucceeded) {
                readSucceeded = runTargetAttempt("hci_fallback_mac", attemptBudgetMs) { attemptClock, onValidRead ->
                    val fallbackPeripheral = scanForMac(fallbackMac, remaining(attemptClock, attemptBudgetMs))
                    if (fallbackPeripheral == null) {
                        sink.event("target_match_not_found", mapOf("source" to "hci_fallback_mac", "mac" to fallbackMac))
                        false
                    } else {
                        sink.event("target_selected", targetFields("hci_fallback_mac", fallbackMac, fallbackPeripheral))
                        tryCandidate(fallbackPeripheral, fallbackMac, attemptClock, attemptBudgetMs, onValidRead)
                    }
                }
            }
        } finally {
            sink.event(
                "method_finished",
                mapOf("method" to "GeeFlow BlueFalcon", "validRead" to readSucceeded.toString(), "elapsedMs" to methodClock.elapsedNow().inWholeMilliseconds.toString()),
            )
        }
        return readSucceeded
    }

    /** Runs GeeFlow's read-only initialization reads after the three comparable probes. */
    suspend fun runReadOnlyInitializationStage(
        fallbackMac: String,
        budgetMs: Long = INITIALIZATION_STAGE_BUDGET_MS,
    ): Boolean {
        val stageClock = TimeSource.Monotonic.markNow()
        var link: WendougeeBleConnection? = null
        var everyReadSucceeded = true
        val targetPeripheralId = lastSuccessfulPeripheralId ?: fallbackMac
        val targetSource = if (lastSuccessfulPeripheralId == null) "hci_fallback_mac" else "last_successful_probe_target"
        sink.event(
            "geeflow_initialization_stage_started",
            mapOf("budgetMs" to budgetMs.toString(), "target" to targetPeripheralId, "targetSource" to targetSource, "fallbackMac" to fallbackMac),
        )
        sink.event(
            "ff55_init_command_review",
            mapOf(
                "geeflowFrame" to "ff55ffff070001015c",
                "geeflowSourceName" to "CMD_SET_LANGUAGE",
                "officialHciFrame" to "ff55ffff070001005b",
                "reviewSource" to "protocol-audit-v04-phone-hci/captures/official_wendougee_events_v02.md:48",
                "meaning" to "unresolved; GeeFlow and official app send different payloads",
                "sent" to "false",
                "decision" to "omit state-changing FF55 write from physical diagnostic",
            ),
        )
        try {
            withTimeoutOrNull(budgetMs) {
                link = transport.open(
                    DeviceConnection.Ble(peripheralId = targetPeripheralId, macAddress = fallbackMac),
                    diagnosticSink = sink,
                )
                val openedLink = requireNotNull(link)
                sink.event("geeflow_initialization_gatt_ready", mapOf("address" to openedLink.peripheralId))
                coroutineScope {
                    val responses = MutableSharedFlow<ByteArray>(extraBufferCapacity = NOTIFICATION_BUFFER_SIZE)
                    val stateObserver = launch {
                        var previousState = blueFalcon.connectionState(openedLink.peripheral)
                        sink.event("geeflow_initialization_connection_state", mapOf("address" to openedLink.peripheralId, "state" to previousState.toString()))
                        while (true) {
                            delay(CONNECTION_STATE_POLL_MS)
                            val currentState = blueFalcon.connectionState(openedLink.peripheral)
                            if (currentState != previousState) {
                                sink.event("geeflow_initialization_connection_state", mapOf("address" to openedLink.peripheralId, "state" to currentState.toString()))
                                previousState = currentState
                                if (currentState == BluetoothPeripheralState.Disconnected) break
                            }
                        }
                    }
                    val collector = launch {
                        transport.notifications(openedLink).collect { notification ->
                            if (notification.control) {
                                sink.event("initialization_notification_control", notificationFields(notification.bytes, control = true))
                            } else {
                                sink.event("initialization_notification_modbus", notificationFields(notification.bytes, control = false))
                                responses.emit(notification.bytes)
                            }
                        }
                    }
                    try {
                        val modbus = diagnosticModbusSession(openedLink, responses)
                        everyReadSucceeded = runInitializationRead("config_0_37", stageClock, budgetMs) {
                            modbus.readHoldingRegisters(0, CONFIG_REGISTER_COUNT, READ_TIMEOUT_SECONDS.seconds).joinToString(",")
                        } && everyReadSucceeded
                        everyReadSucceeded = runInitializationRead("water_alarm_396_1", stageClock, budgetMs) {
                            modbus.readHoldingRegisters(WendougeeRegisters.WATER_ALARM, 1, READ_TIMEOUT_SECONDS.seconds).joinToString(",")
                        } && everyReadSucceeded
                        everyReadSucceeded = runInitializationRead("firmware_1441_1", stageClock, budgetMs) {
                            modbus.readHoldingRegisters(WendougeeRegisters.FIRMWARE_VERSION, 1, READ_TIMEOUT_SECONDS.seconds).joinToString(",")
                        } && everyReadSucceeded
                        everyReadSucceeded = runInitializationRead("telemetry_1404_20", stageClock, budgetMs) {
                            modbus.readHoldingRegisters(TELEMETRY_REGISTER_BASE, TELEMETRY_REGISTER_COUNT, READ_TIMEOUT_SECONDS.seconds).joinToString(",")
                        } && everyReadSucceeded
                        everyReadSucceeded = runInitializationRead("status_coils_182_24", stageClock, budgetMs) {
                            modbus.readCoils(STATUS_COIL_BASE, STATUS_COIL_COUNT, READ_TIMEOUT_SECONDS.seconds).joinToString(",")
                        } && everyReadSucceeded
                    } finally {
                        collector.cancelAndJoin()
                        stateObserver.cancelAndJoin()
                    }
                }
            } ?: run { everyReadSucceeded = false }
        } catch (failure: Throwable) {
            if (failure is CancellationException) throw failure
            everyReadSucceeded = false
            sink.event("geeflow_initialization_stage_failed", mapOf("error" to (failure.message ?: failure::class.simpleName.orEmpty())))
        } finally {
            withContext(NonCancellable) {
                link?.let { opened ->
                    sink.event("geeflow_initialization_disconnect_requested", mapOf("address" to opened.peripheralId))
                    runCatching { transport.close(opened) }
                        .onSuccess { sink.event("geeflow_initialization_disconnect_returned", mapOf("address" to opened.peripheralId)) }
                        .onFailure { sink.event("geeflow_initialization_disconnect_failed", mapOf("error" to (it.message ?: it::class.simpleName.orEmpty()))) }
                }
            }
            sink.event(
                "geeflow_initialization_stage_finished",
                mapOf("allReadOnlyReadsSucceeded" to everyReadSucceeded.toString(), "elapsedMs" to stageClock.elapsedNow().inWholeMilliseconds.toString()),
            )
        }
        return everyReadSucceeded
    }

    private suspend fun runTargetAttempt(
        source: String,
        budgetMs: Long,
        block: suspend (TimeMark, () -> Unit) -> Boolean,
    ): Boolean {
        val attemptClock = TimeSource.Monotonic.markNow()
        sink.event("target_attempt_started", mapOf("source" to source, "budgetMs" to budgetMs.toString()))
        var validReadDuringAttempt = false
        var deadlineReached = false
        val attemptResult = withTimeoutOrNull(budgetMs) {
            block(attemptClock) { validReadDuringAttempt = true }
        }.also { deadlineReached = it == null }
        val result = attemptResult ?: validReadDuringAttempt
        sink.event(
            "target_attempt_finished",
            mapOf("source" to source, "validRead" to result.toString(), "deadlineReached" to deadlineReached.toString(), "elapsedMs" to attemptClock.elapsedNow().inWholeMilliseconds.toString()),
        )
        return result
    }

    private suspend fun scanForSerial(serialNumber: String, timeoutMs: Long, matchSerial: String): BluetoothPeripheral? {
        if (serialNumber.isBlank() || timeoutMs <= 0) return null
        val scanClock = TimeSource.Monotonic.markNow()
        sink.event("scan_started", mapOf("strategy" to "GeeFlow serial match", "timeoutMs" to timeoutMs.toString()))
        val observed = mutableSetOf<String>()
        val result = scanCoordinator.scanning {
            withTimeoutOrNull(timeoutMs) {
                blueFalcon.peripherals.mapNotNull { peripherals ->
                    peripherals.forEach { peripheral -> logScanObservation(peripheral, observed, "typed_serial", matchSerial) }
                    peripherals.firstOrNull { peripheral ->
                        advertisesSerial(peripheral, serialNumber)
                    }
                }.firstOrNull()
            }
        }
        sink.event("scan_finished", mapOf("strategy" to "GeeFlow serial match", "source" to "typed_serial", "found" to (result != null).toString(), "address" to result?.uuid.orEmpty(), "name" to result?.name.orEmpty(), "observedCount" to observed.size.toString(), "elapsedMs" to scanClock.elapsedNow().inWholeMilliseconds.toString()))
        return result
    }

    private suspend fun scanForMac(mac: String, timeoutMs: Long): BluetoothPeripheral? {
        if (timeoutMs <= 0) return null
        val scanClock = TimeSource.Monotonic.markNow()
        sink.event("scan_started", mapOf("strategy" to "GeeFlow HCI MAC match", "mac" to mac, "timeoutMs" to timeoutMs.toString()))
        val observed = mutableSetOf<String>()
        val result = scanCoordinator.scanning {
            withTimeoutOrNull(timeoutMs) {
                blueFalcon.peripherals.mapNotNull { peripherals ->
                    peripherals.forEach { peripheral -> logScanObservation(peripheral, observed, "hci_fallback_mac", null) }
                    peripherals.firstOrNull { peripheral ->
                        val adv = advertisement(peripheral)
                        discoverer.matchesAddress(adv, mac) ||
                            discoverer.tryRecogniseSerial(adv, FALLBACK_SERIAL) != null
                    }
                }.firstOrNull()
            }
        }
        sink.event("scan_finished", mapOf("strategy" to "GeeFlow HCI MAC match", "source" to "hci_fallback_mac", "mac" to mac, "found" to (result != null).toString(), "address" to result?.uuid.orEmpty(), "name" to result?.name.orEmpty(), "observedCount" to observed.size.toString(), "elapsedMs" to scanClock.elapsedNow().inWholeMilliseconds.toString()))
        return result
    }

    private fun logScanObservation(
        peripheral: BluetoothPeripheral,
        observed: MutableSet<String>,
        source: String,
        typedSerial: String?,
    ) {
        val manufacturerData = peripheral.manufacturerData.orEmpty()
        val manufacturerHex = manufacturerData.toSortedMap().entries.joinToString(";") { (id, value) -> "$id:${value.toHex()}" }
        val observationKey = "${peripheral.uuid}|${peripheral.name.orEmpty()}|$manufacturerHex"
        if (!observed.add(observationKey)) return
        val nameSerialMatch = typedSerial?.let { serialInText(peripheral.name, it) } ?: false
        val dataSerialMatch = typedSerial?.let { serialInData(manufacturerData.values.toList(), it) } ?: false
        sink.event(
            "scan_observation",
            mapOf(
                "source" to source,
                "address" to peripheral.uuid,
                "name" to peripheral.name.orEmpty(),
                "rssi" to peripheral.rssi?.toString().orEmpty(),
                "connectable" to peripheral.isConnectable?.toString().orEmpty(),
                "advertisedServices" to peripheral.advertisedServiceUUIDs.joinToString(",") { it.toString() },
                "manufacturerDataHex" to manufacturerHex,
                "characteristics" to peripheral.characteristics.joinToString(",") { it.uuid.toString() },
                "typedSerialMatchName" to nameSerialMatch.toString(),
                "typedSerialMatchManufacturerData" to dataSerialMatch.toString(),
            ),
        )
    }

    private fun advertisesSerial(peripheral: BluetoothPeripheral, serialNumber: String): Boolean =
        serialInText(peripheral.name, serialNumber) || serialInData(peripheral.manufacturerData.orEmpty().values.toList(), serialNumber)

    private fun serialInText(name: String?, serialNumber: String): Boolean {
        val serial = serialNumber.trim().uppercase()
        if (serial.isEmpty()) return false
        val candidate = name?.uppercase().orEmpty()
        return Regex("(^|[^A-Z0-9])${Regex.escape(serial)}($|[^A-Z0-9])").containsMatchIn(candidate)
    }

    private fun serialInData(advertisedValues: List<ByteArray>, serialNumber: String): Boolean {
        val needle = serialNumber.trim().uppercase().encodeToByteArray()
        if (needle.isEmpty()) return false
        return advertisedValues.any { value ->
            value.indices.any { start ->
                start + needle.size <= value.size && needle.indices.all { offset ->
                    (value[start + offset].toInt() and 0xFF).toChar().uppercaseChar() == needle[offset].toInt().toChar().uppercaseChar()
                }
            }
        }
    }

    private suspend fun tryCandidate(
        peripheral: BluetoothPeripheral,
        mac: String,
        clock: TimeMark,
        budgetMs: Long,
        onValidRead: () -> Unit,
    ): Boolean {
        var link: WendougeeBleConnection? = null
        return try {
            val openedLink = transport.open(
                DeviceConnection.Ble(peripheralId = peripheral.uuid, macAddress = mac),
                diagnosticSink = sink,
            )
            link = openedLink
            sink.event(
                "gatt_connected",
                mapOf(
                    "address" to peripheral.uuid,
                    "name" to peripheral.name.orEmpty(),
                    "services" to peripheral.services.joinToString(",") { it.uuid.toString() },
                    "characteristics" to peripheral.characteristics.joinToString(",") { it.uuid.toString() },
                ),
            )
            coroutineScope {
                val responses = MutableSharedFlow<ByteArray>(extraBufferCapacity = NOTIFICATION_BUFFER_SIZE)
                val stateObserver = launch {
                    var previousState = blueFalcon.connectionState(openedLink.peripheral)
                    sink.event("gatt_connection_state_observed", mapOf("address" to openedLink.peripheralId, "state" to previousState.toString()))
                    while (true) {
                        delay(CONNECTION_STATE_POLL_MS)
                        val currentState = blueFalcon.connectionState(openedLink.peripheral)
                        if (currentState != previousState) {
                            sink.event("gatt_connection_state_observed", mapOf("address" to openedLink.peripheralId, "state" to currentState.toString()))
                            previousState = currentState
                            if (currentState == BluetoothPeripheralState.Disconnected) break
                        }
                    }
                }
                val collector = launch {
                    transport.notifications(openedLink).collect { notification ->
                        sink.event(
                            if (notification.control) "notification_control" else "notification_modbus",
                            mapOf(
                                "hex" to toHexString(notification.bytes),
                                "length" to notification.bytes.size.toString(),
                                "decodeStatus" to if (notification.control) "raw_only_ff55_not_decoded" else "awaiting_modbus_frame_validation",
                            ),
                        )
                        if (!notification.control) responses.emit(notification.bytes)
                    }
                }
                try {
                    val modbus = ModbusSession(
                        notifications = responses,
                        write = { bytes -> writeDiagnosticData(openedLink, bytes) },
                        unitId = MODBUS_UNIT_ID,
                        defaultTimeout = READ_TIMEOUT_SECONDS.seconds,
                        completeFrames = false,
                        diagnosticSink = ModbusSessionDiagnosticSink { name, fields -> sink.event(name, fields) },
                    )
                    val values = modbus.readHoldingRegisters(0, CONFIG_REGISTER_COUNT, READ_TIMEOUT_SECONDS.seconds)
                    sink.event(
                        "modbus_read_valid",
                        mapOf("function" to "03", "baseRegister" to "0", "count" to values.size.toString(), "values" to values.joinToString(",")),
                    )
                    lastSuccessfulPeripheralId = peripheral.uuid
                    onValidRead()
                    delay(remaining(clock, budgetMs))
                    true
                } finally {
                    collector.cancelAndJoin()
                    stateObserver.cancelAndJoin()
                }
            }
        } catch (failure: TimeoutCancellationException) {
            if (remaining(clock, budgetMs) <= 0) throw failure
            sink.event("candidate_read_timeout", mapOf("address" to peripheral.uuid, "timeoutMs" to (READ_TIMEOUT_SECONDS * 1_000).toString()))
            false
        } catch (failure: Throwable) {
            if (failure is CancellationException) throw failure
            sink.event("candidate_failed", mapOf("address" to peripheral.uuid, "error" to (failure.message ?: failure::class.simpleName.orEmpty())))
            false
        } finally {
            withContext(NonCancellable) {
                link?.let { openedLink ->
                    sink.event("gatt_disconnect_requested", mapOf("address" to openedLink.peripheralId, "reason" to "diagnostic_attempt_finished"))
                    runCatching { transport.close(openedLink) }
                        .onSuccess {
                            sink.event("gatt_disconnect_call_returned", mapOf("address" to openedLink.peripheralId, "disconnected" to transport.isDisconnected(openedLink).toString()))
                        }
                        .onFailure { sink.event("gatt_disconnect_call_failed", mapOf("address" to openedLink.peripheralId, "error" to (it.message ?: it::class.simpleName.orEmpty()))) }
                    if (remaining(clock, budgetMs) > 0) delay(DISCONNECT_SETTLE_MS)
                }
            }
        }
    }

    private fun diagnosticModbusSession(
        link: WendougeeBleConnection,
        responses: MutableSharedFlow<ByteArray>,
    ) = ModbusSession(
        notifications = responses,
        write = { bytes -> writeDiagnosticData(link, bytes) },
        unitId = MODBUS_UNIT_ID,
        defaultTimeout = READ_TIMEOUT_SECONDS.seconds,
        completeFrames = false,
        diagnosticSink = ModbusSessionDiagnosticSink { name, fields -> sink.event(name, fields) },
    )

    private suspend fun writeDiagnosticData(link: WendougeeBleConnection, bytes: ByteArray) {
        try {
            transport.writeData(link, bytes)
            sink.event("gatt_characteristic_write_call_returned", mapOf("characteristic" to link.data.uuid.toString(), "length" to bytes.size.toString(), "mode" to "without_response"))
        } catch (failure: Throwable) {
            sink.event("gatt_characteristic_write_call_failed", mapOf("characteristic" to link.data.uuid.toString(), "length" to bytes.size.toString(), "error" to (failure.message ?: failure::class.simpleName.orEmpty())))
            throw failure
        }
    }

    private suspend fun runInitializationRead(
        label: String,
        stageClock: TimeMark,
        budgetMs: Long,
        read: suspend () -> String,
    ): Boolean {
        sink.event("geeflow_initialization_read_started", mapOf("label" to label))
        return try {
            val values = read()
            sink.event("geeflow_initialization_read_succeeded", mapOf("label" to label, "values" to values))
            true
        } catch (failure: TimeoutCancellationException) {
            if (remaining(stageClock, budgetMs) <= 0) throw failure
            sink.event(
                "geeflow_initialization_read_failed",
                mapOf("label" to label, "error" to "response timeout", "timeoutMs" to (READ_TIMEOUT_SECONDS * 1_000).toString()),
            )
            false
        } catch (failure: Throwable) {
            if (failure is CancellationException) throw failure
            sink.event("geeflow_initialization_read_failed", mapOf("label" to label, "error" to (failure.message ?: failure::class.simpleName.orEmpty())))
            false
        }
    }

    private fun notificationFields(bytes: ByteArray, control: Boolean) = mapOf(
        "hex" to toHexString(bytes),
        "length" to bytes.size.toString(),
        "decodeStatus" to if (control) "raw_only_ff55_not_decoded" else "raw_modbus_fragment",
    )

    private fun targetFields(source: String, reference: String, peripheral: BluetoothPeripheral) = mapOf(
        "source" to source,
        "reference" to reference,
        "address" to peripheral.uuid,
        "name" to peripheral.name.orEmpty(),
        "rssi" to peripheral.rssi?.toString().orEmpty(),
        "connectable" to peripheral.isConnectable?.toString().orEmpty(),
        "manufacturerDataHex" to peripheral.manufacturerData.orEmpty().toSortedMap().entries.joinToString(";") { (id, value) -> "$id:${value.toHex()}" },
    )

    private fun advertisement(peripheral: BluetoothPeripheral) =
        BleAdvertisement(peripheralId = peripheral.uuid, name = peripheral.name)

    private fun macFor(peripheral: BluetoothPeripheral, fallbackMac: String): String =
        discoverer.macAddressOf(advertisement(peripheral)) ?: fallbackMac

    private fun remaining(clock: TimeMark, budgetMs: Long): Long =
        (budgetMs - clock.elapsedNow().inWholeMilliseconds).coerceAtLeast(0)

    companion object {
        const val TARGET_ATTEMPT_BUDGET_MS = 30_000L
        const val INITIALIZATION_STAGE_BUDGET_MS = 60_000L
        private const val READ_TIMEOUT_SECONDS = 3L
        private const val DISCONNECT_SETTLE_MS = 500L
        private const val NOTIFICATION_BUFFER_SIZE = 64
        private const val CONNECTION_STATE_POLL_MS = 100L
        private const val FALLBACK_SERIAL = "AB08118958"
    }
}

private fun ByteArray.toHex(): String = toHexString(this)

fun interface ConnectionDiagnosticSink {
    fun event(name: String, fields: Map<String, String>)
}
