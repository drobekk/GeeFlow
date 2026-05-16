@file:OptIn(ExperimentalUuidApi::class)

package app.geeflow.data.device.ble.modbus

import dev.bluefalcon.core.BlueFalcon
import dev.bluefalcon.core.BluetoothCharacteristic
import dev.bluefalcon.core.BluetoothPeripheral
import dev.bluefalcon.core.plugin.BlueFalconClient
import dev.bluefalcon.core.plugin.BlueFalconPlugin
import dev.bluefalcon.core.plugin.PluginConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi

/**
 * Generic Modbus RTU-over-BLE plugin for Blue Falcon 3.x.
 *
 * Each peripheral that speaks Modbus exposes a pair of characteristics:
 *   - a request characteristic the client writes Modbus PDUs to
 *   - a response characteristic the device notifies with replies
 *
 * Construct a [ModbusSession] per (peripheral, request, response) tuple via [session],
 * then call [ModbusSession.readHoldingRegisters], [ModbusSession.writeSingleRegister], etc.
 * Sessions serialize request/response pairs via an internal mutex and match replies
 * against function code + register bytes for robust ACK correlation.
 *
 * Usage:
 * ```
 * val modbus = ModbusPlugin.create { defaultTimeout = 1.seconds }
 * val client = BlueFalcon {
 *     engine = myEngine
 *     install(modbus)
 * }
 * val session = modbus.session(peripheral, reqChar, respChar, unitId = 0x01)
 * session.writeSingleRegister(address = 0x0030, value = 93)
 * val temps = session.readHoldingRegisters(address = 0x0000, count = 4)
 * ```
 */
class ModbusPlugin(val config: Config) : BlueFalconPlugin {

    private var client: BlueFalcon? = null

    private val _traffic = MutableSharedFlow<ModbusTrafficEvent>(
        extraBufferCapacity = TRAFFIC_BUFFER_CAPACITY,
    )

    /**
     * Optional observation stream of every notification payload received on any
     * peripheral while the plugin is installed. Intended for logging/tracing — session
     * request/response correlation does not flow through this channel.
     */
    val traffic: SharedFlow<ModbusTrafficEvent> = _traffic.asSharedFlow()

    class Config : PluginConfig() {
        /** Default Modbus slave/unit id used when a session does not specify one. */
        var defaultUnitId: Byte = DEFAULT_UNIT_ID

        /** Default request/response timeout used when a session call omits one. */
        var defaultTimeout: Duration = DEFAULT_TIMEOUT
    }

    override fun install(client: BlueFalconClient, config: PluginConfig) {
        this.client = client as? BlueFalcon
            ?: error("ModbusPlugin requires a BlueFalcon client")
    }

    override suspend fun onNotificationReceived(
        peripheral: BluetoothPeripheral,
        characteristic: BluetoothCharacteristic,
        value: ByteArray,
    ) {
        _traffic.tryEmit(ModbusTrafficEvent(peripheral, characteristic, value))
    }

    /**
     * Create a Modbus session bound to a specific peripheral and request/response
     * characteristic pair.
     *
     * @param peripheral the connected peripheral
     * @param requestCharacteristic characteristic to write Modbus PDUs to
     * @param responseCharacteristic characteristic whose notifications carry replies
     *        (must already have notifications enabled via
     *        [BlueFalcon.notifyCharacteristic])
     * @param unitId Modbus slave id; defaults to [Config.defaultUnitId]
     */
    fun session(
        peripheral: BluetoothPeripheral,
        requestCharacteristic: BluetoothCharacteristic,
        responseCharacteristic: BluetoothCharacteristic,
        unitId: Byte = config.defaultUnitId,
    ): ModbusSession {
        val bf = client ?: error("ModbusPlugin not installed — call BlueFalcon { install(plugin) } first")
        return ModbusSession(
            client = bf,
            peripheral = peripheral,
            requestCharacteristic = requestCharacteristic,
            responseCharacteristic = responseCharacteristic,
            unitId = unitId,
            defaultTimeout = config.defaultTimeout,
        )
    }

    companion object {
        private const val TRAFFIC_BUFFER_CAPACITY = 64
        private const val DEFAULT_UNIT_ID: Byte = 0x01
        private val DEFAULT_TIMEOUT: Duration = 1.seconds

        fun create(configure: Config.() -> Unit = {}): ModbusPlugin =
            ModbusPlugin(Config().apply(configure))
    }
}

/** Raw notification event emitted while the plugin is installed. */
data class ModbusTrafficEvent(
    val peripheral: BluetoothPeripheral,
    val characteristic: BluetoothCharacteristic,
    val value: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModbusTrafficEvent) return false
        return peripheral.uuid == other.peripheral.uuid &&
            characteristic.uuid == other.characteristic.uuid &&
            value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        var result = peripheral.uuid.hashCode()
        result = 31 * result + characteristic.uuid.hashCode()
        result = 31 * result + value.contentHashCode()
        return result
    }
}

/** DSL entry point. */
fun installModbus(configure: ModbusPlugin.Config.() -> Unit = {}): ModbusPlugin =
    ModbusPlugin.create(configure)
