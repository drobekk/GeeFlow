package dev.drobek.geeflow.data.device.ble

import co.touchlab.kermit.Logger
import dev.bluefalcon.AdvertisementDataRetrievalKeys
import dev.bluefalcon.BlueFalcon
import dev.bluefalcon.BlueFalconDelegate
import dev.bluefalcon.BluetoothCharacteristic
import dev.bluefalcon.BluetoothPeripheral
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Single
class BlueFalconBleClient(
    private val scope: CoroutineScope,
    private val blueFalcon: BlueFalcon
) : BleClient, BlueFalconDelegate {

    private val _connectionState = MutableStateFlow(BleClient.ConnectionState.Disconnected)
    override val connectionState: StateFlow<BleClient.ConnectionState> = _connectionState.asStateFlow()

    private val _incomingData = MutableSharedFlow<Pair<String, ByteArray>>(extraBufferCapacity = 20)
    override val incomingData: SharedFlow<Pair<String, ByteArray>> = _incomingData.asSharedFlow()

    private var targetMacAddress: String? = null
    private var connectedPeripheral: BluetoothPeripheral? = null
    private var isConnectingToGatt = false
    private var connectionTimeoutJob: Job? = null

    private val characteristics = mutableMapOf<String, BluetoothCharacteristic>()
    
    companion object {
        private const val TAG = "BleClient"
        private val hexPattern = Regex("([a-fA-F0-9]{12})\$")
        private const val CONNECTION_TIMEOUT_MS = 10000L
    }

    init {
        blueFalcon.delegates.add(this)
    }

    override fun connect(macAddress: String) {
        if (_connectionState.value != BleClient.ConnectionState.Disconnected) {
            Logger.withTag(TAG).w { "Already connected or connecting, ignoring request for $macAddress" }
            return
        }
        isConnectingToGatt = false
        targetMacAddress = macAddress
        characteristics.clear()
        
        Logger.withTag(TAG).i { "Starting scan for target device: $macAddress" }

        _connectionState.update { BleClient.ConnectionState.Connecting }

        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = scope.launch {
            delay(CONNECTION_TIMEOUT_MS)
            if (_connectionState.value == BleClient.ConnectionState.Connecting) {
                Logger.withTag(TAG).w { "Connection timeout reached (${CONNECTION_TIMEOUT_MS}ms)" }
                blueFalcon.stopScanning()
                connectedPeripheral?.let { blueFalcon.disconnect(it) }
                _connectionState.update { BleClient.ConnectionState.Disconnected }
            }
        }

        blueFalcon.scan()
    }

    override fun disconnect() {
        connectionTimeoutJob?.cancel()
        connectedPeripheral?.let {
            Logger.withTag(TAG).i { "Initiating disconnect from ${it.uuid}" }
            blueFalcon.disconnect(it)
        }
    }

    override suspend fun writeCharacteristic(uuid: String, data: ByteArray, withoutEncoding: Boolean): Boolean {
        val peripheral = connectedPeripheral ?: return false
        val characteristic = characteristics.values.firstOrNull { it.name?.lowercase()?.contains(uuid.lowercase()) == true } ?: return false
        if (withoutEncoding) {
            blueFalcon.writeCharacteristicWithoutEncoding(peripheral, characteristic, data, 2)
        } else {
            blueFalcon.writeCharacteristic(peripheral, characteristic, data.decodeToString(), 2)
        }
        return true
    }

    override suspend fun readCharacteristic(uuid: String): Boolean {
        val peripheral = connectedPeripheral ?: return false
        val characteristic = characteristics.values.firstOrNull { it.name?.lowercase()?.contains(uuid.lowercase()) == true } ?: return false
        blueFalcon.readCharacteristic(peripheral, characteristic)
        return true
    }

    override suspend fun notifyCharacteristic(uuid: String, notify: Boolean): Boolean {
        val peripheral = connectedPeripheral ?: return false
        val characteristic = characteristics.values.firstOrNull { it.name?.lowercase()?.contains(uuid.lowercase()) == true } ?: return false
        blueFalcon.notifyCharacteristic(peripheral, characteristic, notify)
        return true
    }

    override suspend fun changeMTU(mtu: Int): Boolean {
        val peripheral = connectedPeripheral ?: return false
        blueFalcon.changeMTU(peripheral, mtu)
        return true
    }

    // Delegate methods
    override fun didDiscoverDevice(
        bluetoothPeripheral: BluetoothPeripheral,
        advertisementData: Map<AdvertisementDataRetrievalKeys, Any>
    ) {
        val target = targetMacAddress ?: return
        val normalizedTarget = target.filter { it.isLetterOrDigit() }.uppercase()

        val deviceName = advertisementData[AdvertisementDataRetrievalKeys.LocalName] as? String
            ?: bluetoothPeripheral.name ?: ""

        val macInName = hexPattern.find(deviceName)?.value?.uppercase()
        val isOurDevice = if (macInName != null) {
            macInName == normalizedTarget
        } else {
            bluetoothPeripheral.uuid.filter { it.isLetterOrDigit() }.uppercase() == normalizedTarget
        }

        if (isOurDevice && _connectionState.value == BleClient.ConnectionState.Connecting && !isConnectingToGatt) {
            isConnectingToGatt = true
            Logger.withTag(TAG).i { "Target device discovered ($deviceName). Stopping scan and connecting..." }
            blueFalcon.stopScanning()
            scope.launch {
                delay(200)
                blueFalcon.connect(bluetoothPeripheral, false)
            }
        }
    }

    override fun didConnect(bluetoothPeripheral: BluetoothPeripheral) {
        Logger.withTag(TAG).i { "Successfully connected to ${bluetoothPeripheral.uuid}" }
        connectionTimeoutJob?.cancel()
        connectedPeripheral = bluetoothPeripheral
        _connectionState.update { BleClient.ConnectionState.Connected }
        blueFalcon.discoverServices(bluetoothPeripheral)
    }

    override fun didDisconnect(bluetoothPeripheral: BluetoothPeripheral) {
        Logger.withTag(TAG).i { "Disconnected from ${bluetoothPeripheral.uuid}" }
        connectionTimeoutJob?.cancel()
        connectedPeripheral = null
        isConnectingToGatt = false
        characteristics.clear()
        _connectionState.update { BleClient.ConnectionState.Disconnected }
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
                val uuid = characteristic.name?.lowercase() ?: ""
                characteristics[uuid] = characteristic
                Logger.withTag(TAG).d { "Discovered characteristic: $uuid" }
            }
    }

    override fun didUpdateNotificationStateFor(
        bluetoothPeripheral: BluetoothPeripheral,
        bluetoothCharacteristic: BluetoothCharacteristic
    ) {
        val uuid = bluetoothCharacteristic.name?.lowercase() ?: ""
        Logger.withTag(TAG).d { "Notification state updated for: $uuid" }
        // Notify incoming data about state change so caller can react if needed
        // For now, WendougeeDataSController relied on this to start initialization.
        // We can emit an empty byte array or a special signal, or add a callback to BleClient.
        // Let's emit an empty array with the uuid.
        _incomingData.tryEmit(Pair(uuid, ByteArray(0)))
    }

    override fun didCharacteristcValueChanged(
        bluetoothPeripheral: BluetoothPeripheral,
        bluetoothCharacteristic: BluetoothCharacteristic
    ) {
        val data = bluetoothCharacteristic.value ?: return
        val uuid = bluetoothCharacteristic.name?.lowercase() ?: ""
        _incomingData.tryEmit(Pair(uuid, data))
    }
}
