@file:OptIn(ExperimentalUuidApi::class)
@file:Suppress("TooManyFunctions", "TooGenericExceptionCaught", "UnsafeCallOnNullableType")

package dev.drobek.geeflow.data.device.ble

import co.touchlab.kermit.Logger
import dev.bluefalcon.AdvertisementDataRetrievalKeys
import dev.bluefalcon.BlueFalcon
import dev.bluefalcon.BlueFalconDelegate
import dev.bluefalcon.BluetoothCharacteristic
import dev.bluefalcon.BluetoothPeripheral
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeout
import org.koin.core.annotation.Single
import kotlin.uuid.ExperimentalUuidApi

@Single
class BlueFalconBleClient(
    private val blueFalcon: BlueFalcon,
) : BleClient, BlueFalconDelegate {

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _incomingData = MutableSharedFlow<CharacteristicData>(extraBufferCapacity = 20)
    override val incomingData: SharedFlow<CharacteristicData> = _incomingData.asSharedFlow()

    private val _discoveredDevices = MutableStateFlow<Set<BleDevice>>(emptySet())
    override val discoveredDevices: StateFlow<Set<BleDevice>> = _discoveredDevices.asStateFlow()

    private var connectPeripheralId: String? = null
    private var connectDeferred: CompletableDeferred<Unit>? = null
    private var isConnectingToGatt = false
    private var connectedPeripheral: BluetoothPeripheral? = null

    private val characteristics = mutableMapOf<String, BluetoothCharacteristic>()

    companion object {
        private const val TAG = "BleClient"
    }

    init {
        blueFalcon.delegates.add(this)
    }

    override fun startScan() {
        _discoveredDevices.value = emptySet()
        blueFalcon.scan()
    }

    override fun stopScan() {
        if (connectPeripheralId != null) return // scan is owned by connect() — don't interrupt it
        blueFalcon.stopScanning()
    }

    override suspend fun connect(peripheralId: String, timeoutMs: Long) {
        check(_connectionState.value is BleConnectionState.Disconnected) {
            "Already connected or connecting — disconnect first"
        }

        val deferred = CompletableDeferred<Unit>()
        connectDeferred = deferred
        connectPeripheralId = peripheralId
        isConnectingToGatt = false
        characteristics.clear()

        _connectionState.value = BleConnectionState.Connecting

        try {
            withTimeout(timeoutMs) {
                val peripheral = blueFalcon.retrievePeripheral(peripheralId)
                    ?: throw BleConnectException("Peripheral $peripheralId not found — scan first or use controller fallback")
                Logger.withTag(TAG).i { "connect: connecting to $peripheralId" }
                isConnectingToGatt = true
                blueFalcon.connect(peripheral, false)
                deferred.await()
            }
            _connectionState.value = BleConnectionState.Connected(connectedPeripheral!!.uuid)
            Logger.withTag(TAG).i { "connect: ready, peripheralId=${connectedPeripheral!!.uuid}" }
        } catch (t: Throwable) {
            Logger.withTag(TAG).e(t) { "connect($peripheralId) failed" }
            blueFalcon.stopScanning()
            connectedPeripheral?.let { blueFalcon.disconnect(it) }
            connectPeripheralId = null
            connectDeferred = null
            isConnectingToGatt = false
            _connectionState.value = BleConnectionState.Disconnected
            throw BleConnectException("connect($peripheralId) failed: ${t.message}", t)
        }
    }

    override fun disconnect() {
        connectedPeripheral?.let {
            Logger.withTag(TAG).i { "disconnect: ${it.uuid}" }
            blueFalcon.disconnect(it)
        }
    }

    override suspend fun writeCharacteristic(uuid: String, data: ByteArray, withoutEncoding: Boolean): Boolean {
        val peripheral = connectedPeripheral ?: return false
        val characteristic = findCharacteristic(uuid) ?: return false
        if (withoutEncoding) {
            blueFalcon.writeCharacteristicWithoutEncoding(peripheral, characteristic, data, 2)
        } else {
            blueFalcon.writeCharacteristic(peripheral, characteristic, data.decodeToString(), 2)
        }
        return true
    }

    override suspend fun readCharacteristic(uuid: String): Boolean {
        val peripheral = connectedPeripheral ?: return false
        val characteristic = findCharacteristic(uuid) ?: return false
        blueFalcon.readCharacteristic(peripheral, characteristic)
        return true
    }

    override suspend fun notifyCharacteristic(uuid: String, notify: Boolean): Boolean {
        val peripheral = connectedPeripheral ?: return false
        val characteristic = findCharacteristic(uuid) ?: return false
        blueFalcon.notifyCharacteristic(peripheral, characteristic, notify)
        return true
    }

    override suspend fun changeMTU(mtu: Int): Boolean {
        val peripheral = connectedPeripheral ?: return false
        blueFalcon.changeMTU(peripheral, mtu)
        return true
    }

    override fun didDiscoverDevice(
        bluetoothPeripheral: BluetoothPeripheral,
        advertisementData: Map<AdvertisementDataRetrievalKeys, Any>,
    ) {
        val name = advertisementData[AdvertisementDataRetrievalKeys.LocalName] as? String
            ?: bluetoothPeripheral.name
        val bleDevice = BleDevice(peripheralId = bluetoothPeripheral.uuid, name = name)
        _discoveredDevices.update { existing ->
            if (existing.any { it.peripheralId == bleDevice.peripheralId }) existing else existing + bleDevice
        }
    }

    override fun didConnect(bluetoothPeripheral: BluetoothPeripheral) {
        Logger.withTag(TAG).i { "didConnect: ${bluetoothPeripheral.uuid}" }
        connectedPeripheral = bluetoothPeripheral
        // Discover services — deferred completes after characteristic discovery
        blueFalcon.discoverServices(bluetoothPeripheral)
    }

    override fun didDisconnect(bluetoothPeripheral: BluetoothPeripheral) {
        Logger.withTag(TAG).i { "didDisconnect: ${bluetoothPeripheral.uuid}" }
        val deferred = connectDeferred
        connectDeferred = null
        connectPeripheralId = null
        isConnectingToGatt = false
        connectedPeripheral = null
        characteristics.clear()
        if (deferred != null && deferred.isActive) {
            deferred.completeExceptionally(BleConnectException("disconnected during connect"))
        }
        _connectionState.value = BleConnectionState.Disconnected
    }

    override fun didDiscoverServices(bluetoothPeripheral: BluetoothPeripheral) {
        bluetoothPeripheral.services.forEach { service ->
            blueFalcon.discoverCharacteristics(bluetoothPeripheral, service.value)
        }
    }

    override fun didDiscoverCharacteristics(bluetoothPeripheral: BluetoothPeripheral) {
        bluetoothPeripheral.services.values
            .flatMap { it.characteristics }
            .forEach { characteristic ->
                val uuid = characteristic.name?.lowercase().orEmpty()
                characteristics[uuid] = characteristic
                Logger.withTag(TAG).d { "Discovered characteristic: $uuid" }
            }
        // Signal that the connect flow can proceed to Connected
        connectDeferred?.complete(Unit)
        connectDeferred = null
        connectPeripheralId = null
    }

    override fun didUpdateNotificationStateFor(
        bluetoothPeripheral: BluetoothPeripheral,
        bluetoothCharacteristic: BluetoothCharacteristic,
    ) {
        val uuid = bluetoothCharacteristic.name?.lowercase().orEmpty()
        _incomingData.tryEmit(CharacteristicData(uuid, ByteArray(0)))
    }

    override fun didCharacteristcValueChanged(
        bluetoothPeripheral: BluetoothPeripheral,
        bluetoothCharacteristic: BluetoothCharacteristic,
    ) {
        val data = bluetoothCharacteristic.value ?: return
        val uuid = bluetoothCharacteristic.name?.lowercase().orEmpty()
        _incomingData.tryEmit(CharacteristicData(uuid, data))
    }

    // endregion

    /**
     * Looks up a characteristic by exact UUID key first (full lowercase UUID string),
     * then falls back to substring match. Logs a warning on fallback so full UUIDs can
     * be collected and the short-suffix pattern retired.
     */
    private fun findCharacteristic(uuid: String): BluetoothCharacteristic? {
        characteristics[uuid.lowercase()]?.let { return it }
        return characteristics.values.firstOrNull {
            it.name?.lowercase()?.contains(uuid.lowercase()) == true
        }?.also {
            Logger.withTag(TAG).w {
                "findCharacteristic: no exact match for '$uuid'; matched '${it.name}' by substring. " +
                    "Collect full UUID from logs to retire the short-suffix pattern."
            }
        }
    }
}
