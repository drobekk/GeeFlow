package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.impl.controller.WendougeeCommands.decodeHex
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.SmartScale
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WendougeeSmartScaleTest {
    private var state = DeviceState()
    private val found = mutableListOf<SmartScale>()
    private val scanStates = mutableListOf<Boolean>()
    private val parser = WendougeeFrameParser(
        onStateUpdate = { state = state.it() },
        onScaleFound = { found += it },
        onScanStatus = { scanStates += it },
    )

    @Test
    fun `when recorded mass registers differ then cup weight comes from register 1415`() {
        val telemetry = "0103280000001e0000000004f6038a0009005902cd0000000003f60000001d000000080000003c000002a774a6".decodeHex()

        parser.handleIncomingFrame(telemetry, "DATA")

        assertEquals(101.4f, state.weight)
        assertEquals(67.9f, state.weightRate)
    }

    @Test
    fun `when a grinder status arrives then the connected scale remains unchanged`() {
        state = connectedState()

        receive("ff55ffff8b000601014172657371")

        assertEquals(SCALE_NAME, state.smartScale?.name)
        assertTrue(found.isEmpty())
    }

    @Test
    fun `when scale status reports disconnected then stale connection and mass are cleared`() {
        state = connectedState()

        receive("ff55ffff8b00020200e2")

        assertNull(state.smartScale)
        assertNull(state.weight)
        assertNull(state.weightRate)
        assertEquals(SmartScale(SCALE_NAME, false), found.single())
    }

    @Test
    fun `when all integrations are enabled then scale bit remains enabled`() {
        receive("ff55ffff9a000107f5")

        assertTrue(state.smartScaleEnabled)
    }

    @Test
    fun `when toggling scale connectivity then grinder and reserved bits are preserved`() {
        val enable = buildScaleConnectivityFrame(0x83, true)
        val disable = buildScaleConnectivityFrame(0x87, false)

        assertContentEquals(buildControlFrame(0x9A, byteArrayOf(0x87.toByte())), enable)
        assertContentEquals(buildControlFrame(0x9A, byteArrayOf(0x83.toByte())), disable)
    }

    @Test
    fun `when scan stops then active search is cleared immediately`() {
        state = connectedState().copy(smartScaleSearchActive = true)

        receive("ff55ffff83000100d7")

        assertFalse(state.smartScaleSearchActive)
        assertEquals(listOf(false), scanStates)
    }

    @Test
    fun `when scan starts then search becomes active`() {
        state = connectedState()

        receive("ff55ffff83000101d8")

        assertTrue(state.smartScaleSearchActive)
        assertEquals(listOf(true), scanStates)
    }

    @Test
    fun `when another accessory disconnects then scale connection is retained`() {
        state = connectedState()

        receive("ff55ffff880004417265736a")

        assertEquals(SCALE_NAME, state.smartScale?.name)
        assertTrue(found.isEmpty())
    }

    @Test
    fun `when connected notification arrives then scale is connected without an additional acknowledgement`() {
        receive("ff55ffff860012424f4f4b4f4f5f53435f5520383936303538c1")

        assertEquals(SmartScale(SCALE_NAME, true), state.smartScale)
        assertEquals(SCALE_NAME, found.single().name)
    }

    @Test
    fun `when discovery reports a grinder then it is excluded from scales`() {
        val grinder = buildScaleFrame(0x81, "Milo123")

        parser.handleIncomingFrame(grinder, "CTRL")

        assertTrue(found.isEmpty())
        assertNull(state.smartScale)
    }

    @Test
    fun `when history includes a grinder then it is excluded from scales`() {
        val history = buildControlFrame(0x8C, byteArrayOf(0, 4) + "Milo".encodeToByteArray())

        parser.handleIncomingFrame(history, "CTRL")

        assertTrue(found.isEmpty())
    }

    @Test
    fun `when scale history follows connection then it does not mark the scale disconnected`() {
        state = connectedState()

        receive("ff55ffff8c00140212424f4f4b4f4f5f53435f5520383936303538dd")

        assertEquals(SmartScale(SCALE_NAME, true), found.single())
    }

    @Test
    fun `when a control frame is split then no state changes before its checksum arrives`() {
        val frame = "ff55ffff9a000107f5".decodeHex()

        parser.handleIncomingFrame(frame.copyOfRange(0, 8), "CTRL")
        val enabledBeforeChecksum = state.smartScaleEnabled
        parser.handleIncomingFrame(frame.copyOfRange(8, 9), "CTRL")

        assertFalse(enabledBeforeChecksum)
        assertTrue(state.smartScaleEnabled)
    }

    @Test
    fun `when invalid checksum precedes a valid frame then only the valid frame is applied`() {
        val invalid = "ff55ffff9a00010400".decodeHex()
        val valid = "ff55ffff9a000107f5".decodeHex()

        parser.handleIncomingFrame(invalid + valid, "CTRL")

        assertTrue(state.smartScaleEnabled)
    }

    @Test
    fun `when concatenated scan frames arrive then both statuses are applied`() {
        state = connectedState()
        val frames = "ff55ffff83000101d8ff55ffff83000100d7".decodeHex()

        parser.handleIncomingFrame(frames, "CTRL")

        assertEquals(listOf(true, false), scanStates)
        assertFalse(state.smartScaleSearchActive)
    }

    @Test
    fun `when parser is reset then old fragments cannot complete a new session frame`() {
        val frame = "ff55ffff9a000107f5".decodeHex()
        parser.handleIncomingFrame(frame.copyOfRange(0, 8), "CTRL")

        parser.reset()
        parser.handleIncomingFrame(frame.copyOfRange(8, 9), "CTRL")

        assertFalse(state.smartScaleEnabled)
    }

    @Test
    fun `when discovery reports the connected scale then its connected state is preserved`() {
        state = connectedState()

        parser.handleIncomingFrame(buildScaleFrame(0x81, SCALE_NAME), "CTRL")

        assertEquals(SmartScale(SCALE_NAME, true), found.single())
    }

    @Test
    fun `when integration is disabled then scale connection and measurements are cleared`() {
        state = connectedState().copy(smartScaleSearchActive = true)

        receive("ff55ffff9a000100ee")

        assertFalse(state.smartScaleEnabled)
        assertFalse(state.smartScaleSearchActive)
        assertNull(state.smartScale)
        assertNull(state.weight)
    }

    @Test
    fun `when refreshing discovery then scan command matches captured request`() {
        val command = WendougeeCommands.CMD_SCALE_SCAN

        assertContentEquals("ff55ffff82000101d7".decodeHex(), command)
    }

    private fun connectedState() = DeviceState(
        smartScale = SmartScale(SCALE_NAME, true),
        smartScaleEnabled = true,
        weight = 20f,
        weightRate = 2f,
    )

    private fun receive(hex: String) = parser.handleIncomingFrame(hex.decodeHex(), "CTRL")

    private companion object {
        const val SCALE_NAME = "BOOKOO_SC_U 896058"
    }
}
