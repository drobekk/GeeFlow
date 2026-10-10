package app.geeflow.domain.device.usecase

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.MaintenanceSettingsRepository
import app.geeflow.data.device.model.DeviceError
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.user.UserRepository
import app.geeflow.domain.device.ProfileExecutionCoordinator
import app.geeflow.domain.device.ProfileExecutionRecorder
import app.geeflow.domain.exception.DeviceErrorException
import app.geeflow.domain.exception.DeviceNotConnectedException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DeviceErrorGuardTest {
    private val controller = mockk<DeviceController>(relaxUnitFun = true)
    private val provider = mockk<DeviceControllerProvider>()
    private val maintenance = mockk<MaintenanceSettingsRepository>(relaxUnitFun = true)
    private val users = mockk<UserRepository>()
    private val coordinator = mockk<ProfileExecutionCoordinator>(relaxUnitFun = true)
    private val state = MutableStateFlow(DeviceState(connectionStatus = DeviceState.ConnectionStatus.Connected))
    private val profile = BrewProfile(
        userId = 1,
        name = "Shot",
        description = "",
        finishCondition = null,
        steps = listOf(ProfileStep.Pressure(30, 9f)),
    )

    init {
        every { controller.deviceState } returns state
        every { provider.getController(9) } returns controller
        coEvery { coordinator.withUnownedControl<Unit>(9, any()) } coAnswers {
            secondArg<suspend () -> Unit>().invoke()
        }
    }

    @Test
    fun `when a known or unknown fault is active then rejects starts and live targets without side effects`() = runTest {
        val operations: List<suspend () -> Unit> = listOf(
            { StartManualBrewingUseCase(coordinator, provider)(9) },
            { StartFreeVariableBrewingUseCase(coordinator, provider)(9, true) },
            { StartFreeVariableBrewingUseCase(coordinator, provider)(9, false) },
            { StartProfileBrewingUseCase(provider, users, coordinator)(9, profile) },
            { StartCleaningUseCase(coordinator, maintenance, provider)(9) },
            { SetFreeBrewFlowUseCase(coordinator, provider)(9, 2f) },
            { SetFreeBrewPressureUseCase(coordinator, provider)(9, 9f) },
        )

        for (code in (1..8) + 258) {
            state.value = state.value.copy(error = DeviceError(code))

            for (operation in operations) {
                val result = runCatching { operation() }

                val error = assertIs<DeviceErrorException>(result.exceptionOrNull())
                assertEquals(9L, error.deviceId)
                assertEquals(code, error.errorCode)
            }
        }

        coVerify(exactly = 0) {
            controller.startManualBrewing()
            controller.startFreeVariableBrewing(any())
            coordinator.start(any(), any())
            controller.setCleaningSettings(any(), any(), any())
            controller.startCleaning()
            controller.setFreeBrewFlowTarget(any())
            controller.setFreeBrewPressureTarget(any())
            maintenance.postpone(any(), any(), any())
        }
    }

    @Test
    fun `when legacy water alarm is cleared then allows previously blocked operation`() = runTest {
        state.value = state.value.copy(waterLevelAlarm = true)

        val result = runCatching { StartManualBrewingUseCase(coordinator, provider)(9) }

        assertIs<DeviceErrorException>(result.exceptionOrNull())

        state.value = state.value.copy(waterLevelAlarm = false)

        StartManualBrewingUseCase(coordinator, provider)(9)

        coVerify(exactly = 1) { controller.startManualBrewing() }
    }

    @Test
    fun `when device is disconnected and has a fault then connection error takes precedence`() {
        state.value = state.value.copy(
            connectionStatus = DeviceState.ConnectionStatus.Disconnected,
            error = DeviceError(8),
        )

        val result = runCatching { controller.requireReady(9) }

        assertIs<DeviceNotConnectedException>(result.exceptionOrNull())
    }

    @Test
    fun `when a fault is active then stop commands remain available`() = runTest {
        state.value = state.value.copy(error = DeviceError(6))
        coEvery { coordinator.stop(9) } returns false

        StopBrewingUseCase(provider, coordinator)(9)
        StopCleaningUseCase(provider)(9)
        StopFreeVariableBrewingUseCase(coordinator, provider)(9)

        coVerify(exactly = 1) {
            controller.stopManualBrewing()
            controller.stopCleaning()
            controller.stopFreeVariableBrewing()
        }
    }

    @Test
    fun `when a fault is active then direct profile coordinator start rejects it`() = runTest {
        val execution = ProfileExecutionCoordinator(provider, backgroundScope, mockk<ProfileExecutionRecorder>())
        state.value = state.value.copy(error = DeviceError(258))

        val result = runCatching { execution.start(9, profile) }

        assertIs<DeviceErrorException>(result.exceptionOrNull())
        coVerify(exactly = 0) {
            controller.startProfileBrewing(any())
            controller.openLiveSession(any())
        }
    }
}
