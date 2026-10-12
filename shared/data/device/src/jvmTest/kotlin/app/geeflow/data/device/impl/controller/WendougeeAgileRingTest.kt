package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.ble.modbus.ModbusSession
import app.geeflow.data.device.model.DeviceState
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class WendougeeAgileRingTest {
    private val owner = mockk<WendougeeController>(relaxed = true)
    private val modbus = mockk<ModbusSession>()
    private val config = mockk<DeviceState.Config>()
    private val state = MutableStateFlow(DeviceState(config = config, agileRingSupported = true))
    private val control = WendougeeSettingsControl(owner)

    init {
        every { config.agileRingEnabled } returns false
        every { owner.mutableState } returns state
        every { owner.operationMutex } returns Mutex()
        every { owner.requireSession() } returns WendougeeSession(mockk(), modbus, mockk())
    }

    @Test
    fun `when enabling ring then writes native register before updating state`() = runTest {
        coEvery { modbus.writeMultipleRegisters(28, listOf(1), any()) } returns Unit

        control.setAgileRing(true)

        coVerifyOrder {
            modbus.writeMultipleRegisters(28, listOf(1), any())
            owner.updateState(any())
        }
    }

    @Test
    fun `when disabling ring then writes zero to native register`() = runTest {
        every { config.agileRingEnabled } returns true
        coEvery { modbus.writeMultipleRegisters(28, listOf(0), any()) } returns Unit

        control.setAgileRing(false)

        coVerifyOrder {
            modbus.writeMultipleRegisters(28, listOf(0), any())
            owner.updateState(any())
        }
    }

    @Test
    fun `when ring write fails then leaves confirmed state unchanged`() = runTest {
        coEvery { modbus.writeMultipleRegisters(any(), any(), any()) } throws IllegalStateException("No ACK")

        val result = runCatching { control.setAgileRing(true) }

        assertTrue(result.isFailure)
        verify(exactly = 0) { owner.updateState(any()) }
    }

    @Test
    fun `when firmware does not support switching then rejects without writing`() = runTest {
        state.value = state.value.copy(agileRingSupported = false)

        val result = runCatching { control.setAgileRing(true) }

        assertTrue(result.isFailure)
        verify(exactly = 0) { owner.requireSession() }
    }
}
