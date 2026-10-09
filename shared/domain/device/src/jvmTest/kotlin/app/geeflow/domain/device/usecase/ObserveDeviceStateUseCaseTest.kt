package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.DeviceState.Config
import app.geeflow.data.device.model.DeviceState.HeatingMode
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.data.user.model.TemperatureUnit
import app.geeflow.data.user.model.User
import app.geeflow.domain.device.ProfileExecutionCoordinator
import app.geeflow.domain.device.ProfileRunState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveDeviceStateUseCaseTest {
    private val provider = mockk<DeviceControllerProvider>(relaxUnitFun = true)
    private val controller = mockk<DeviceController>(relaxUnitFun = true)
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val settings = mockk<UserSettingsRepository>(relaxUnitFun = true)
    private val coordinator = mockk<ProfileExecutionCoordinator>(relaxUnitFun = true)
    private val state = MutableStateFlow(DeviceState(connectionStatus = DeviceState.ConnectionStatus.Connected))
    private val selected = MutableStateFlow<User?>(User(id = 7, name = "Ada"))
    private val config = Config(130f, 93f, true, true, 30f, 9f, 5f, 3f, 3, HeatingMode.FullSpeed, true)

    init {
        every { provider.getController(9) } returns controller
        every { controller.deviceState } returns state
        every { users.selectedUser } returns selected
        every { settings.temperatureUnit(7) } returns flowOf(TemperatureUnit.CELSIUS)
        every { coordinator.state } returns MutableStateFlow(ProfileRunState())
    }

    @Test
    fun `when temperature unit is CELSIUS then converts telemetry and targets`() = runTest {
        state.value = state.value.copy(brewBoilerTemp = 100f, steamBoilerTemp = 130f, config = config)
        every { settings.temperatureUnit(7) } returns flowOf(TemperatureUnit.CELSIUS)
        val useCase = ObserveDeviceStateUseCase(coordinator, provider, settings, users)

        val result = useCase(9).first()

        assertEquals(100f, result.brewBoilerTemp)
        assertEquals(130f, result.steamBoilerTemp)
        assertEquals(93f, result.config?.targetBrewTemp)
    }

    @Test
    fun `when temperature unit is FAHRENHEIT then converts telemetry and targets`() = runTest {
        state.value = state.value.copy(brewBoilerTemp = 100f, steamBoilerTemp = 130f, config = config)
        every { settings.temperatureUnit(7) } returns flowOf(TemperatureUnit.FAHRENHEIT)
        val useCase = ObserveDeviceStateUseCase(coordinator, provider, settings, users)

        val result = useCase(9).first()

        assertEquals(212f, result.brewBoilerTemp)
        assertEquals(266f, result.steamBoilerTemp)
        assertEquals(199.4f, result.config?.targetBrewTemp)
    }
}
