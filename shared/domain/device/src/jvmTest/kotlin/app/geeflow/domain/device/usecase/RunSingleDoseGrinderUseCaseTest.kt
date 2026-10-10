package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.SingleDoseGrinder
import app.geeflow.domain.exception.DeviceNotConnectedException
import app.geeflow.domain.exception.GrinderNotConnectedException
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs

class RunSingleDoseGrinderUseCaseTest {
    private val provider = mockk<DeviceControllerProvider>(relaxUnitFun = true)
    private val controller = mockk<DeviceController>(relaxUnitFun = true)
    private val state = MutableStateFlow(DeviceState(connectionStatus = DeviceState.ConnectionStatus.Connected))

    init {
        every { provider.getController(9) } returns controller
        every { controller.deviceState } returns state
    }

    @Test
    fun `when grinder is connected then sends grinding settings`() = runTest {
        val useCase = RunSingleDoseGrinderUseCase(provider)
        state.value = state.value.copy(singleDoseGrinder = SingleDoseGrinder("Milo", isConnected = true))

        useCase(9, grindingSize = 300, grindingSpeed = 600)

        coVerify(exactly = 1) { controller.runSingleDoseGrinder(300, 600) }
    }

    @Test
    fun `when grinder is absent then rejects command with business error`() = runTest {
        val useCase = RunSingleDoseGrinderUseCase(provider)

        val result = runCatching { useCase(9, grindingSize = 300, grindingSpeed = 600) }

        assertIs<GrinderNotConnectedException>(result.exceptionOrNull())
        coVerify(exactly = 0) { controller.runSingleDoseGrinder(any(), any()) }
    }

    @Test
    fun `when grinder is disconnected then rejects command with business error`() = runTest {
        val useCase = RunSingleDoseGrinderUseCase(provider)
        state.value = state.value.copy(singleDoseGrinder = SingleDoseGrinder("Milo", isConnected = false))

        val result = runCatching { useCase(9, grindingSize = 300, grindingSpeed = 600) }

        assertIs<GrinderNotConnectedException>(result.exceptionOrNull())
        coVerify(exactly = 0) { controller.runSingleDoseGrinder(any(), any()) }
    }

    @Test
    fun `when device is disconnected then rejects command before checking grinder`() = runTest {
        val useCase = RunSingleDoseGrinderUseCase(provider)
        state.value = state.value.copy(connectionStatus = DeviceState.ConnectionStatus.Disconnected)

        val result = runCatching { useCase(9, grindingSize = 300, grindingSpeed = 600) }

        assertIs<DeviceNotConnectedException>(result.exceptionOrNull())
        coVerify(exactly = 0) { controller.runSingleDoseGrinder(any(), any()) }
    }
}
