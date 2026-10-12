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
    suspend fun open(ble: DeviceConnection.Ble): WendougeeBleConnection {
        val peripheral = resolvePeripheral(ble)
        try {
            // Flush any stale GATT from a prior session. Without this, Android's addGatt()
            // guard silently drops the new handle if the old onConnectionStateChange(DISCONNECTED)
            // hasn't fired yet — subsequent writes go to the dead GATT and no-op.
            runCatching { blueFalcon.disconnect(peripheral) }
            delay(STALE_GATT_SETTLE_MS)

            blueFalcon.connect(peripheral)

            if (!awaitConnected(peripheral)) {
                Logger.withTag(TAG).e { "Connection did not reach Connected state — aborting" }
                runCatching { blueFalcon.disconnect(peripheral) }
                error("BLE connection failed")
            }

            val (dataChar, ctrlChar) = awaitCharacteristics(peripheral)
                ?: run {
                    Logger.withTag(TAG).e { "Characteristics not ready — aborting connect" }
                    blueFalcon.disconnect(peripheral)
                    error("Wendougee characteristics unavailable")
                }

            tryChangeMtu(peripheral)

            // Disable notifications before enabling to guarantee the CCCD 0x0000 → 0x0001
            // write reaches the device on every connection regardless of cached BLE stack state.
            blueFalcon.notifyCharacteristic(peripheral, dataChar, notify = false)
            blueFalcon.notifyCharacteristic(peripheral, ctrlChar, notify = false)
            delay(NOTIFY_RESET_DELAY_MS)
            blueFalcon.notifyCharacteristic(peripheral, dataChar, notify = true)
            blueFalcon.notifyCharacteristic(peripheral, ctrlChar, notify = true)

            return WendougeeBleConnection(peripheral, dataChar, ctrlChar)
        } catch (failure: Throwable) {
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
    private suspend fun resolvePeripheral(ble: DeviceConnection.Ble): BluetoothPeripheral {
        blueFalcon.peripherals.value.firstOrNull { it.uuid == ble.peripheralId }?.let { return it }
        Logger.withTag(TAG).w {
            "Peripheral ${ble.peripheralId} not in cache; scanning for MAC=${ble.macAddress}"
        }
        return scanForMac(ble.macAddress, timeoutMs = SCAN_TIMEOUT_MS)
            ?: error("Device with MAC ${ble.macAddress} not found via scan")
    }

    private suspend fun scanForMac(targetMac: String, timeoutMs: Long): BluetoothPeripheral? = scanCoordinator.scanning {
        withTimeoutOrNull(timeoutMs) {
            blueFalcon.peripherals
                .mapNotNull { set ->
                    set.firstOrNull { p ->
                        discoverer.macAddressOf(BleAdvertisement(p.uuid, p.name)) == targetMac
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
        blueFalcon.discoverServices(peripheral)
        servicesDiscovered.await()
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
}

private fun BluetoothPeripheral.findBySuffix(suffix: String): BluetoothCharacteristic? =
    characteristics.firstOrNull {
        it.uuid.toString().equals("00010203-0405-0607-0809-0a0b0c0d$suffix", ignoreCase = true)
    }
