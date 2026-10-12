package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.ble.modbus.ModbusSession
import app.geeflow.data.device.model.SupportedDevice
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WendougeeFirmwareTest {
    @Test
    fun `when firmware is queried then reads the native firmware register`() = runTest {
        val modbus = mockk<ModbusSession>()
        coEvery { modbus.readHoldingRegisters(0x05A1, 1, any()) } returns listOf(212)

        val firmware = readWendougeeFirmware(modbus)

        assertEquals(212, firmware)
        coVerify(exactly = 1) { modbus.readHoldingRegisters(0x05A1, 1, any()) }
    }

    @Test
    fun `when firmware register is unsupported then returns unknown without failing connection`() = runTest {
        val modbus = mockk<ModbusSession>()
        coEvery { modbus.readHoldingRegisters(any(), any(), any()) } throws IllegalStateException("Illegal address")

        val firmware = readWendougeeFirmware(modbus)

        assertNull(firmware)
    }

    @Test
    fun `when firmware read times out then returns unknown`() = runTest {
        val modbus = mockk<ModbusSession>()
        coEvery { modbus.readHoldingRegisters(any(), any(), any()) } coAnswers {
            withTimeout(10) { awaitCancellation() }
        }

        val firmware = readWendougeeFirmware(modbus)

        assertNull(firmware)
    }

    @Test
    fun `when connection is cancelled then firmware read propagates cancellation`() = runTest {
        val modbus = mockk<ModbusSession>()
        coEvery { modbus.readHoldingRegisters(any(), any(), any()) } throws CancellationException()

        val result = runCatching { readWendougeeFirmware(modbus) }

        assertTrue(result.exceptionOrNull() is CancellationException)
    }

    @Test
    fun `when model or firmware lacks ring switching then does not expose Agile Ring`() {
        val lita = SupportedDevice.Lita
        val data = SupportedDevice.WendougeeDataS

        val supported = supportsAgileRing(lita, 212)
        val older = supportsAgileRing(lita, 211)
        val unknown = supportsAgileRing(lita, null)
        val otherModel = supportsAgileRing(data, 212)

        assertTrue(supported)
        assertFalse(older)
        assertFalse(unknown)
        assertFalse(otherModel)
    }
}
