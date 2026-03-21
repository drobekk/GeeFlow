package dev.drobek.geeflow.data.device.ble

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BleClient {
    val connectionState: StateFlow<ConnectionState>
    val incomingData: SharedFlow<Pair<String, ByteArray>>

    fun connect(macAddress: String)
    fun disconnect()

    suspend fun writeCharacteristic(uuid: String, data: ByteArray, withoutEncoding: Boolean = true): Boolean
    suspend fun readCharacteristic(uuid: String): Boolean
    suspend fun notifyCharacteristic(uuid: String, notify: Boolean): Boolean
    suspend fun changeMTU(mtu: Int): Boolean

    enum class ConnectionState {
        Disconnected,
        Connecting,
        Connected
    }
}
