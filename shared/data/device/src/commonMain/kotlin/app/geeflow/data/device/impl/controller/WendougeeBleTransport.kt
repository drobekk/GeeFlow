@file:Suppress("TooGenericExceptionCaught")

@file:OptIn(ExperimentalUuidApi::class)

package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.ble.BleScanCoordinator
import app.geeflow.data.device.impl.discovery.BleAdvertisement
import app.geeflow.data.device.impl.discovery.WendougeeBleDeviceDiscoverer
import app.geeflow.data.device.model.DeviceConnection
import co.touchlab.kermit.Logger
import dev.bluefalcon.core.BlueFalcon
import dev.bluefalcon.core.BluetoothCharacteristic
import dev.bluefalcon.core.BluetoothPeripheral
import dev.bluefalcon.core.BluetoothPeripheralState
import dev.bluefalcon.core.ServiceDiscoveryPhase
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.Single
import kotlin.uuid.ExperimentalUuidApi

internal data class WendougeeBleConnection(
    val peripheral: BluetoothPeripheral,
    val data: BluetoothCharacteristic,
    val control: BluetoothCharacteristic,
) {
    val peripheralId: String get() = peripheral.uuid
}

internal data class WendougeeNotification(val control: Boolean, val bytes: ByteArray)

@Single
internal class WendougeeBleTransport(
    private val blueFalcon: BlueFalcon,
    private val discoverer: WendougeeBleDeviceDiscoverer,
    private val scanCoordinator: BleScanCoordinator,
) {
    suspend fun open(
        ble: DeviceConnection.Ble,
        diagnosticSink: ConnectionDiagnosticSink? = null,
    ): WendougeeBleConnection {
        val peripheral = resolvePeripheral(ble, diagnosticSink)
        diagnosticSink.record("gatt_stale_disconnect_started", mapOf("address" to peripheral.uuid))
        try {
            // Flush any stale GATT from a prior session. Without this, Android's addGatt()
            // guard silently drops the new handle if the old onConnectionStateChange(DISCONNECTED)
            // hasn't fired yet — subsequent writes go to the dead GATT and no-op.
            val staleDisconnect = runCatching { blueFalcon.disconnect(peripheral) }
            diagnosticSink.record(
                "gatt_stale_disconnect_finished",
                mapOf("address" to peripheral.uuid, "success" to staleDisconnect.isSuccess.toString(), "error" to staleDisconnect.exceptionOrNull()?.message.orEmpty()),
            )
            delay(STALE_GATT_SETTLE_MS)

            diagnosticSink.record("gatt_connect_requested", mapOf("address" to peripheral.uuid))
            blueFalcon.connect(peripheral)
            diagnosticSink.record("gatt_connect_call_returned", mapOf("address" to peripheral.uuid))

            val connected = awaitConnected(peripheral)
            diagnosticSink.record(
                "gatt_connection_state_result",
                mapOf("address" to peripheral.uuid, "connected" to connected.toString(), "state" to blueFalcon.connectionState(peripheral).toString()),
            )
            if (!connected) {
                Logger.withTag(TAG).e { "Connection did not reach Connected state — aborting" }
                runCatching { blueFalcon.disconnect(peripheral) }
                error("BLE connection failed")
            }

            val (dataChar, ctrlChar) = awaitCharacteristics(peripheral, diagnosticSink)
                ?: run {
                    diagnosticSink.record("gatt_characteristics_timeout", mapOf("address" to peripheral.uuid, "timeoutMs" to CHARS_READY_TIMEOUT_MS.toString()))
                    Logger.withTag(TAG).e { "Characteristics not ready — aborting connect" }
                    blueFalcon.disconnect(peripheral)
                    error("Wendougee characteristics unavailable")
                }

            tryChangeMtu(peripheral, diagnosticSink)

            // Disable notifications before enabling to guarantee the CCCD 0x0000 → 0x0001
            // write reaches the device on every connection regardless of cached BLE stack state.
            diagnosticSink.record("gatt_notification_toggle_requested", mapOf("characteristic" to dataChar.uuid.toString(), "enabled" to "false"))
            blueFalcon.notifyCharacteristic(peripheral, dataChar, notify = false)
            diagnosticSink.record("gatt_notification_toggle_returned", mapOf("characteristic" to dataChar.uuid.toString(), "enabled" to "false", "notifying" to dataChar.isNotifying.toString()))
            diagnosticSink.record("gatt_notification_toggle_requested", mapOf("characteristic" to ctrlChar.uuid.toString(), "enabled" to "false"))
            blueFalcon.notifyCharacteristic(peripheral, ctrlChar, notify = false)
            diagnosticSink.record("gatt_notification_toggle_returned", mapOf("characteristic" to ctrlChar.uuid.toString(), "enabled" to "false", "notifying" to ctrlChar.isNotifying.toString()))
            delay(NOTIFY_RESET_DELAY_MS)
            diagnosticSink.record("gatt_notification_toggle_requested", mapOf("characteristic" to dataChar.uuid.toString(), "enabled" to "true"))
            blueFalcon.notifyCharacteristic(peripheral, dataChar, notify = true)
            diagnosticSink.record("gatt_notification_toggle_returned", mapOf("characteristic" to dataChar.uuid.toString(), "enabled" to "true", "notifying" to dataChar.isNotifying.toString()))
            diagnosticSink.record("gatt_notification_toggle_requested", mapOf("characteristic" to ctrlChar.uuid.toString(), "enabled" to "true"))
            blueFalcon.notifyCharacteristic(peripheral, ctrlChar, notify = true)
            diagnosticSink.record("gatt_notification_toggle_returned", mapOf("characteristic" to ctrlChar.uuid.toString(), "enabled" to "true", "notifying" to ctrlChar.isNotifying.toString()))
            diagnosticSink.record("gatt_notifications_requested", mapOf("data" to dataChar.uuid.toString(), "control" to ctrlChar.uuid.toString(), "apiCallsReturned" to "true"))

            return WendougeeBleConnection(peripheral, dataChar, ctrlChar)
        } catch (failure: Throwable) {
            diagnosticSink.record("gatt_open_failed", mapOf("address" to peripheral.uuid, "error" to (failure.message ?: failure::class.simpleName.orEmpty())))
            withContext(NonCancellable) { runCatching { blueFalcon.disconnect(peripheral) } }
            throw failure
        }
    }

    suspend fun close(connection: WendougeeBleConnection) = blueFalcon.disconnect(connection.peripheral)
    fun isDisconnected(connection: WendougeeBleConnection): Boolean =
        blueFalcon.connectionState(connection.peripheral) == BluetoothPeripheralState.Disconnected
    suspend fun writeData(connection: WendougeeBleConnection, data: ByteArray) =
        blueFalcon.writeCharacteristic(connection.peripheral, connection.data, data, WRITE_TYPE_NO_RESPONSE)
    suspend fun writeControl(connection: WendougeeBleConnection, data: ByteArray) =
        blueFalcon.writeCharacteristic(connection.peripheral, connection.control, data, WRITE_TYPE_NO_RESPONSE)
    fun notifications(connection: WendougeeBleConnection): Flow<WendougeeNotification> =
        blueFalcon.engine.characteristicNotifications.mapNotNull { notification ->
            if (notification.peripheral.uuid != connection.peripheral.uuid || notification.value.isEmpty()) {
                null
            } else {
                when (notification.characteristic.uuid) {
                    connection.data.uuid -> WendougeeNotification(false, notification.value)
                    connection.control.uuid -> WendougeeNotification(true, notification.value)
                    else -> null
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
    private suspend fun resolvePeripheral(
        ble: DeviceConnection.Ble,
        diagnosticSink: ConnectionDiagnosticSink?,
    ): BluetoothPeripheral {
        blueFalcon.peripherals.value.firstOrNull { it.uuid == ble.peripheralId }?.let {
            diagnosticSink.record("gatt_peripheral_cache_hit", mapOf("peripheralId" to ble.peripheralId, "name" to it.name.orEmpty()))
            return it
        }
        diagnosticSink.record("gatt_peripheral_cache_miss", mapOf("peripheralId" to ble.peripheralId, "mac" to ble.macAddress))
        Logger.withTag(TAG).w {
            "Peripheral ${ble.peripheralId} not in cache; scanning for MAC=${ble.macAddress}"
        }
        diagnosticSink.record("gatt_fallback_scan_started", mapOf("mac" to ble.macAddress, "timeoutMs" to SCAN_TIMEOUT_MS.toString()))
        val scanned = scanForMac(ble.macAddress, timeoutMs = SCAN_TIMEOUT_MS)
        diagnosticSink.record(
            "gatt_fallback_scan_finished",
            mapOf("mac" to ble.macAddress, "found" to (scanned != null).toString(), "address" to scanned?.uuid.orEmpty(), "name" to scanned?.name.orEmpty()),
        )
        return scanned ?: error("Device with MAC ${ble.macAddress} not found via scan")
    }

    private suspend fun scanForMac(targetMac: String, timeoutMs: Long): BluetoothPeripheral? = scanCoordinator.scanning {
        withTimeoutOrNull(timeoutMs) {
            blueFalcon.peripherals
                .mapNotNull { set ->
                    set.firstOrNull { p ->
                        discoverer.matchesAddress(BleAdvertisement(p.uuid, p.name), targetMac)
                    }
                }
                .first()
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
        diagnosticSink: ConnectionDiagnosticSink?,
    ): Pair<BluetoothCharacteristic, BluetoothCharacteristic>? = withTimeoutOrNull(CHARS_READY_TIMEOUT_MS) {
        delay(POST_CONNECT_SETTLE_MS)
        // Reconnect retains the Kotlin service list, but the native GATT handles are new.
        // Subscribe before discovery and wait for fresh services before requesting characteristics.
        val servicesDiscovered = async(start = CoroutineStart.UNDISPATCHED) {
            blueFalcon.serviceDiscoveryUpdates.first {
                it.peripheral.uuid == peripheral.uuid &&
                    it.phase == ServiceDiscoveryPhase.ServicesDiscovered
            }
        }
        Logger.withTag(TAG).d { "Triggering discoverServices" }
        diagnosticSink.record("gatt_discover_services_requested", mapOf("address" to peripheral.uuid))
        blueFalcon.discoverServices(peripheral)
        servicesDiscovered.await()
        diagnosticSink.record(
            "gatt_services_discovered",
            mapOf(
                "address" to peripheral.uuid,
                "services" to peripheral.services.joinToString(";") { "${it.uuid}:${it.name.orEmpty()}" },
                "characteristics" to peripheral.characteristics.joinToString(";") { characteristic ->
                    "${characteristic.uuid}:${characteristic.name.orEmpty()}:notifying=${characteristic.isNotifying}:descriptors=${characteristic.descriptors.joinToString(",") { it.uuid.toString() }}"
                },
            ),
        )
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
            if (data != null && ctrl != null) {
                diagnosticSink.record("gatt_protocol_characteristics_found", mapOf("data" to data.uuid.toString(), "control" to ctrl.uuid.toString()))
                return@withTimeoutOrNull data to ctrl
            }
            delay(CHARS_RETRY_DELAY_MS)
        }
        null
    }

    private suspend fun tryChangeMtu(peripheral: BluetoothPeripheral, diagnosticSink: ConnectionDiagnosticSink?) {
        diagnosticSink.record("gatt_mtu_request", mapOf("address" to peripheral.uuid, "requestedMtu" to MTU_SIZE.toString()))
        val result = withTimeoutOrNull(MTU_TIMEOUT_MS) {
            runCatching { blueFalcon.changeMTU(peripheral, MTU_SIZE) }
        }
        diagnosticSink.record(
            "gatt_mtu_result",
            mapOf(
                "address" to peripheral.uuid,
                "completed" to (result != null).toString(),
                "success" to (result?.isSuccess ?: false).toString(),
                "error" to result?.exceptionOrNull()?.message.orEmpty(),
            ),
        )
        if (result == null || result.isFailure) {
            Logger.withTag(TAG).w { "MTU negotiation failed or timed out" }
        }
        delay(POST_MTU_DELAY_MS)
    }
}

private fun ConnectionDiagnosticSink?.record(name: String, fields: Map<String, String>) {
    runCatching { this?.event(name, fields) }
}

private fun BluetoothPeripheral.findBySuffix(suffix: String): BluetoothCharacteristic? =
    characteristics.firstOrNull {
        it.uuid.toString().equals("00010203-0405-0607-0809-0a0b0c0d$suffix", ignoreCase = true)
    }
