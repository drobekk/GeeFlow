package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.impl.controller.WendougeeCommands.decodeHex
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.SingleDoseGrinder
import app.geeflow.data.device.model.SmartScale
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WendougeeSingleDoseTest {
    private var state = DeviceState(
        smartScaleEnabled = true,
        smartScale = SmartScale("BOOKOO_Test", true),
        weight = 42f,
        singleDoseGrinderEnabled = true,
    )
    private val found = mutableListOf<SingleDoseGrinder>()
    private val parser = WendougeeFrameParser(
        onStateUpdate = { state = state.it() },
        onGrinderFound = { found += it },
    )

    @Test
    fun `when discoveries contain all accessory types then only Single Dose names enter grinder list`() {
        listOf("Milo_123", "BGM_456", "WDGm_Ares", "BOOKOO_Test", "Unknown").forEach {
            receive(0x81, it.encodeToByteArray())
        }

        assertEquals(listOf("Milo_123", "BGM_456"), found.map { it.name })
        assertEquals("BOOKOO_Test", state.smartScale?.name)
    }

    @Test
    fun `when grinder connects then scale connection and weight remain unchanged`() {
        receive(0x80, "Milo_123".encodeToByteArray())

        assertEquals(SingleDoseGrinder("Milo_123", true), state.singleDoseGrinder)
        assertEquals(42f, state.weight)
        assertEquals("BOOKOO_Test", state.smartScale?.name)
    }

    @Test
    fun `when type zero status arrives then its connection is restored without relying on name prefix`() {
        receive(0x8B, byteArrayOf(0, 1) + "Custom grinder".encodeToByteArray())

        assertEquals("Custom grinder", state.singleDoseGrinder?.name)
        assertTrue(found.single().isConnected)
    }

    @Test
    fun `when other accessory status arrives then grinder remains connected`() {
        state = state.copy(singleDoseGrinder = SingleDoseGrinder("Milo_123", true))

        receive(0x8B, byteArrayOf(1, 0))
        receive(0x8B, byteArrayOf(2, 0))

        assertEquals("Milo_123", state.singleDoseGrinder?.name)
        assertTrue(found.isEmpty())
    }

    @Test
    fun `when history repeats connected grinder then it remains connected`() {
        state = state.copy(singleDoseGrinder = SingleDoseGrinder("Milo_123", true))

        receive(0x8C, byteArrayOf(0, 8) + "Milo_123".encodeToByteArray())

        assertEquals(SingleDoseGrinder("Milo_123", true), found.single())
    }

    @Test
    fun `when history has wrong type or truncated name then it is ignored`() {
        receive(0x8C, byteArrayOf(1, 8) + "Milo_123".encodeToByteArray())
        receive(0x8C, byteArrayOf(0, 20) + "Milo_123".encodeToByteArray())

        assertTrue(found.isEmpty())
    }

    @Test
    fun `when another device disconnects then the active grinder is preserved`() {
        state = state.copy(singleDoseGrinder = SingleDoseGrinder("Milo_123", true))

        receive(0x88, "BGM_other".encodeToByteArray())

        assertEquals("Milo_123", state.singleDoseGrinder?.name)
    }

    @Test
    fun `when active grinder disconnects then scale is preserved`() {
        state = state.copy(singleDoseGrinder = SingleDoseGrinder("Milo_123", true))

        receive(0x88, "Milo_123".encodeToByteArray())

        assertNull(state.singleDoseGrinder)
        assertFalse(found.single().isConnected)
        assertEquals("BOOKOO_Test", state.smartScale?.name)
    }

    @Test
    fun `when status reports grinder disconnected then stale grinder is cleared`() {
        state = state.copy(singleDoseGrinder = SingleDoseGrinder("Milo_123", true))

        receive(0x8B, byteArrayOf(0, 0))

        assertNull(state.singleDoseGrinder)
        assertFalse(found.single().isConnected)
    }

    @Test
    fun `when shared scan starts then only enabled categories show searching`() {
        state = state.copy(smartScaleEnabled = false)

        receive(0x83, byteArrayOf(1))

        assertTrue(state.singleDoseGrinderSearchActive)
        assertFalse(state.smartScaleSearchActive)
    }

    @Test
    fun `when shared scan stops then both searching indicators stop`() {
        state = state.copy(singleDoseGrinderSearchActive = true, smartScaleSearchActive = true)

        receive(0x83, byteArrayOf(0))

        assertFalse(state.singleDoseGrinderSearchActive)
        assertFalse(state.smartScaleSearchActive)
    }

    @Test
    fun `when grinder integration is disabled then its connection clears and scale remains active`() {
        state = state.copy(singleDoseGrinder = SingleDoseGrinder("Milo_123", true))

        receive(0x9A, byteArrayOf(6))

        assertFalse(state.singleDoseGrinderEnabled)
        assertNull(state.singleDoseGrinder)
        assertTrue(state.smartScaleEnabled)
        assertEquals(42f, state.weight)
    }

    @Test
    fun `when enabling grinder then all other integration bits are preserved`() {
        val frame = buildIntegrationConnectivityFrame(6, SINGLE_DOSE_ENABLED_MASK, true)

        assertContentEquals("ff55ffff9a000107f5".decodeHex(), frame)
    }

    @Test
    fun `when disabling grinder then scale and commercial integration bits are preserved`() {
        val frame = buildIntegrationConnectivityFrame(7, SINGLE_DOSE_ENABLED_MASK, false)

        assertContentEquals("ff55ffff9a000106f4".decodeHex(), frame)
    }

    private fun receive(command: Int, payload: ByteArray) {
        parser.handleIncomingFrame(buildControlFrame(command, payload), "CTRL")
    }
}
