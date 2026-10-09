package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.MaintenanceSettingsRepository
import app.geeflow.data.device.model.CleaningProgram
import app.geeflow.data.device.model.DeviceConstraints
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.MaintenanceSettings
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveMaintenanceSettingsContractTest {
    private val provider = mockk<DeviceControllerProvider>(relaxUnitFun = true)
    private val controller = mockk<DeviceController>(relaxUnitFun = true)
    private val maintenance = mockk<MaintenanceSettingsRepository>(relaxUnitFun = true)
    private val state = MutableStateFlow(DeviceState(connectionStatus = DeviceState.ConnectionStatus.Connected))
    private val constraints = DeviceConstraints(80..100, 120..140, 1..10, 1..60, 1..30, 1..30, 1..10, 0f..12f, 0f..10f)

    init {
        every { provider.getController(9) } returns controller
        every { controller.deviceState } returns state
        every { controller.constraints } returns constraints
    }

    @Test
    fun `when settings already exist then emits stored settings without initialization`() = runTest {
        val expected = MaintenanceSettings(daily = CleaningProgram(5, 3, 3), deep = CleaningProgram(5, 3, 6))
        every { maintenance.observe(9) } returns flowOf(expected)
        val useCase = ObserveMaintenanceSettingsUseCase(maintenance, provider)

        val result = useCase(9).first()

        assertEquals(expected, result)
        coVerify(exactly = 0) { maintenance.initialize(any(), any()) }
    }
}
