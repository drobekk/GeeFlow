package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.ble.modbus.ModbusCrc
import app.geeflow.data.device.model.DeviceError
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.DeviceState.BrewStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WendougeeTelemetryParserTest {
    private var state = DeviceState()
    private val parser = WendougeeFrameParser(onStateUpdate = { state = state.it() })

    @Test
    fun `when a single status byte is idle then checksum bytes cannot report free variable brewing`() {
        val response = status(0)

        parser.handleIncomingFrame(response, "DATA")

        assertEquals(BrewStatus.Idle, state.brewStatus)
    }

    @Test
    fun `when any of the first four status bits is set then reports profile brewing`() {
        val makingBits = listOf(1, 2, 4, 8)

        val statuses = makingBits.map { bit ->
            parser.handleIncomingFrame(status(bit), "DATA")
            state.brewStatus
        }

        assertEquals(List(makingBits.size) { BrewStatus.Profile }, statuses)
    }

    @Test
    fun `when multiple first byte states are set then the lowest active bit wins`() {
        val bytes = listOf(0x11, 0x30, 0x60, 0xC0)

        val statuses = bytes.map { value ->
            parser.handleIncomingFrame(status(value, 0, 0), "DATA")
            state.brewStatus
        }

        assertEquals(listOf(BrewStatus.Profile, BrewStatus.Manual, BrewStatus.Cleaning, BrewStatus.WaterFlow), statuses)
    }

    @Test
    fun `when free hand and general activity flags coexist then free hand remains stoppable`() {
        val activityBytes = listOf(1, 2, 4, 8, 0x10, 0x20, 0x40, 0x80, 0xFF)

        val statuses = activityBytes.map { activity ->
            parser.handleIncomingFrame(status(activity, 8, 0), "DATA")
            state.brewStatus
        }

        assertEquals(List(activityBytes.size) { BrewStatus.FreeVariable }, statuses)
    }

    @Test
    fun `when free hand stops then general activity and idle states are recognized again`() {
        parser.handleIncomingFrame(status(0x40, 8, 0), "DATA")

        parser.handleIncomingFrame(status(0x40, 0, 0), "DATA")
        val waterFlowStatus = state.brewStatus
        parser.handleIncomingFrame(status(0, 0, 0), "DATA")
        val idleStatus = state.brewStatus

        assertEquals(BrewStatus.WaterFlow, waterFlowStatus)
        assertEquals(BrewStatus.Idle, idleStatus)
    }

    @Test
    fun `when a complete second status byte reports free variable then retains that mode`() {
        val response = status(0, 8, 0)

        parser.handleIncomingFrame(response, "DATA")

        assertEquals(BrewStatus.FreeVariable, state.brewStatus)
    }

    @Test
    fun `when status and telemetry share one notification then both frames update the state`() {
        val response = status(0, 8, 0) + telemetry(20)

        parser.handleIncomingFrame(response, "DATA")

        assertEquals(BrewStatus.FreeVariable, state.brewStatus)
        assertEquals(93.5f, state.brewBoilerTemp)
        assertEquals(DeviceError(7), state.error)
    }

    @Test
    fun `when water flow or the last first byte bit is active then the machine is not idle`() {
        val responses = listOf(status(0x40), status(0x80))

        val statuses = responses.map { response ->
            parser.handleIncomingFrame(response, "DATA")
            state.brewStatus
        }

        assertEquals(listOf(BrewStatus.WaterFlow, BrewStatus.Cleaning), statuses)
    }

    @Test
    fun `when status has an unsupported byte count then preserves the previous state`() {
        state = state.copy(brewStatus = BrewStatus.Profile)
        val responses = listOf(status(), status(0, 0, 0, 0))

        responses.forEach { parser.handleIncomingFrame(it, "DATA") }

        assertEquals(BrewStatus.Profile, state.brewStatus)
    }

    @Test
    fun `when a telemetry frame arrives in fragments then updates only after the full frame`() {
        val response = telemetry(20)

        parser.handleIncomingFrame(response.copyOfRange(0, 11), "DATA")
        val incompleteState = state
        parser.handleIncomingFrame(response.copyOfRange(11, response.size), "DATA")

        assertNull(incompleteState.pressure)
        assertEquals(9.3f, state.pressure)
        assertEquals(101.4f, state.weight)
        assertEquals(DeviceError(7), state.error)
    }

    @Test
    fun `when extended telemetry has twenty two words then decodes the native shared fields`() {
        val response = telemetry(22)

        parser.handleIncomingFrame(response, "DATA")

        assertEquals(93.5f, state.brewBoilerTemp)
        assertEquals(101.4f, state.weight)
        assertEquals(12f, state.weightRate)
        assertEquals(DeviceError(7), state.error)
    }

    @Test
    fun `when telemetry has an unsupported word count then preserves existing values`() {
        state = state.copy(error = DeviceError(2), weight = 42f)
        val response = telemetry(21)

        parser.handleIncomingFrame(response, "DATA")

        assertEquals(DeviceError(2), state.error)
        assertEquals(42f, state.weight)
    }

    @Test
    fun `when telemetry has a wrong unit or checksum then cannot overwrite an alarm`() {
        state = state.copy(error = DeviceError(2))
        val otherUnit = telemetry(20, unit = 2)
        val corrupted = telemetry(20).also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }

        parser.handleIncomingFrame(otherUnit, "DATA")
        parser.handleIncomingFrame(corrupted, "DATA")

        assertEquals(DeviceError(2), state.error)
        assertNull(state.telemetryTime)
    }

    @Test
    fun `when parser is reset between fragments then previous connection bytes are discarded`() {
        val response = telemetry(20)
        parser.handleIncomingFrame(response.copyOfRange(0, 11), "DATA")

        parser.reset()
        parser.handleIncomingFrame(response.copyOfRange(11, response.size), "DATA")

        assertNull(state.telemetryTime)
    }

    @Test
    fun `when a DATA fragment starts with FF55 then it remains part of the Modbus response`() {
        val response = telemetry(20).copyOfRange(0, 43).also {
            it[7] = 0xFF.toByte()
            it[8] = 0x55
        }.let(ModbusCrc::append)

        parser.handleIncomingFrame(response.copyOfRange(0, 7), "DATA")
        parser.handleIncomingFrame(response.copyOfRange(7, response.size), "DATA")

        assertEquals(DeviceError(0xFF55), state.error)
        assertEquals(93.5f, state.brewBoilerTemp)
    }

    private fun status(vararg values: Int): ByteArray = ModbusCrc.append(
        byteArrayOf(1, 1, values.size.toByte()) + values.map { it.toByte() }.toByteArray(),
    )

    private fun telemetry(wordCount: Int, unit: Int = 1): ByteArray {
        val words = MutableList(wordCount) { 0 }
        words[1] = 27
        words[2] = 7
        words[4] = 1234
        words[5] = 935
        words[6] = 93
        words[11] = 1014
        words[18] = 6
        words[19] = 679
        val payload = words.flatMap { listOf((it shr 8).toByte(), it.toByte()) }.toByteArray()
        return ModbusCrc.append(byteArrayOf(unit.toByte(), 3, payload.size.toByte()) + payload)
    }
}
