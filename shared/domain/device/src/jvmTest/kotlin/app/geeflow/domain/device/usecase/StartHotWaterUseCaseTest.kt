package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceCapability
import app.geeflow.data.device.model.DeviceError
import app.geeflow.data.device.model.DeviceState
import app.geeflow.domain.device.ProfileExecutionCoordinator
import app.geeflow.domain.exception.DeviceErrorException
import app.geeflow.domain.exception.DeviceNotConnectedException
import app.geeflow.domain.exception.MachineBusyException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class StartHotWaterUseCaseTest {
    private val provider = mockk<DeviceControllerProvider>()
    private val controller = mockk<DeviceController>(relaxed = true)
    private val coordinator = mockk<ProfileExecutionCoordinator>()
    private val state = MutableStateFlow(DeviceState(connectionStatus = DeviceState.ConnectionStatus.Connected))
    private val useCase = StartHotWaterUseCase(coordinator, provider)

    init {
        every { provider.getController(9) } returns controller
        every { controller.deviceState } returns state
        every { controller.capabilities } returns setOf(DeviceCapability.HotWaterDispensing)
        coEvery { coordinator.withUnownedControl<Unit>(9, any()) } coAnswers {
            secondArg<suspend () -> Unit>().invoke()
        }
    }

    @Test
    fun `when supported device is ready then starts hot water`() = runTest {
        useCase(9)

        coVerify(exactly = 1) { controller.hotWater.start() }
    }

    @Test
    fun `when device has an alarm then rejects hot water`() = runTest {
        state.value = state.value.copy(error = DeviceError(1))

        val result = runCatching { useCase(9) }

        assertTrue(result.exceptionOrNull() is DeviceErrorException)
        coVerify(exactly = 0) { controller.hotWater.start() }
    }

    @Test
    fun `when device is busy then rejects hot water`() = runTest {
        state.value = state.value.copy(brewStatus = DeviceState.BrewStatus.Manual)

        val result = runCatching { useCase(9) }

        assertTrue(result.exceptionOrNull() is MachineBusyException)
        coVerify(exactly = 0) { controller.hotWater.start() }
    }

    @Test
    fun `when device is disconnected then rejects hot water`() = runTest {
        state.value = state.value.copy(connectionStatus = DeviceState.ConnectionStatus.Disconnected)

        val result = runCatching { useCase(9) }

        assertTrue(result.exceptionOrNull() is DeviceNotConnectedException)
        coVerify(exactly = 0) { controller.hotWater.start() }
    }

    @Test
    fun `when model has no hot water capability then does not send the command`() = runTest {
        every { controller.capabilities } returns emptySet()

        val result = runCatching { useCase(9) }

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { controller.hotWater.start() }
    }
}
