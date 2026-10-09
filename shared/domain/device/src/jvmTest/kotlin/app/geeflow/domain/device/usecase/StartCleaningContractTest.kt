package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.MaintenanceSettingsRepository
import app.geeflow.data.device.model.CleaningProgram
import app.geeflow.data.device.model.CleaningType
import app.geeflow.data.device.model.DeviceCapability
import app.geeflow.data.device.model.DeviceConstraints
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.MaintenanceSettings
import app.geeflow.domain.device.ProfileExecutionCoordinator
import app.geeflow.domain.device.ProfileRunState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class StartCleaningContractTest {
    private val provider = mockk<DeviceControllerProvider>(relaxUnitFun = true)
    private val controller = mockk<DeviceController>(relaxUnitFun = true)
    private val maintenance = mockk<MaintenanceSettingsRepository>(relaxUnitFun = true)
    private val coordinator = mockk<ProfileExecutionCoordinator>(relaxUnitFun = true)
    private val state = MutableStateFlow(DeviceState(connectionStatus = DeviceState.ConnectionStatus.Connected))
    private val constraints = DeviceConstraints(80..100, 120..140, 1..10, 1..60, 1..30, 1..30, 1..10, 0f..12f, 0f..10f)

    init {
        every { provider.getController(9) } returns controller
        every { controller.deviceState } returns state
        every { controller.constraints } returns constraints
        every { coordinator.state } returns MutableStateFlow(ProfileRunState())
        coEvery { coordinator.withUnownedControl<Unit>(9, any()) } coAnswers {
            secondArg<suspend () -> Unit>().invoke()
        }
    }

    @Test
    fun `when daily cleaning starts then postpones daily reminder before hardware commands`() = runTest {
        val settings = MaintenanceSettings(daily = CleaningProgram(5, 3, 3), deep = CleaningProgram(5, 3, 6))
        every { maintenance.observe(9) } returns flowOf(settings)
        every {
            controller.capabilities
        } returns setOf(DeviceCapability.CleaningMode, DeviceCapability.CleaningSettings)
        val useCase = StartCleaningUseCase(coordinator, maintenance, provider)

        useCase(9, CleaningType.Daily)

        coVerifyOrder {
            maintenance.postpone(9, setOf(CleaningType.Daily), any())
            controller.setCleaningSettings(
                settings.daily.flushSeconds.toFloat(),
                settings.daily.restSeconds.toFloat(),
                settings.daily.cycles
            )
            controller.startCleaning()
        }
    }

    @Test
    fun `when deep cleaning starts then postpones both reminders before hardware commands`() = runTest {
        val settings = MaintenanceSettings(daily = CleaningProgram(5, 3, 3), deep = CleaningProgram(5, 3, 6))
        every { maintenance.observe(9) } returns flowOf(settings)
        every {
            controller.capabilities
        } returns setOf(DeviceCapability.CleaningMode, DeviceCapability.CleaningSettings)
        val useCase = StartCleaningUseCase(coordinator, maintenance, provider)

        useCase(9, CleaningType.Deep)

        coVerifyOrder {
            maintenance.postpone(9, CleaningType.entries.toSet(), any())
            controller.setCleaningSettings(
                settings.deep.flushSeconds.toFloat(),
                settings.deep.restSeconds.toFloat(),
                settings.deep.cycles
            )
            controller.startCleaning()
        }
    }

    @Test
    fun `when cleaning is already running then rejects hardware commands`() = runTest {
        val settings = MaintenanceSettings(daily = CleaningProgram(5, 3, 3), deep = CleaningProgram(5, 3, 6))
        every { maintenance.observe(9) } returns flowOf(settings)
        every {
            controller.capabilities
        } returns setOf(DeviceCapability.CleaningMode, DeviceCapability.CleaningSettings)
        val useCase = StartCleaningUseCase(coordinator, maintenance, provider)
        state.value = state.value.copy(brewStatus = DeviceState.BrewStatus.Cleaning)

        val result = runCatching { useCase(9) }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
        coVerify(exactly = 0) { controller.startCleaning() }
    }
}
