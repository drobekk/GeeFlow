package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.DeviceState.BoilerType
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.data.user.model.TemperatureUnit
import app.geeflow.data.user.model.User
import io.mockk.Called
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SetBoilerSettingsUseCaseTest {
    private val provider = mockk<DeviceControllerProvider>(relaxUnitFun = true)
    private val controller = mockk<DeviceController>(relaxUnitFun = true)
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val settings = mockk<UserSettingsRepository>(relaxUnitFun = true)
    private val state = MutableStateFlow(DeviceState(connectionStatus = DeviceState.ConnectionStatus.Connected))
    private val selected = MutableStateFlow<User?>(User(id = 7, name = "Ada"))

    init {
        every { provider.getController(9) } returns controller
        every { controller.deviceState } returns state
        every { users.selectedUser } returns selected
        every { settings.temperatureUnit(7) } returns flowOf(TemperatureUnit.CELSIUS)
    }

    @Test
    fun `when Brew boiler uses CELSIUS then writes Celsius temperature`() = runTest {
        every { settings.temperatureUnit(7) } returns flowOf(TemperatureUnit.CELSIUS)
        val useCase = SetBoilerSettingsUseCase(provider, settings, users)

        useCase(9, BoilerType.Brew, true, 93)

        coVerifyOrder {
            controller.setBoilerState(BoilerType.Brew, true)
            controller.setBrewTemperature(93)
        }
    }

    @Test
    fun `when Brew boiler uses FAHRENHEIT then writes Celsius temperature`() = runTest {
        every { settings.temperatureUnit(7) } returns flowOf(TemperatureUnit.FAHRENHEIT)
        val useCase = SetBoilerSettingsUseCase(provider, settings, users)

        useCase(9, BoilerType.Brew, true, 212)

        coVerifyOrder {
            controller.setBoilerState(BoilerType.Brew, true)
            controller.setBrewTemperature(100)
        }
    }

    @Test
    fun `when Steam boiler uses FAHRENHEIT then writes Celsius temperature`() = runTest {
        every { settings.temperatureUnit(7) } returns flowOf(TemperatureUnit.FAHRENHEIT)
        val useCase = SetBoilerSettingsUseCase(provider, settings, users)

        useCase(9, BoilerType.Steam, true, 266)

        coVerifyOrder {
            controller.setBoilerState(BoilerType.Steam, true)
            controller.setSteamTemperature(130)
        }
    }

    @Test
    fun `when no user is selected then does not write to device`() = runTest {
        selected.value = null
        val useCase = SetBoilerSettingsUseCase(provider, settings, users)

        useCase(9, BoilerType.Brew, true, 93)

        coVerify { controller wasNot Called }
    }
}
