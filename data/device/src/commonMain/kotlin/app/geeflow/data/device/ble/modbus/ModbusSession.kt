@file:Suppress("TooManyFunctions")

package app.geeflow.data.device.ble.modbus

import dev.bluefalcon.core.BlueFalcon
import dev.bluefalcon.core.BluetoothCharacteristic
import dev.bluefalcon.core.BluetoothPeripheral
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.time.Duration

/**
 * One Modbus session against a single peripheral and request/response characteristic pair.
 *
 * All operations serialize through an internal [Mutex], so concurrent calls on the same
 * session queue rather than interleave. Responses are matched against the function code
 * and, for address-bearing operations, the register/address bytes of the request so a
 * stale notification from a previous call does not satisfy the current one.
 *
 * Construct via [ModbusPlugin.session].
 */
class ModbusSession internal constructor(
    private val client: BlueFalcon,
    private val peripheral: BluetoothPeripheral,
    private val requestCharacteristic: BluetoothCharacteristic,
    private val responseCharacteristic: BluetoothCharacteristic,
    val unitId: Byte,
    private val defaultTimeout: Duration,
) {
    private val mutex = Mutex()

    suspend fun readCoils(address: Int, count: Int, timeout: Duration = defaultTimeout): List<Boolean> {
        val request = ModbusFrame.readCoils(unitId, address, count)
        val response = exchange(request, ModbusFunctionCode.READ_COILS, addressMatcher = null, timeout)
        val payload = ModbusFrame.parseResponse(response, unitId, ModbusFunctionCode.READ_COILS)
        val data = ModbusFrame.parseByteCountedPayload(payload)
        return ModbusFrame.decodeCoils(data, count)
    }

    suspend fun readDiscreteInputs(address: Int, count: Int, timeout: Duration = defaultTimeout): List<Boolean> {
        val request = ModbusFrame.readDiscreteInputs(unitId, address, count)
        val response = exchange(request, ModbusFunctionCode.READ_DISCRETE_INPUTS, addressMatcher = null, timeout)
        val payload = ModbusFrame.parseResponse(response, unitId, ModbusFunctionCode.READ_DISCRETE_INPUTS)
        val data = ModbusFrame.parseByteCountedPayload(payload)
        return ModbusFrame.decodeCoils(data, count)
    }

    suspend fun readHoldingRegisters(address: Int, count: Int, timeout: Duration = defaultTimeout): List<Int> {
        val request = ModbusFrame.readHoldingRegisters(unitId, address, count)
        val response = exchange(request, ModbusFunctionCode.READ_HOLDING_REGISTERS, addressMatcher = null, timeout)
        val payload = ModbusFrame.parseResponse(response, unitId, ModbusFunctionCode.READ_HOLDING_REGISTERS)
        val data = ModbusFrame.parseByteCountedPayload(payload)
        return ModbusFrame.decodeRegisters(data)
    }

    suspend fun readInputRegisters(address: Int, count: Int, timeout: Duration = defaultTimeout): List<Int> {
        val request = ModbusFrame.readInputRegisters(unitId, address, count)
        val response = exchange(request, ModbusFunctionCode.READ_INPUT_REGISTERS, addressMatcher = null, timeout)
        val payload = ModbusFrame.parseResponse(response, unitId, ModbusFunctionCode.READ_INPUT_REGISTERS)
        val data = ModbusFrame.parseByteCountedPayload(payload)
        return ModbusFrame.decodeRegisters(data)
    }

    suspend fun writeSingleCoil(address: Int, state: Boolean, timeout: Duration = defaultTimeout) {
        val request = ModbusFrame.writeSingleCoil(unitId, address, state)
        val response = exchange(
            request,
            ModbusFunctionCode.WRITE_SINGLE_COIL,
            addressMatcher = AddressMatcher(address),
            timeout,
        )
        ModbusFrame.parseResponse(response, unitId, ModbusFunctionCode.WRITE_SINGLE_COIL)
    }

    suspend fun writeSingleRegister(address: Int, value: Int, timeout: Duration = defaultTimeout) {
        val request = ModbusFrame.writeSingleRegister(unitId, address, value)
        val response = exchange(
            request,
            ModbusFunctionCode.WRITE_SINGLE_REGISTER,
            addressMatcher = AddressMatcher(address),
            timeout,
        )
        ModbusFrame.parseResponse(response, unitId, ModbusFunctionCode.WRITE_SINGLE_REGISTER)
    }

    suspend fun writeMultipleRegisters(
        address: Int,
        values: List<Int>,
        timeout: Duration = defaultTimeout,
    ) {
        val request = ModbusFrame.writeMultipleRegisters(unitId, address, values)
        val response = exchange(
            request,
            ModbusFunctionCode.WRITE_MULTIPLE_REGISTERS,
            addressMatcher = AddressMatcher(address),
            timeout,
        )
        ModbusFrame.parseResponse(response, unitId, ModbusFunctionCode.WRITE_MULTIPLE_REGISTERS)
    }

    suspend fun writeMultipleCoils(
        address: Int,
        states: List<Boolean>,
        timeout: Duration = defaultTimeout,
    ) {
        val request = ModbusFrame.writeMultipleCoils(unitId, address, states)
        val response = exchange(
            request,
            ModbusFunctionCode.WRITE_MULTIPLE_COILS,
            addressMatcher = AddressMatcher(address),
            timeout,
        )
        ModbusFrame.parseResponse(response, unitId, ModbusFunctionCode.WRITE_MULTIPLE_COILS)
    }

    /**
     * Write an already-assembled payload (CRC included) and wait for a response that
     * starts with [expectedPrefix]. Use for device-specific polling or proprietary
     * frames that do not fit the standard Modbus function codes.
     */
    suspend fun sendAndAwaitPrefix(
        payload: ByteArray,
        expectedPrefix: ByteArray,
        timeout: Duration = defaultTimeout,
    ): ByteArray = mutex.withLock {
        withTimeout(timeout) {
            coroutineScope {
                val ack = async {
                    responseCharacteristic.notifications.first { value ->
                        value.size >= expectedPrefix.size &&
                            value.sliceArray(expectedPrefix.indices).contentEquals(expectedPrefix)
                    }
                }
                yield()
                client.writeCharacteristic(
                    peripheral,
                    requestCharacteristic,
                    payload,
                    DEFAULT_WRITE_TYPE,
                )
                ack.await()
            }
        }
    }

    /**
     * Write an arbitrary Modbus frame (CRC included) and wait for a response with the
     * given function code, optionally matching on address bytes. For callers that build
     * non-standard frames but want the protocol framing + address matching benefits.
     */
    suspend fun sendAndAwaitFc(
        payload: ByteArray,
        expectedFc: Byte,
        addressMatcher: AddressMatcher? = null,
        timeout: Duration = defaultTimeout,
    ): ByteArray = exchange(payload, expectedFc, addressMatcher, timeout)

    private suspend fun exchange(
        payload: ByteArray,
        expectedFc: Byte,
        addressMatcher: AddressMatcher?,
        timeout: Duration,
    ): ByteArray = mutex.withLock {
        withTimeout(timeout) {
            coroutineScope {
                val exceptionFc = (expectedFc.toInt() or ModbusFunctionCode.EXCEPTION_MASK.toInt()).toByte()
                val ack = async {
                    responseCharacteristic.notifications.first { value ->
                        matchResponse(value, expectedFc, exceptionFc, addressMatcher)
                    }
                }
                yield()
                client.writeCharacteristic(
                    peripheral,
                    requestCharacteristic,
                    payload,
                    DEFAULT_WRITE_TYPE,
                )
                ack.await()
            }
        }
    }

    @Suppress("ReturnCount")
    private fun matchResponse(
        value: ByteArray,
        expectedFc: Byte,
        exceptionFc: Byte,
        addressMatcher: AddressMatcher?,
    ): Boolean {
        if (value.size < MIN_RESPONSE_SIZE) return false
        if (value[UNIT_ID_IDX] != unitId) return false
        val fc = value[FC_IDX]
        if (fc == exceptionFc) return true
        if (fc != expectedFc) return false
        if (addressMatcher == null) return true
        return value.size > ADDRESS_LO_IDX &&
            value[ADDRESS_HI_IDX] == addressMatcher.hi &&
            value[ADDRESS_LO_IDX] == addressMatcher.lo
    }

    /** Address bytes to match in the response. */
    data class AddressMatcher(val hi: Byte, val lo: Byte) {
        constructor(address: Int) : this(
            hi = ((address ushr BITS_PER_BYTE) and BYTE_MASK).toByte(),
            lo = (address and BYTE_MASK).toByte(),
        )

        companion object {
            private const val BYTE_MASK = 0xFF
            private const val BITS_PER_BYTE = 8
        }
    }

    companion object {
        private const val MIN_RESPONSE_SIZE = 4
        private const val UNIT_ID_IDX = 0
        private const val FC_IDX = 1
        private const val ADDRESS_HI_IDX = 2
        private const val ADDRESS_LO_IDX = 3

        /** WRITE_TYPE_NO_RESPONSE on Android, ignored on iOS/desktop. Matches legacy client. */
        private const val DEFAULT_WRITE_TYPE = 2
    }
}
