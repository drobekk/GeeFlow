package dev.drobek.geeflow.data.device.ble

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

class ModbusBleClient(
    private val bleClient: BleClient,
    private val rxUuid: String,
    private val txUuid: String,
) {
    private val mutex = Mutex()

    suspend fun writeSingleRegister(
        reg: Int,
        value: Int,
        timeoutMs: Long = 1000L,
    ) {
        val regHi = (reg ushr BYTE_SHIFT).toByte()
        val regLo = (reg and BYTE_MASK).toByte()
        val header = byteArrayOf(
            MODBUS_DEVICE_ADDRESS,
            FC_WRITE_SINGLE,
            regHi,
            regLo,
            (value ushr BYTE_SHIFT).toByte(),
            (value and BYTE_MASK).toByte(),
        )
        val payload = header + ModbusCrcCalculator.calculateCRC(header)
        writeAndAwaitModbus(payload, FC_WRITE_SINGLE, regHi, regLo, timeoutMs)
    }

    suspend fun writeMultipleRegisters(
        reg: Int,
        values: List<Int>,
        timeoutMs: Long = 1000L,
    ) {
        val regHi = (reg ushr BYTE_SHIFT).toByte()
        val regLo = (reg and BYTE_MASK).toByte()
        val num = values.size
        val byteCount = num * 2

        val header = byteArrayOf(
            MODBUS_DEVICE_ADDRESS,
            FC_WRITE_MULTIPLE,
            regHi,
            regLo,
            (num ushr BYTE_SHIFT).toByte(),
            (num and BYTE_MASK).toByte(),
            byteCount.toByte(),
        )

        val data = ByteArray(byteCount)
        values.forEachIndexed { i, v ->
            data[i * 2] = (v ushr BYTE_SHIFT).toByte()
            data[i * 2 + 1] = (v and BYTE_MASK).toByte()
        }

        val payload = header + data
        val fullCommand = payload + ModbusCrcCalculator.calculateCRC(payload)

        writeAndAwaitModbus(fullCommand, FC_WRITE_MULTIPLE, regHi, regLo, timeoutMs)
    }

    suspend fun sendCustomCommandAndWaitForPrefix(
        payload: ByteArray,
        expectedPrefix: ByteArray,
        timeoutMs: Long = 1000L,
    ) {
        mutex.withLock {
            withTimeout(timeoutMs) {
                val ackDeferred = async {
                    bleClient.incomingData.first { cd ->
                        cd.uuid.contains(rxUuid) && cd.value.size >= expectedPrefix.size &&
                            cd.value.sliceArray(expectedPrefix.indices).contentEquals(expectedPrefix)
                    }
                }
                kotlinx.coroutines.yield()
                bleClient.writeCharacteristic(txUuid, payload)
                ackDeferred.await()
            }
        }
    }

    suspend fun writeAndAwaitModbus(
        payload: ByteArray,
        expectedFc: Byte,
        expectedRegHi: Byte,
        expectedRegLo: Byte,
        timeoutMs: Long = 1000L,
    ) {
        mutex.withLock {
            withTimeout(timeoutMs) {
                val ackDeferred = async {
                    bleClient.incomingData.first { cd ->
                        cd.uuid.contains(rxUuid) && cd.value.size >= MODBUS_ACK_MIN_SIZE &&
                            cd.value[1] == expectedFc &&
                            cd.value[2] == expectedRegHi &&
                            cd.value[MODBUS_ACK_REG_LO_IDX] == expectedRegLo
                    }
                }
                kotlinx.coroutines.yield()
                bleClient.writeCharacteristic(txUuid, payload)
                ackDeferred.await()
            }
        }
    }

    companion object {
        private const val BYTE_MASK = 0xFF
        private const val BYTE_SHIFT = 8
        const val FC_WRITE_SINGLE: Byte = 0x06
        const val FC_WRITE_MULTIPLE: Byte = 0x10
        private const val MODBUS_DEVICE_ADDRESS: Byte = 0x01
        private const val MODBUS_ACK_MIN_SIZE = 4
        private const val MODBUS_ACK_REG_LO_IDX = 3
    }
}
