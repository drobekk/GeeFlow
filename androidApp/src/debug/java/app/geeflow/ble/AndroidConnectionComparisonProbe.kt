package app.geeflow.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.SystemClock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

internal enum class ComparisonMethod(val title: String, val style: ScanStyle) {
    LITALITE("LitaLite Bleak-style port", ScanStyle.LITALITE),
    CREMA("Crema CoreBluetooth-style port", ScanStyle.CREMA),
}

internal enum class ScanStyle { LITALITE, CREMA }

internal data class DiagnosticCandidate(
    val device: BluetoothDevice,
    val name: String,
    val rssi: Int,
    val advertisedServices: List<String>,
    val rawAdvertisement: ByteArray,
    val manufacturerData: Map<Int, ByteArray>,
    val serviceData: Map<String, ByteArray>,
    val connectable: Boolean?,
)

/** Android-native translations of the referenced apps' scan and connect choices. */
internal class AndroidConnectionComparisonProbe(
    context: Context,
    private val sink: app.geeflow.data.device.impl.controller.ConnectionDiagnosticSink,
) {
    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? get() = bluetoothManager?.adapter

    suspend fun run(method: ComparisonMethod, serialNumber: String): Boolean {
        val startMs = SystemClock.elapsedRealtime()
        var validRead = false
        sink.event("method_started", mapOf("method" to method.title, "targetAttemptBudgetMs" to METHOD_BUDGET_MS.toString(), "maxAttempts" to "2"))
        try {
            validRead = runTargetAttempt(method, serialNumber, MatchSource.TYPED_SERIAL)
            if (!validRead) validRead = runTargetAttempt(method, serialNumber, MatchSource.HCI_FALLBACK)
        } catch (failure: Throwable) {
            if (failure is CancellationException) throw failure
            sink.event("method_error", mapOf("method" to method.title, "error" to failure.message.orEmpty()))
        } finally {
            sink.event(
                "method_finished",
                mapOf(
                    "method" to method.title,
                    "validRead" to validRead.toString(),
                    "elapsedMs" to (SystemClock.elapsedRealtime() - startMs).toString(),
                ),
            )
        }
        return validRead
    }

    private suspend fun runTargetAttempt(method: ComparisonMethod, serialNumber: String, source: MatchSource): Boolean {
        val attemptStartMs = SystemClock.elapsedRealtime()
        var validRead = false
        var deadlineReached = false
        sink.event("target_attempt_started", mapOf("method" to method.title, "source" to source.label, "budgetMs" to METHOD_BUDGET_MS.toString()))
        try {
            withTimeoutOrNull(METHOD_BUDGET_MS) {
                val candidate = scan(method, serialNumber, source, METHOD_BUDGET_MS)
                if (candidate == null) {
                    sink.event(
                        if (source == MatchSource.TYPED_SERIAL) "serial_scan_failed" else "fallback_scan_failed",
                        mapOf("method" to method.title, "source" to source.label, "serial" to serialNumber, "fallbackName" to ConnectionDiagnosticProtocol.FALLBACK_NAME, "fallbackMac" to ConnectionDiagnosticProtocol.FALLBACK_MAC),
                    )
                } else {
                    sink.event("target_selected", candidate.fields(source.label) + mapOf("typedSerial" to serialNumber))
                    try {
                        validRead = connectReadAndObserve(method, candidate, attemptStartMs) { validRead = true }
                    } catch (failure: Throwable) {
                        if (failure is CancellationException) throw failure
                        sink.event("candidate_failed", candidate.fields(source.label) + ("error" to failure.message.orEmpty()))
                    }
                }
            }.also { deadlineReached = it == null }
        } finally {
            sink.event(
                "target_attempt_finished",
                mapOf("method" to method.title, "source" to source.label, "validRead" to validRead.toString(), "deadlineReached" to deadlineReached.toString(), "elapsedMs" to (SystemClock.elapsedRealtime() - attemptStartMs).toString()),
            )
        }
        return validRead
    }

    private suspend fun scan(
        method: ComparisonMethod,
        serialNumber: String,
        source: MatchSource,
        requestedDurationMs: Long,
    ): DiagnosticCandidate? {
        val scanner = adapter?.bluetoothLeScanner ?: error("Bluetooth LE scanner unavailable")
        val phaseBudgetMs = requestedDurationMs.coerceAtMost(METHOD_BUDGET_MS)
        if (phaseBudgetMs <= 0) return null
        sink.event(
            "scan_started",
            mapOf("method" to method.title, "strategy" to method.style.name, "source" to source.label, "timeoutMs" to phaseBudgetMs.toString()),
        )
        sink.event(
            "scan_settings",
            mapOf("method" to method.title, "mode" to "LOW_LATENCY", "callbackType" to "ALL_MATCHES", "reportDelayMs" to "0", "filters" to "none"),
        )

        val found = ConcurrentHashMap<String, DiagnosticCandidate>()
        val observedCandidates = ConcurrentHashMap<String, DiagnosticCandidate>()
        val firstMatchAt = AtomicLong(0L)
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setReportDelay(0L)
            .build()
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val candidate = result.asCandidate()
                val previousObservation = observedCandidates[candidate.device.address]
                if (previousObservation == null || candidate.changedFrom(previousObservation)) {
                    observedCandidates[candidate.device.address] = candidate
                    sink.event(
                        "scan_observation",
                        candidate.fields(source.label) + mapOf(
                            "method" to method.title,
                            "callbackType" to callbackType.toString(),
                            "typedSerialMatchName" to ConnectionDiagnosticProtocol.serialMatches(candidate.name, serialNumber).toString(),
                            "typedSerialMatchData" to ConnectionDiagnosticProtocol.serialMatchesData(candidate.allAdvertisedData(), serialNumber).toString(),
                            "fallbackNameMatch" to candidate.name.equals(ConnectionDiagnosticProtocol.FALLBACK_NAME, ignoreCase = true).toString(),
                            "fallbackMacMatch" to ConnectionDiagnosticProtocol.fallbackMatches(candidate.name, candidate.device.address).toString(),
                        ),
                    )
                }
                if (matches(method, source, candidate, serialNumber)) {
                    firstMatchAt.compareAndSet(0L, SystemClock.elapsedRealtime())
                    val old = found[candidate.device.address]
                    if (old == null || candidate.rssi >= old.rssi) found[candidate.device.address] = candidate
                    if (old == null || kotlin.math.abs(candidate.rssi - old.rssi) >= RSSI_LOG_DELTA) {
                        sink.event("scan_candidate", candidate.fields(source.label) + ("method" to method.title))
                    }
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
            }

            override fun onScanFailed(errorCode: Int) {
                sink.event("scan_failed_callback", mapOf("method" to method.title, "source" to source.label, "status" to errorCode.toString()))
            }
        }

        scanner.startScan(null, scanSettings, callback)
        val scanStartMs = SystemClock.elapsedRealtime()
        try {
            while (SystemClock.elapsedRealtime() - scanStartMs < phaseBudgetMs) {
                val matchedAt = firstMatchAt.get()
                val hasCollectedEnough = matchedAt != 0L &&
                    SystemClock.elapsedRealtime() - matchedAt >= RSSI_COLLECTION_AFTER_MATCH_MS
                if (hasCollectedEnough) break
                delay(SCAN_POLL_INTERVAL_MS)
            }
        } finally {
            scanner.stopScan(callback)
        }
        val chosen = found.values.maxByOrNull { it.rssi }
        sink.event(
            "scan_finished",
            mapOf(
                "method" to method.title,
                "source" to source.label,
                "matchedCount" to found.size.toString(),
                "observedAdvertiserCount" to observedCandidates.size.toString(),
                "selectedAddress" to chosen?.device?.address.orEmpty(),
                "selectedRssi" to chosen?.rssi?.toString().orEmpty(),
                "actualScanMs" to (SystemClock.elapsedRealtime() - scanStartMs).toString(),
            ),
        )
        return chosen
    }

    private fun matches(
        method: ComparisonMethod,
        source: MatchSource,
        candidate: DiagnosticCandidate,
        serialNumber: String,
    ): Boolean {
        val typedSerialMatch = ConnectionDiagnosticProtocol.serialMatches(candidate.name, serialNumber) ||
            ConnectionDiagnosticProtocol.serialMatchesData(candidate.allAdvertisedData(), serialNumber)
        val fallbackMatch = ConnectionDiagnosticProtocol.fallbackMatches(candidate.name, candidate.device.address)
        if (method.style == ScanStyle.CREMA &&
            !candidate.name.startsWith(ADVERTISING_PREFIX, ignoreCase = true) &&
            !typedSerialMatch &&
            !fallbackMatch
        ) return false
        return when (source) {
            MatchSource.TYPED_SERIAL -> typedSerialMatch
            MatchSource.HCI_FALLBACK -> fallbackMatch
        }
    }

    private suspend fun connectReadAndObserve(
        method: ComparisonMethod,
        candidate: DiagnosticCandidate,
        methodStartMs: Long,
        onValidRead: () -> Unit,
    ): Boolean {
        val session = AndroidGattDiagnosticSession(appContext, method, sink)
        var disconnectReason = "candidate_attempt_incomplete"
        return try {
            val connected = withTimeoutOrNull(GATT_CONNECT_TIMEOUT_MS) {
                session.connect(candidate.device)
                true
            } ?: false
            if (!connected) {
                disconnectReason = "gatt_setup_timeout"
                sink.event("gatt_setup_timeout", mapOf("method" to method.title, "timeoutMs" to GATT_CONNECT_TIMEOUT_MS.toString()))
                return false
            }
            val frame = withTimeoutOrNull(READ_TIMEOUT_MS) { session.requestRead() }
            if (frame == null) {
                val pending = session.pendingResponseBytes()
                disconnectReason = "modbus_response_timeout"
                sink.event(
                    "modbus_response_timeout",
                    mapOf("method" to method.title, "timeoutMs" to READ_TIMEOUT_MS.toString(), "pendingHex" to pending.hex(), "pendingLength" to pending.size.toString()),
                )
                return false
            }
            val result = ConnectionDiagnosticProtocol.decodeFrame(frame)
            sink.event(
                if (result.valid) "modbus_read_valid" else "modbus_read_invalid",
                mapOf(
                    "method" to method.title,
                    "rawFrame" to frame.hex(),
                    "reason" to result.reason,
                    "registerCount" to result.registers.size.toString(),
                    "registers" to result.registers.joinToString(","),
                ),
            )
            if (result.valid) {
                onValidRead()
                disconnectReason = "valid_read_window_finished"
                val remainingMs = (METHOD_BUDGET_MS - (SystemClock.elapsedRealtime() - methodStartMs)).coerceAtLeast(0)
                if (remainingMs > 0) delay(remainingMs)
            } else {
                disconnectReason = "modbus_response_invalid"
            }
            result.valid
        } catch (failure: Throwable) {
            disconnectReason = "probe_failed:${failure.message ?: failure::class.simpleName.orEmpty()}"
            throw failure
        } finally {
            withContext(NonCancellable + Dispatchers.IO) { session.close(disconnectReason) }
            delay(DISCONNECT_SETTLE_MS)
        }
    }

    private fun ScanResult.asCandidate(): DiagnosticCandidate {
        val record = scanRecord
        val name = record?.deviceName ?: runCatching { device.name }.getOrNull().orEmpty()
        val manufacturer = record?.manufacturerSpecificData
        val manufacturerData = buildMap {
            if (manufacturer != null) {
                for (index in 0 until manufacturer.size()) put(manufacturer.keyAt(index), manufacturer.valueAt(index))
            }
        }
        val serviceData = record?.serviceData.orEmpty().mapKeys { it.key.uuid.toString() }
        return DiagnosticCandidate(
            device = device,
            name = name,
            rssi = rssi,
            advertisedServices = record?.serviceUuids.orEmpty().map { it.uuid.toString() },
            rawAdvertisement = record?.bytes?.copyOf() ?: byteArrayOf(),
            manufacturerData = manufacturerData,
            serviceData = serviceData,
            connectable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) isConnectable else null,
        )
    }

    private fun DiagnosticCandidate.fields(source: String) = mapOf(
        "source" to source,
        "address" to device.address,
        "name" to name,
        "rssi" to rssi.toString(),
        "connectable" to connectable?.toString().orEmpty(),
        "rawAdvertisementHex" to rawAdvertisement.hex(),
        "advertisedServices" to advertisedServices.joinToString(","),
        "manufacturerDataHex" to manufacturerData.toSortedMap().entries.joinToString(";") { (id, bytes) -> "$id:${bytes.hex()}" },
        "serviceDataHex" to serviceData.toSortedMap().entries.joinToString(";") { (uuid, bytes) -> "$uuid:${bytes.hex()}" },
    )

    private fun DiagnosticCandidate.allAdvertisedData(): List<ByteArray> =
        listOf(rawAdvertisement) + manufacturerData.values + serviceData.values

    private fun DiagnosticCandidate.changedFrom(previous: DiagnosticCandidate): Boolean =
        name != previous.name ||
            kotlin.math.abs(rssi - previous.rssi) >= RSSI_LOG_DELTA ||
            !rawAdvertisement.contentEquals(previous.rawAdvertisement) ||
            manufacturerData.size != previous.manufacturerData.size ||
            serviceData.size != previous.serviceData.size ||
            manufacturerData.entries.any { (key, value) -> previous.manufacturerData[key]?.contentEquals(value) != true } ||
            serviceData.entries.any { (key, value) -> previous.serviceData[key]?.contentEquals(value) != true }

    private enum class MatchSource(val label: String) {
        TYPED_SERIAL("typed_serial"),
        HCI_FALLBACK("hci_fallback_mac"),
    }

    companion object {
        const val METHOD_BUDGET_MS = 30_000L
        private const val GATT_CONNECT_TIMEOUT_MS = 10_000L
        private const val READ_TIMEOUT_MS = 3_000L
        private const val DISCONNECT_SETTLE_MS = 500L
        private const val RSSI_LOG_DELTA = 5
        private const val RSSI_COLLECTION_AFTER_MATCH_MS = 1_500L
        private const val SCAN_POLL_INTERVAL_MS = 100L
        private const val ADVERTISING_PREFIX = "WDG_Data_"
    }
}

private class AndroidGattDiagnosticSession(
    private val context: Context,
    private val method: ComparisonMethod,
    private val sink: app.geeflow.data.device.impl.controller.ConnectionDiagnosticSink,
) {
    private val connected = CompletableDeferred<Unit>()
    private val servicesReady = CompletableDeferred<Unit>()
    private val responseFrame = CompletableDeferred<ByteArray>()
    private val disconnected = CompletableDeferred<Unit>()
    private val mtuChanged = CompletableDeferred<Pair<Int, Int>>()
    private val decoder = ModbusResponseAccumulator { frame ->
        val result = ConnectionDiagnosticProtocol.decodeFrame(frame)
        sink.event(
            "modbus_frame",
            mapOf("method" to method.title, "hex" to frame.hex(), "decodeStatus" to result.reason, "crcValid" to ConnectionDiagnosticProtocol.validCrc(frame).toString()),
        )
        responseFrame.complete(frame)
    }
    private var gatt: BluetoothGatt? = null
    private var dataCharacteristic: BluetoothGattCharacteristic? = null
    private var pendingDescriptors = emptyList<BluetoothGattDescriptor>()
    private var descriptorIndex = 0
    private var disconnectReason: String? = null

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            sink.event(
                "gatt_connection_state",
                mapOf(
                    "method" to method.title,
                    "status" to status.toString(),
                    "statusName" to if (status == BluetoothGatt.GATT_SUCCESS) "GATT_SUCCESS" else "GATT_STATUS_$status",
                    "state" to stateName(newState),
                    "disconnectCause" to if (newState == BluetoothProfile.STATE_DISCONNECTED) disconnectReason ?: "remote_or_stack_disconnect" else "",
                ),
            )
            if (status != BluetoothGatt.GATT_SUCCESS) {
                connected.completeExceptionally(IllegalStateException("GATT connection status $status"))
                servicesReady.completeExceptionally(IllegalStateException("GATT connection status $status"))
                disconnected.complete(Unit)
                return
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connected.complete(Unit)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                disconnected.complete(Unit)
                if (!connected.isCompleted) connected.completeExceptionally(IllegalStateException("Disconnected before GATT connection completed"))
                if (!servicesReady.isCompleted) servicesReady.completeExceptionally(IllegalStateException("Disconnected before services were ready"))
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            sink.event("gatt_services_discovered", mapOf("method" to method.title, "status" to status.toString(), "serviceCount" to gatt.services.size.toString()))
            if (status != BluetoothGatt.GATT_SUCCESS) {
                servicesReady.completeExceptionally(IllegalStateException("Service discovery status $status"))
                return
            }
            gatt.services.forEach { service ->
                sink.event("gatt_service", mapOf("method" to method.title, "uuid" to service.uuid.toString(), "primary" to service.type.toString()))
                service.characteristics.forEach { characteristic ->
                    sink.event(
                        "gatt_characteristic",
                        mapOf("method" to method.title, "service" to service.uuid.toString(), "uuid" to characteristic.uuid.toString(), "properties" to characteristic.describeProperties(), "descriptorUuids" to characteristic.descriptors.joinToString(",") { it.uuid.toString() }),
                    )
                }
            }

            val service = gatt.getService(UUID.fromString(ConnectionDiagnosticProtocol.SERVICE_UUID))
                ?: run { servicesReady.completeExceptionally(IllegalStateException("Expected Wendougee service not found")); return }
            val data = service.getCharacteristic(UUID.fromString(ConnectionDiagnosticProtocol.DATA_UUID))
                ?: run { servicesReady.completeExceptionally(IllegalStateException("Expected Modbus characteristic not found")); return }
            val control = service.getCharacteristic(UUID.fromString(ConnectionDiagnosticProtocol.CONTROL_UUID))
                ?: run { servicesReady.completeExceptionally(IllegalStateException("Expected control characteristic not found")); return }
            dataCharacteristic = data
            pendingDescriptors = listOf(data, control).mapNotNull { it.getDescriptor(CCCD_UUID) }
            if (pendingDescriptors.size != 2) {
                servicesReady.completeExceptionally(IllegalStateException("CCCD missing from one or both protocol characteristics"))
                return
            }
            descriptorIndex = 0
            enableNextNotification(gatt)
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            sink.event("gatt_mtu_changed", mapOf("method" to method.title, "mtu" to mtu.toString(), "status" to status.toString()))
            mtuChanged.complete(mtu to status)
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            sink.event("gatt_cccd_write", mapOf("method" to method.title, "uuid" to descriptor.uuid.toString(), "status" to status.toString(), "value" to descriptor.value?.hex().orEmpty()))
            if (status != BluetoothGatt.GATT_SUCCESS) {
                servicesReady.completeExceptionally(IllegalStateException("CCCD write status $status"))
                return
            }
            descriptorIndex++
            enableNextNotification(gatt)
        }

        @Deprecated("Android calls the value overload on API 33 and later")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            characteristic.value?.let { handleNotification(characteristic, it) }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            handleNotification(characteristic, value)
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            sink.event("gatt_characteristic_write_callback", mapOf("method" to method.title, "characteristic" to characteristic.uuid.toString(), "status" to status.toString()))
        }
    }

    suspend fun connect(device: BluetoothDevice) {
        sink.event("gatt_connect_requested", mapOf("method" to method.title, "address" to device.address))
        gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
            ?: error("BluetoothDevice.connectGatt returned null")
        sink.event("gatt_connect_call_returned", mapOf("method" to method.title, "address" to device.address))
        connected.await()
        requestReferenceMtu(requireNotNull(gatt))
        val started = requireNotNull(gatt).discoverServices()
        sink.event("gatt_operation", mapOf("method" to method.title, "operation" to "discoverServices", "accepted" to started.toString()))
        check(started) { "discoverServices returned false" }
        servicesReady.await()
    }

    fun pendingResponseBytes(): ByteArray = decoder.pendingSnapshot()

    private suspend fun requestReferenceMtu(activeGatt: BluetoothGatt) {
        sink.event("gatt_mtu_policy", mapOf("method" to method.title, "requested" to "true", "requestedMtu" to REFERENCE_MTU.toString(), "reason" to "mirror LitaLite and Crema reference MTU"))
        val accepted = runCatching { activeGatt.requestMtu(REFERENCE_MTU) }
            .onFailure { failure ->
                sink.event("gatt_mtu_request", mapOf("method" to method.title, "requestedMtu" to REFERENCE_MTU.toString(), "accepted" to "false", "error" to (failure.message ?: failure::class.simpleName.orEmpty())))
            }
            .getOrDefault(false)
        if (accepted) {
            sink.event("gatt_mtu_request", mapOf("method" to method.title, "requestedMtu" to REFERENCE_MTU.toString(), "accepted" to "true"))
        }
        val result = if (accepted) withTimeoutOrNull(MTU_CALLBACK_TIMEOUT_MS) { mtuChanged.await() } else null
        sink.event(
            "gatt_mtu_result",
            mapOf(
                "method" to method.title,
                "requestedMtu" to REFERENCE_MTU.toString(),
                "requestAccepted" to accepted.toString(),
                "callbackReceived" to (result != null).toString(),
                "actualMtu" to result?.first?.toString().orEmpty(),
                "status" to result?.second?.toString().orEmpty(),
            ),
        )
    }

    suspend fun requestRead(): ByteArray {
        val activeGatt = gatt ?: error("No GATT session")
        val characteristic = dataCharacteristic ?: error("Modbus characteristic missing")
        val frame = ConnectionDiagnosticProtocol.readRequest()
        sink.event("modbus_tx", mapOf("method" to method.title, "hex" to frame.hex(), "writeType" to "WITHOUT_RESPONSE", "decodedAs" to "Read Holding Registers 0..36"))
        val queued = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activeGatt.writeCharacteristic(characteristic, frame, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = frame
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            activeGatt.writeCharacteristic(characteristic)
        }
        sink.event("gatt_write_queued", mapOf("method" to method.title, "accepted" to queued.toString()))
        check(queued) { "Android BLE stack rejected the Modbus write" }
        return responseFrame.await()
    }

    suspend fun close(reason: String) {
        val activeGatt = gatt ?: return
        disconnectReason = reason
        sink.event("gatt_disconnect_requested", mapOf("method" to method.title, "address" to activeGatt.device.address, "reason" to reason))
        val disconnectCall = runCatching { activeGatt.disconnect() }
        sink.event("gatt_disconnect_call_returned", mapOf("method" to method.title, "address" to activeGatt.device.address, "success" to disconnectCall.isSuccess.toString(), "error" to disconnectCall.exceptionOrNull()?.message.orEmpty()))
        val callbackReceived = withTimeoutOrNull(DISCONNECT_WAIT_MS) { disconnected.await(); true } ?: false
        val closeCall = runCatching { activeGatt.close() }
        sink.event("gatt_close_call_returned", mapOf("method" to method.title, "address" to activeGatt.device.address, "disconnectCallbackReceived" to callbackReceived.toString(), "success" to closeCall.isSuccess.toString(), "error" to closeCall.exceptionOrNull()?.message.orEmpty()))
        gatt = null
    }

    private fun enableNextNotification(activeGatt: BluetoothGatt) {
        if (descriptorIndex >= pendingDescriptors.size) {
            sink.event("gatt_notifications_ready", mapOf("method" to method.title, "subscribedCharacteristics" to pendingDescriptors.size.toString()))
            servicesReady.complete(Unit)
            return
        }
        val descriptor = pendingDescriptors[descriptorIndex]
        val characteristic = descriptor.characteristic
        val cccdValue = when {
            characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0 -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            else -> {
                servicesReady.completeExceptionally(IllegalStateException("Characteristic does not support notifications or indications"))
                return
            }
        }
        val localSet = activeGatt.setCharacteristicNotification(characteristic, true)
        sink.event("gatt_local_notification", mapOf("method" to method.title, "characteristic" to characteristic.uuid.toString(), "enabled" to localSet.toString(), "mode" to if (cccdValue.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) "notify" else "indicate"))
        if (!localSet) {
            servicesReady.completeExceptionally(IllegalStateException("setCharacteristicNotification returned false"))
            return
        }
        val accepted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activeGatt.writeDescriptor(descriptor, cccdValue) == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = cccdValue
            @Suppress("DEPRECATION")
            activeGatt.writeDescriptor(descriptor)
        }
        sink.event("gatt_cccd_requested", mapOf("method" to method.title, "uuid" to descriptor.uuid.toString(), "accepted" to accepted.toString()))
        if (!accepted) servicesReady.completeExceptionally(IllegalStateException("CCCD write was rejected by Android BLE stack"))
    }

    private fun handleNotification(characteristic: BluetoothGattCharacteristic, value: ByteArray) {
        val uuid = characteristic.uuid.toString()
        val isData = uuid.equals(ConnectionDiagnosticProtocol.DATA_UUID, ignoreCase = true)
        sink.event(
            if (isData) "notification_modbus" else "notification_control",
            mapOf("method" to method.title, "characteristic" to uuid, "length" to value.size.toString(), "hex" to value.hex(), "decodeStatus" to if (isData) "awaiting_modbus_frame_validation" else "raw_only_ff55_not_decoded"),
        )
        if (isData) decoder.append(value)
    }

    private fun BluetoothGattCharacteristic.describeProperties(): String {
        val bits = properties
        return buildList {
            if (bits and BluetoothGattCharacteristic.PROPERTY_READ != 0) add("read")
            if (bits and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) add("write")
            if (bits and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) add("writeWithoutResponse")
            if (bits and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) add("notify")
            if (bits and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) add("indicate")
            if (bits and BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE != 0) add("signedWrite")
        }.joinToString(",")
    }

    private fun stateName(state: Int): String = when (state) {
        BluetoothProfile.STATE_CONNECTED -> "connected"
        BluetoothProfile.STATE_CONNECTING -> "connecting"
        BluetoothProfile.STATE_DISCONNECTING -> "disconnecting"
        BluetoothProfile.STATE_DISCONNECTED -> "disconnected"
        else -> state.toString()
    }

    companion object {
        private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private const val REFERENCE_MTU = 200
        private const val MTU_CALLBACK_TIMEOUT_MS = 1_500L
        private const val DISCONNECT_WAIT_MS = 1_500L
    }
}

private fun ByteArray.hex(): String = joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
