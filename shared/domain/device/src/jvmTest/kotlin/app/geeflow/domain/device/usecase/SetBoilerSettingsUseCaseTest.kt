package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceError
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.DeviceState.BoilerType
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.data.user.model.TemperatureUnit
import app.geeflow.data.user.model.User
import app.geeflow.domain.exception.DeviceNotConnectedException
import io.mockk.Called
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs

class SetBoilerSettingsUseCaseTest {
    private val provider = mockk<DeviceControllerProvider>(relaxUnitFun = true)
    private val controller = mockk<DeviceController>(relaxed = true)
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
    fun `when a fault is active then allows toggling both boilers and changing their temperatures`() = runTest {
        state.value = state.value.copy(error = DeviceError(7))
        val useCase = SetBoilerSettingsUseCase(provider, settings, users)

        for (boiler in BoilerType.entries) {
            val temperature = if (boiler == BoilerType.Brew) 93 else 130

            useCase(9, boiler, true, temperature)
            useCase(9, boiler, false, temperature - 1)

            coVerifyOrder {
                controller.settings.setBoilerState(boiler, true)
                if (boiler == BoilerType.Brew) {
                    controller.settings.setBrewTemperature(temperature)
                } else {
                    controller.settings.setSteamTemperature(temperature)
                }
                controller.settings.setBoilerState(boiler, false)
                if (boiler == BoilerType.Brew) {
                    controller.settings.setBrewTemperature(temperature - 1)
                } else {
                    controller.settings.setSteamTemperature(temperature - 1)
                }
            }
        }
    }

    @Test
    fun `when device is disconnected then rejects boiler changes`() = runTest {
        state.value = state.value.copy(connectionStatus = DeviceState.ConnectionStatus.Disconnected)
        val useCase = SetBoilerSettingsUseCase(provider, settings, users)

        val result = runCatching { useCase(9, BoilerType.Brew, true, 93) }

        assertIs<DeviceNotConnectedException>(result.exceptionOrNull())
        coVerify(exactly = 0) {
            controller.settings.setBoilerState(any(), any())
            controller.settings.setBrewTemperature(any())
            controller.settings.setSteamTemperature(any())
        }
    }

    @Test
    fun `when Brew boiler uses CELSIUS then writes Celsius temperature`() = runTest {
        every { settings.temperatureUnit(7) } returns flowOf(TemperatureUnit.CELSIUS)
        val useCase = SetBoilerSettingsUseCase(provider, settings, users)

        useCase(9, BoilerType.Brew, true, 93)

        coVerifyOrder {
            controller.settings.setBoilerState(BoilerType.Brew, true)
            controller.settings.setBrewTemperature(93)
        }
    }

    @Test
    fun `when Brew boiler uses FAHRENHEIT then writes Celsius temperature`() = runTest {
        every { settings.temperatureUnit(7) } returns flowOf(TemperatureUnit.FAHRENHEIT)
        val useCase = SetBoilerSettingsUseCase(provider, settings, users)

        useCase(9, BoilerType.Brew, true, 212)

        coVerifyOrder {
            controller.settings.setBoilerState(BoilerType.Brew, true)
            controller.settings.setBrewTemperature(100)
        }
    }

    @Test
    fun `when Steam boiler uses FAHRENHEIT then writes Celsius temperature`() = runTest {
        every { settings.temperatureUnit(7) } returns flowOf(TemperatureUnit.FAHRENHEIT)
        val useCase = SetBoilerSettingsUseCase(provider, settings, users)

        useCase(9, BoilerType.Steam, true, 266)

        coVerifyOrder {
            controller.settings.setBoilerState(BoilerType.Steam, true)
            controller.settings.setSteamTemperature(130)
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
