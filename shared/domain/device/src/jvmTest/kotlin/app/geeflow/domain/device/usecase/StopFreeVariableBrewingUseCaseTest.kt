package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceState
import app.geeflow.domain.device.ProfileExecutionCoordinator
import app.geeflow.domain.device.ProfileRunState
import app.geeflow.domain.exception.DeviceNotConnectedException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class StopFreeVariableBrewingUseCaseTest {
    private val provider = mockk<DeviceControllerProvider>(relaxUnitFun = true)
    private val controller = mockk<DeviceController>(relaxUnitFun = true)
    private val coordinator = mockk<ProfileExecutionCoordinator>(relaxUnitFun = true)
    private val state = MutableStateFlow(DeviceState(connectionStatus = DeviceState.ConnectionStatus.Connected))

    init {
        every { provider.getController(9) } returns controller
        every { controller.deviceState } returns state
        every { coordinator.state } returns MutableStateFlow(ProfileRunState())
        coEvery { coordinator.stop(9) } returns false
    }

    @Test
    fun `when device is connected then sends requested command`() = runTest {
        val useCase = StopFreeVariableBrewingUseCase(coordinator, provider)

        useCase(9)

        coVerifyOrder {
            controller.stopFreeVariableBrewing()
        }
    }

    @Test
    fun `when device is disconnected then rejects command`() = runTest {
        val useCase = StopFreeVariableBrewingUseCase(coordinator, provider)
        state.value = state.value.copy(connectionStatus = DeviceState.ConnectionStatus.Disconnected)

        val result = runCatching { useCase(9) }

        assertTrue(result.exceptionOrNull() is DeviceNotConnectedException)
        coVerify(exactly = 0) { controller.stopFreeVariableBrewing() }
    }

    @Test
    fun `when coordinator stops profile then does not send free variable stop`() = runTest {
        val useCase = StopFreeVariableBrewingUseCase(coordinator, provider)
        coEvery { coordinator.stop(9) } returns true

        useCase(9)

        coVerify(exactly = 0) { controller.stopFreeVariableBrewing() }
    }
}
