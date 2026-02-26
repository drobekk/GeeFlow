@file:OptIn(ExperimentalUuidApi::class)

package dev.drobek.geeflow.data.device.impl

import co.touchlab.kermit.Logger
import dev.bluefalcon.AdvertisementDataRetrievalKeys
import dev.bluefalcon.BlueFalcon
import dev.bluefalcon.BlueFalconDelegate
import dev.bluefalcon.BluetoothCharacteristic
import dev.bluefalcon.BluetoothPeripheral
import dev.drobek.geeflow.data.device.api.DeviceController
import dev.drobek.geeflow.domain.device.model.MachineState
import dev.drobek.geeflow.domain.device.model.MachineState.ConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.annotation.Singleton
import kotlin.uuid.ExperimentalUuidApi

@Singleton
class WendougeeController(
    private val scope: CoroutineScope,
    private val blueFalcon: BlueFalcon
) : DeviceController, BlueFalconDelegate {

    private val _machineState = MutableStateFlow(MachineState())
    override val machineState: StateFlow<MachineState> = _machineState.asStateFlow()

    private var targetMacAddress: String? = null
    private var connectedPeripheral: BluetoothPeripheral? = null
    private var pollingJob: Job? = null

    private var writeChar: BluetoothCharacteristic? = null
    private var notifyChar: BluetoothCharacteristic? = null

    private var connectionTimeoutJob: Job? = null

    companion object {
        private val CMD_POLLING = "0103057C001484D1".decodeHex()
        private val CMD_BREW_START = "01050096FF002C24".decodeHex()
        private val CMD_BREW_STOP = "0105009600006DE4".decodeHex()

        private const val DATA_UUID_SUFFIX = "2b10"
        private const val CTRL_UUID_SUFFIX = "2c10"

        private const val CONNECTION_TIMEOUT_MS = 10000L
        private const val POLLING_INTERVAL_MS = 1000L
        private const val BREW_PULSE_MS = 150L
    }

    init {
        blueFalcon.delegates.add(this)
    }

    private fun startPolling(peripheral: BluetoothPeripheral) {
        Logger.d { "Starting Modbus polling" }
        pollingJob?.cancel()
        pollingJob = scope.launch {
            Logger.d { "Negotiating MTU (512)" }
            blueFalcon.changeMTU(peripheral, 512)
            delay(1000)

            while (isActive) {
                writeChar?.let { char ->
                    try {
                        blueFalcon.writeCharacteristicWithoutEncoding(peripheral, char, CMD_POLLING, 2)
                    } catch (e: Exception) {
                        Logger.e(e) { "Error writing polling command" }
                    }
                }
                delay(POLLING_INTERVAL_MS)
            }
        }
    }

    override fun triggerManualBrew() {
        val peripheral = connectedPeripheral ?: return
        scope.launch {
            writeChar?.let { char ->
                Logger.i { "Triggering manual brew pulse" }
                try {
                    blueFalcon.writeCharacteristicWithoutEncoding(peripheral, char, CMD_BREW_START, 2)
                    delay(BREW_PULSE_MS)
                    blueFalcon.writeCharacteristicWithoutEncoding(peripheral, char, CMD_BREW_STOP, 2)
                } catch (e: Exception) {
                    Logger.e(e) { "Error during brew pulse execution" }
                }
            }
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") {
        it
            .toInt()
            .and(0xFF)
            .toString(16)
            .padStart(2, '0')
            .lowercase()
    }

    override fun connect(macAddress: String) {
        if (_machineState.value.connectionStatus != ConnectionStatus.Disconnected) {
            Logger.w { "Already connected or connecting, ignoring request for $macAddress" }
            return
        }
        targetMacAddress = macAddress
        Logger.i { "Starting scan for target device: $macAddress" }

        _machineState.update { it.copy(connectionStatus = ConnectionStatus.Connecting) }

        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = scope.launch {
            delay(CONNECTION_TIMEOUT_MS)
            if (_machineState.value.connectionStatus == ConnectionStatus.Connecting) {
                Logger.w { "Connection timeout reached (10s)" }
                blueFalcon.stopScanning()
                connectedPeripheral?.let { blueFalcon.disconnect(it) }
                _machineState.update { it.copy(connectionStatus = ConnectionStatus.Disconnected) }
            }
        }

        blueFalcon.scan()
    }

    override fun disconnect() {
        connectionTimeoutJob?.cancel()
        connectedPeripheral?.let {
            Logger.i { "Initiating disconnect from ${it.uuid}" }
            blueFalcon.disconnect(it)
        }
    }

    override fun didDiscoverDevice(
        bluetoothPeripheral: BluetoothPeripheral,
        advertisementData: Map<AdvertisementDataRetrievalKeys, Any>
    ) {
        val target = targetMacAddress ?: return
        val normalizedTarget = target
            .filter { it.isLetterOrDigit() }
            .uppercase()
        val pUuid = bluetoothPeripheral.uuid
            .filter { it.isLetterOrDigit() }
            .uppercase()

        if (pUuid == normalizedTarget && _machineState.value.connectionStatus == ConnectionStatus.Connecting) {
            Logger.i { "Target device discovered. Stopping scan and connecting..." }
            blueFalcon.stopScanning()
            scope.launch {
                delay(200)
                blueFalcon.connect(bluetoothPeripheral, false)
            }
        }
    }

    override fun didConnect(bluetoothPeripheral: BluetoothPeripheral) {
        Logger.i { "Successfully connected to ${bluetoothPeripheral.uuid}" }
        connectionTimeoutJob?.cancel()
        connectedPeripheral = bluetoothPeripheral
        _machineState.update { it.copy(connectionStatus = ConnectionStatus.Connected) }
        blueFalcon.discoverServices(bluetoothPeripheral)
    }

    override fun didDisconnect(bluetoothPeripheral: BluetoothPeripheral) {
        Logger.i { "Disconnected from ${bluetoothPeripheral.uuid}" }
        connectionTimeoutJob?.cancel()
        pollingJob?.cancel()
        writeChar = null
        notifyChar = null
        connectedPeripheral = null
        _machineState.update { it.copy(connectionStatus = ConnectionStatus.Disconnected) }
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

                if (uuid.contains(DATA_UUID_SUFFIX)) {
                    writeChar = characteristic
                    Logger.d { "DATA characteristic discovered and ready (2b10)" }
                    blueFalcon.notifyCharacteristic(bluetoothPeripheral, characteristic, true)
                }
                if (uuid.contains(CTRL_UUID_SUFFIX)) {
                    notifyChar = characteristic
                    Logger.d { "CTRL characteristic discovered and ready (2c10)" }
                    blueFalcon.notifyCharacteristic(bluetoothPeripheral, characteristic, true)
                }
            }
    }

    override fun didUpdateNotificationStateFor(
        bluetoothPeripheral: BluetoothPeripheral,
        bluetoothCharacteristic: BluetoothCharacteristic
    ) {
        val uuid = bluetoothCharacteristic.name?.lowercase() ?: ""
        Logger.d { "Notification state updated for: $uuid" }

        if (writeChar != null && notifyChar != null) {
            if (pollingJob == null || pollingJob?.isActive == false) {
                startPolling(bluetoothPeripheral)
            }
        }
    }

    override fun didCharacteristcValueChanged(
        bluetoothPeripheral: BluetoothPeripheral,
        bluetoothCharacteristic: BluetoothCharacteristic
    ) {
        val data = bluetoothCharacteristic.value ?: return
        val hex = data.toHex()

        if (hex.startsWith("010328")) {
            parseModbusBinaryState(data)
        }
    }

    private fun parseModbusBinaryState(payload: ByteArray) {
        try {
            val steamRaw = ((payload[11].toInt() and 0xFF) shl 8) or (payload[12].toInt() and 0xFF)
            val brewRaw = ((payload[13].toInt() and 0xFF) shl 8) or (payload[14].toInt() and 0xFF)
            val pressureRaw = ((payload[15].toInt() and 0xFF) shl 8) or (payload[16].toInt() and 0xFF)

            _machineState.update {
                it.copy(
                    steamBoilerTemp = steamRaw / 10.0f,
                    brewBoilerTemp = brewRaw / 10.0f,
                    pressure = pressureRaw / 10.0f
                )
            }
        } catch (e: Exception) {
            Logger.e(e) { "Modbus parser error" }
        }
    }
}

internal fun String.decodeHex(): ByteArray {
    check(length % 2 == 0) { "Must have an even length" }
    return chunked(2)
        .map {
            it
                .toInt(16)
                .toByte()
        }
        .toByteArray()
}
