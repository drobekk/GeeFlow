package app.geeflow.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionDiagnosticProtocolTest {
    @Test
    fun `read request matches known read-only LitaLite fixture`() {
        assertEquals(ConnectionDiagnosticProtocol.READ_REQUEST_HEX, ConnectionDiagnosticProtocol.readRequest().hex())
    }

    @Test
    fun `valid fragmented response decodes all 37 holding registers`() {
        val frames = mutableListOf<ByteArray>()
        val decoder = ModbusResponseAccumulator(frames::add)
        val response = zeroRegisterResponse()

        decoder.append(byteArrayOf(0x55, 0x01, 0x03, 0x4A))
        decoder.append(response.copyOfRange(3, 31))
        decoder.append(response.copyOfRange(31, response.size))

        assertEquals(1, frames.size)
        val result = ConnectionDiagnosticProtocol.decodeFrame(frames.single())
        assertTrue(result.reason, result.valid)
        assertEquals(37, result.registers.size)
        assertTrue(result.registers.all { it == 0 })
    }

    @Test
    fun `incomplete notification preserves bytes for timeout diagnostics`() {
        val decoder = ModbusResponseAccumulator { error("Incomplete frame must not be emitted") }
        val fragment = byteArrayOf(0x01, 0x03, 0x4A) + ByteArray(17) { it.toByte() }

        decoder.append(fragment)

        assertTrue(fragment.contentEquals(decoder.pendingSnapshot()))
    }

    @Test
    fun `invalid CRC is reported without accepting the read`() {
        val response = zeroRegisterResponse().also { it[it.lastIndex] = (it.last().toInt() xor 0x7F).toByte() }

        val result = ConnectionDiagnosticProtocol.decodeFrame(response)

        assertFalse(result.valid)
        assertEquals("Modbus CRC mismatch", result.reason)
    }

    @Test
    fun `typed serial matching does not infer or rewrite a bluetooth address`() {
        assertTrue(ConnectionDiagnosticProtocol.serialMatches("WDG_Data_AB08118958", "AB08118958"))
        assertFalse(ConnectionDiagnosticProtocol.serialMatches("WDG_Data_1020BA044336", "AB08118958"))
    }

    @Test
    fun `typed serial can match advertised manufacturer or service data`() {
        val manufacturer = byteArrayOf(0x02, 0x01, 0x06) + "AB08118958".encodeToByteArray()
        val service = "serial=AB08118958;model=LITA".encodeToByteArray()

        assertTrue(ConnectionDiagnosticProtocol.serialMatchesData(listOf(manufacturer), "AB08118958"))
        assertTrue(ConnectionDiagnosticProtocol.serialMatchesData(listOf(service), "AB08118958"))
        assertFalse(ConnectionDiagnosticProtocol.serialMatchesData(listOf("AB0811895".encodeToByteArray()), "AB08118958"))
    }

    @Test
    fun `HCI fallback matches both radio address and advertised suffix`() {
        assertTrue(ConnectionDiagnosticProtocol.fallbackMatches(null, "10:20:ba:04:43:36"))
        assertTrue(ConnectionDiagnosticProtocol.fallbackMatches("WDG_Data_1020BA044336", "01:02:03:04:05:06"))
        assertFalse(ConnectionDiagnosticProtocol.fallbackMatches("WDG_Data_000000000000", "01:02:03:04:05:06"))
    }

    private fun zeroRegisterResponse(): ByteArray {
        val body = byteArrayOf(0x01, 0x03, 0x4A) + ByteArray(74)
        var crc = 0xFFFF
        body.forEach { byte ->
            crc = crc xor (byte.toInt() and 0xFF)
            repeat(8) { crc = if ((crc and 1) != 0) (crc ushr 1) xor 0xA001 else crc ushr 1 }
        }
        return body + byteArrayOf((crc and 0xFF).toByte(), ((crc ushr 8) and 0xFF).toByte())
    }

    private fun ByteArray.hex(): String = joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
}
