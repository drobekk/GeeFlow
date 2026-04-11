package dev.drobek.geeflow.data.device.ble

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

class ModbusBleClient(
    private val bleClient: BleClient,
    private val rxUuid: String,
    private val txUuid: String
) {
    private val mutex = Mutex()

    suspend fun writeSingleRegister(
        reg: Int,
        value: Int,
        timeoutMs: Long = 1000L
    ) {
        val regHi = (reg ushr 8).toByte()
        val regLo = (reg and 0xFF).toByte()
        val header = byteArrayOf(
            0x01,
            0x06,
            regHi,
            regLo,
            (value ushr 8).toByte(),
            (value and 0xFF).toByte()
        )
        val payload = header + ModbusCrcCalculator.calculateCRC(header)
        writeAndAwaitModbus(payload, 0x06, regHi, regLo, timeoutMs)
    }

    suspend fun writeMultipleRegisters(
        reg: Int,
        values: List<Int>,
        timeoutMs: Long = 1000L
    ) {
        val regHi = (reg ushr 8).toByte()
        val regLo = (reg and 0xFF).toByte()
        val num = values.size
        val byteCount = num * 2

        val header = byteArrayOf(
            0x01,
            0x10,
            regHi,
            regLo,
            (num ushr 8).toByte(),
            (num and 0xFF).toByte(),
            byteCount.toByte()
        )

        val data = ByteArray(byteCount)
        values.forEachIndexed { i, v ->
            data[i * 2] = (v ushr 8).toByte()
            data[i * 2 + 1] = (v and 0xFF).toByte()
        }

        val payload = header + data
        val fullCommand = payload + ModbusCrcCalculator.calculateCRC(payload)

        writeAndAwaitModbus(fullCommand, 0x10, regHi, regLo, timeoutMs)
    }

    suspend fun sendCustomCommandAndWaitForPrefix(
        payload: ByteArray,
        expectedPrefix: ByteArray,
        timeoutMs: Long = 1000L
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
        timeoutMs: Long = 1000L
    ) {
        mutex.withLock {
            withTimeout(timeoutMs) {
                val ackDeferred = async {
                    bleClient.incomingData.first { cd ->
                        cd.uuid.contains(rxUuid) && cd.value.size >= 4 &&
                            cd.value[1] == expectedFc &&
                            cd.value[2] == expectedRegHi &&
                            cd.value[3] == expectedRegLo
                    }
                }
                kotlinx.coroutines.yield()
                bleClient.writeCharacteristic(txUuid, payload)
                ackDeferred.await()
            }
        }
    }
}
