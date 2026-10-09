package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceState
import app.geeflow.domain.device.ProfileExecutionCoordinator
import app.geeflow.domain.device.ProfileRunState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class StopBrewingUseCaseTest {
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
    fun `when brew status is Manual then stops matching brew mode`() = runTest {
        state.value = state.value.copy(brewStatus = DeviceState.BrewStatus.Manual)
        val useCase = StopBrewingUseCase(provider, coordinator)

        useCase(9)

        coVerify(exactly = 1) { controller.stopManualBrewing() }
    }

    @Test
    fun `when brew status is Profile then stops matching brew mode`() = runTest {
        state.value = state.value.copy(brewStatus = DeviceState.BrewStatus.Profile)
        val useCase = StopBrewingUseCase(provider, coordinator)

        useCase(9)

        coVerify(exactly = 1) { controller.stopProfileBrewing() }
    }

    @Test
    fun `when brew status is FreeVariable then stops matching brew mode`() = runTest {
        state.value = state.value.copy(brewStatus = DeviceState.BrewStatus.FreeVariable)
        val useCase = StopBrewingUseCase(provider, coordinator)

        useCase(9)

        coVerify(exactly = 1) { controller.stopFreeVariableBrewing() }
    }

    @Test
    fun `when coordinator owns brew then delegates stop to coordinator`() = runTest {
        coEvery { coordinator.stop(9) } returns true
        val useCase = StopBrewingUseCase(provider, coordinator)

        useCase(9)

        coVerify(exactly = 0) { controller.stopManualBrewing() }
        coVerify(exactly = 0) { controller.stopProfileBrewing() }
        coVerify(exactly = 0) { controller.stopFreeVariableBrewing() }
    }
}
