package dev.drobek.geeflow.data.device.ble

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BleClient {
    val connectionState: StateFlow<BleConnectionState>
    val incomingData: SharedFlow<CharacteristicData>
    val discoveredDevices: StateFlow<Set<BleDevice>>

    fun startScan()
    fun stopScan()

    /**
     * Suspends until GATT-connected and all characteristics discovered, or throws
     * [BleConnectException] on timeout / failure.
     *
     * Internally: scans, matches the first peripheral whose uuid == [peripheralId],
     * connects to GATT, and waits for characteristic discovery to complete.
     * On Android, [peripheralId] is the hardware MAC address.
     * On iOS, [peripheralId] is the platform UUID assigned by Core Bluetooth.
     */
    suspend fun connect(peripheralId: String, timeoutMs: Long = 10_000L)

    fun disconnect()

    suspend fun writeCharacteristic(uuid: String, data: ByteArray, withoutEncoding: Boolean = true): Boolean
    suspend fun readCharacteristic(uuid: String): Boolean
    suspend fun notifyCharacteristic(uuid: String, notify: Boolean): Boolean
    suspend fun changeMTU(mtu: Int): Boolean
}
