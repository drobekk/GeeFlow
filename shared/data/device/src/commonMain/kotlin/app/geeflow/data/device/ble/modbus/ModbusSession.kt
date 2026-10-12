@file:Suppress("TooManyFunctions")

package app.geeflow.data.device.ble.modbus

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

/**
 * One Modbus session against a single peripheral and request/response characteristic pair.
 *
 * All operations serialize through an internal [Mutex], so concurrent calls on the same
 * session queue rather than interleave. Responses are matched against the function code
 * and, for address-bearing operations, the register/address bytes of the request so a
 * stale notification from a previous call does not satisfy the current one.
 *
 * Transport-independent request/response client; accepts bytes from a connection-owned receiver.
 */
class ModbusSession internal constructor(
    private val notifications: Flow<ByteArray>,
    private val write: suspend (ByteArray) -> Unit,
    val unitId: Byte,
    private val defaultTimeout: Duration,
    private val completeFrames: Boolean = false,
    private val sessionJob: Job? = null,
) {
    private val mutex = Mutex()
    private var needsDrain = false

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

    private suspend fun exchange(
        payload: ByteArray,
        expectedFc: Byte,
        addressMatcher: AddressMatcher?,
        timeout: Duration,
    ): ByteArray = mutex.withLock {
        coroutineScope {
            val requestJob = currentCoroutineContext()[Job]
            val completion = sessionJob?.invokeOnCompletion { requestJob?.cancel() }
            try {
                require(payload.size >= REQUEST_MIN_SIZE && ModbusCrc.verify(payload)) { "Invalid Modbus request" }
                require(payload[UNIT_ID_IDX] == unitId && payload[FC_IDX] == expectedFc) {
                    "Request unit/function mismatch"
                }
                if (needsDrain) drainAfterTimeout()
                try {
                    withTimeout(timeout) {
                        coroutineScope {
                            val frames = ModbusNotifications()
                            var matched: ByteArray? = null
                            val ack = async(start = CoroutineStart.UNDISPATCHED) {
                                notifications.first { bytes ->
                                    matched = (if (completeFrames) listOf(bytes) else frames.receive(bytes)).firstOrNull {
                                        matchResponse(it, payload, expectedFc, addressMatcher)
                                    }
                                    matched != null
                                }
                                requireNotNull(matched)
                            }
                            write(payload)
                            val response = ack.await()
                            ModbusFrame.parseResponse(response, unitId, expectedFc)
                            response
                        }
                    }
                } catch (exception: CancellationException) {
                    needsDrain = true
                    throw exception
                }
            } finally {
                completion?.dispose()
            }
        }
    }

    /**
     * RTU has no transaction identifier. After a timeout discard notifications until a
     * quiet interval, with a bounded recovery window. An identical response arriving
     * after that window still cannot be distinguished from the new request's reply.
     */
    private suspend fun drainAfterTimeout() {
        withTimeoutOrNull(DRAIN_MAX_MS) {
            while (withTimeoutOrNull(DRAIN_QUIET_MS) { notifications.first() } != null) {
                // Each received fragment restarts the quiet interval.
            }
        }
        needsDrain = false
    }

    @Suppress("ReturnCount")
    private fun matchResponse(
        value: ByteArray,
        request: ByteArray,
        expectedFc: Byte,
        addressMatcher: AddressMatcher?,
    ): Boolean {
        if (value.size < MIN_RESPONSE_SIZE) return false
        if (value[UNIT_ID_IDX] != unitId) return false
        val fc = value[FC_IDX]
        val exceptionFc = (expectedFc.toInt() or ModbusFunctionCode.EXCEPTION_MASK.toInt()).toByte()
        if (fc == exceptionFc) return true
        if (fc != expectedFc) return false

        if (addressMatcher != null &&
            (value[ADDRESS_HI_IDX] != addressMatcher.hi || value[ADDRESS_LO_IDX] != addressMatcher.lo)
        ) {
            return false
        }
        return when (expectedFc) {
            ModbusFunctionCode.READ_COILS, ModbusFunctionCode.READ_DISCRETE_INPUTS ->
                (value[BYTE_COUNT_IDX].toInt() and BYTE_MASK) == (requestCount(request) + BITS_PER_BYTE - 1) / BITS_PER_BYTE
            ModbusFunctionCode.READ_HOLDING_REGISTERS, ModbusFunctionCode.READ_INPUT_REGISTERS ->
                (value[BYTE_COUNT_IDX].toInt() and BYTE_MASK) == requestCount(request) * 2
            else -> value.copyOfRange(ADDRESS_HI_IDX, ECHO_END_IDX)
                .contentEquals(request.copyOfRange(ADDRESS_HI_IDX, ECHO_END_IDX))
        }
    }

    private fun requestCount(request: ByteArray) =
        ((request[COUNT_HI_IDX].toInt() and BYTE_MASK) shl BITS_PER_BYTE) or (request[COUNT_LO_IDX].toInt() and BYTE_MASK)

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
        private const val MIN_RESPONSE_SIZE = 5
        private const val REQUEST_MIN_SIZE = 8
        private const val BYTE_MASK = 0xFF
        private const val BITS_PER_BYTE = 8
        private const val ECHO_END_IDX = 6
        private const val BYTE_COUNT_IDX = 2
        private const val COUNT_HI_IDX = 4
        private const val COUNT_LO_IDX = 5
        private const val DRAIN_MAX_MS = 2000L
        private const val DRAIN_QUIET_MS = 200L
        private const val UNIT_ID_IDX = 0
        private const val FC_IDX = 1
        private const val ADDRESS_HI_IDX = 2
        private const val ADDRESS_LO_IDX = 3
    }
}
