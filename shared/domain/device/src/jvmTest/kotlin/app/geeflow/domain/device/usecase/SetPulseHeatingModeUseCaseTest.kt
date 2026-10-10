package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceError
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.DeviceState.HeatingMode
import app.geeflow.domain.exception.DeviceNotConnectedException
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class SetPulseHeatingModeUseCaseTest {
    private val provider = mockk<DeviceControllerProvider>(relaxUnitFun = true)
    private val controller = mockk<DeviceController>(relaxUnitFun = true)
    private val state = MutableStateFlow(DeviceState(connectionStatus = DeviceState.ConnectionStatus.Connected))

    init {
        every { provider.getController(9) } returns controller
        every { controller.deviceState } returns state
    }

    @Test
    fun `when a fault is active then allows changing both heating modes`() = runTest {
        state.value = state.value.copy(error = DeviceError(7))
        val useCase = SetPulseHeatingModeUseCase(provider)

        useCase(9, true)
        useCase(9, false)

        coVerifyOrder {
            controller.setHeatingMode(HeatingMode.Pulse)
            controller.setHeatingMode(HeatingMode.FullSpeed)
        }
    }

    @Test
    fun `when device is connected then sends requested command`() = runTest {
        val useCase = SetPulseHeatingModeUseCase(provider)

        useCase(9, true)

        coVerifyOrder {
            controller.setHeatingMode(HeatingMode.Pulse)
        }
    }

    @Test
    fun `when device is disconnected then rejects command`() = runTest {
        val useCase = SetPulseHeatingModeUseCase(provider)
        state.value = state.value.copy(connectionStatus = DeviceState.ConnectionStatus.Disconnected)

        val result = runCatching { useCase(9, true) }

        assertTrue(result.exceptionOrNull() is DeviceNotConnectedException)
        coVerify(exactly = 0) { controller.setHeatingMode(HeatingMode.Pulse) }
    }

    @Test
    fun `when alternative mode is requested then sends alternative command`() = runTest {
        val useCase = SetPulseHeatingModeUseCase(provider)

        useCase(9, false)

        coVerify(exactly = 1) { controller.setHeatingMode(HeatingMode.FullSpeed) }
    }
}
