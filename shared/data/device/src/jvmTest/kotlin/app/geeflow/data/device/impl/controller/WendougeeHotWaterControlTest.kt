package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.ble.modbus.ModbusSession
import app.geeflow.data.device.model.DeviceCapability
import app.geeflow.data.device.model.DeviceState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class WendougeeHotWaterControlTest {
    @Test
    fun `when starting and stopping hot water then sends original FC06 frames without diagnostic commands`() = runTest {
        val writes = mutableListOf<ByteArray>()
        val responses = MutableSharedFlow<ByteArray>()
        val modbus = ModbusSession(responses, { frame ->
            writes.add(frame)
            responses.emit(frame)
        }, 1, 1.seconds)
        val active = WendougeeSession(mockk(), modbus, responses)
        val owner = mockk<WendougeeController>()
        every { owner.operationMutex } returns Mutex()
        every { owner.requireSession() } returns active
        every { owner.capabilities } returns setOf(DeviceCapability.HotWaterDispensing)
        every { owner.mutableState } returns MutableStateFlow(DeviceState())
        coEvery { owner.refreshBrewStatus(active) } returns Unit
        val control = WendougeeHotWaterControl(owner)

        control.start()
        control.stop()
        val frames = writes.map { toHexString(it).uppercase() }

        assertEquals(listOf("0106065F00017890", "0106065F0000B950"), frames)
    }

    @Test
    fun `when refreshed machine is busy then does not start hot water`() = runTest {
        val owner = mockk<WendougeeController>()
        val active = mockk<WendougeeSession>()
        val state = MutableStateFlow(DeviceState())
        every { owner.operationMutex } returns Mutex()
        every { owner.capabilities } returns setOf(DeviceCapability.HotWaterDispensing)
        every { owner.requireSession() } returns active
        every { owner.mutableState } returns state
        coEvery { owner.refreshBrewStatus(active) } answers {
            state.value = state.value.copy(brewStatus = DeviceState.BrewStatus.Manual)
        }
        val control = WendougeeHotWaterControl(owner)

        val result = runCatching { control.start() }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
