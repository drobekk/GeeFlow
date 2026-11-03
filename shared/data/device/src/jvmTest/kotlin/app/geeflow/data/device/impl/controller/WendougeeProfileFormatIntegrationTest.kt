package app.geeflow.data.device.impl.controller

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.data.device.TimedPumpProfileSupport
import app.geeflow.data.device.ble.modbus.ModbusSession
import app.geeflow.data.device.model.DeviceState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs

class WendougeeProfileFormatIntegrationTest {
    @Test
    fun `when starting packed stages then uploads packed words before the existing start pulse`() = runTest {
        val modbus = mockk<ModbusSession>(relaxed = true)
        val session = session(modbus, WendougeeProfileFormat.Packed)
        val owner = owner(session)
        val control = WendougeeProfileControl(owner)

        control.startProfileBrewing(profile())

        coVerifyOrder {
            modbus.writeMultipleRegisters(2048, listOf(277, 40, 0, 0, 0, 0, 0, 0), any())
            modbus.writeMultipleRegisters(2056, listOf(60, 1, 0, 5, 0, 0, 0, 0, 0), any())
            modbus.writeSingleRegister(87, 2, any())
            owner.sendModbusPulse(150, any())
        }
    }

    @Test
    fun `when binding packed stages then uploads to the second bank before binding`() = runTest {
        val modbus = mockk<ModbusSession>(relaxed = true)
        val control = WendougeeProfileControl(owner(session(modbus, WendougeeProfileFormat.Packed)))

        control.bindProfile(profile())

        coVerifyOrder {
            modbus.writeMultipleRegisters(2560, listOf(277, 40, 0, 0, 0, 0, 0, 0), any())
            modbus.writeMultipleRegisters(2568, listOf(60, 1, 0, 5, 0, 0, 0, 0, 0), any())
            modbus.writeSingleRegister(88, 2, any())
            modbus.writeMultipleRegisters(WendougeeRegisters.BIND_PROFILE, listOf(1), any())
        }
    }

    @Test
    fun `when session is replaced then legacy upload does not retain the previous packed format`() = runTest {
        val modbus = mockk<ModbusSession>(relaxed = true)
        val owner = owner(session(modbus, WendougeeProfileFormat.Packed))
        val control = WendougeeProfileControl(owner)
        val replacement = WendougeeSession(mockk(), modbus, MutableSharedFlow())
        every { owner.requireSession() } returns replacement

        control.startProfileBrewing(profile())

        coVerify(exactly = 1) { modbus.writeMultipleRegisters(2048, listOf(1, 1, 1, 1, 40, 0, 0), any()) }
        coVerify(exactly = 0) { modbus.writeMultipleRegisters(2048, listOf(277, 40, 0, 0, 0, 0, 0, 0), any()) }
    }

    @Test
    fun `when packed finish target is invalid then sends neither registers nor start pulse`() = runTest {
        val modbus = mockk<ModbusSession>(relaxed = true)
        val owner = owner(session(modbus, WendougeeProfileFormat.Packed))
        val control = WendougeeProfileControl(owner)
        val invalid = profile().copy(finishCondition = Condition.Volume(Float.NaN))

        val result = runCatching { control.startProfileBrewing(invalid) }

        assertIs<IllegalArgumentException>(result.exceptionOrNull())
        coVerify(exactly = 0) { modbus.writeMultipleRegisters(any(), any(), any()) }
        coVerify(exactly = 0) { modbus.writeSingleRegister(any(), any(), any()) }
        coVerify(exactly = 0) { owner.sendModbusPulse(any(), any()) }
    }

    private fun session(modbus: ModbusSession, format: WendougeeProfileFormat) =
        WendougeeSession(mockk(), modbus, MutableSharedFlow()).apply { profileFormat = format }

    private fun owner(session: WendougeeSession): WendougeeController = mockk {
        every { operationMutex } returns Mutex()
        every { requireSession() } returns session
        every { deviceState } returns MutableStateFlow(DeviceState())
        every { profilingCapabilities } returns TimedPumpProfileSupport.capabilities
        coEvery { refreshBrewStatus(any()) } returns Unit
        coEvery { sendModbusPulse(any(), any()) } returns Unit
    }

    private fun profile() = BrewProfile(
        userId = 1,
        name = "Packed stages",
        description = "",
        finishCondition = Condition.Volume(40f),
        steps = listOf(ProfileStep.Pressure(5, 6f)),
    )
}
