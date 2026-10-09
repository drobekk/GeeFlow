package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceConstraints
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.data.user.model.TemperatureUnit
import app.geeflow.data.user.model.User
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetDeviceConstraintsUseCaseTest {
    private val provider = mockk<DeviceControllerProvider>(relaxUnitFun = true)
    private val controller = mockk<DeviceController>(relaxUnitFun = true)
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val settings = mockk<UserSettingsRepository>(relaxUnitFun = true)
    private val selected = MutableStateFlow<User?>(User(id = 7, name = "Ada"))
    private val constraints = DeviceConstraints(80..100, 120..140, 1..10, 1..60, 1..30, 1..30, 1..10, 0f..12f, 0f..10f)

    init {
        every { provider.getController(9) } returns controller
        every { controller.constraints } returns constraints
        every { users.selectedUser } returns selected
        every { settings.temperatureUnit(7) } returns flowOf(TemperatureUnit.CELSIUS)
    }

    @Test
    fun `when unit is CELSIUS then returns original ranges`() = runTest {
        every { settings.temperatureUnit(7) } returns flowOf(TemperatureUnit.CELSIUS)
        val useCase = GetDeviceConstraintsUseCase(provider, settings, users)

        val result = useCase(9)

        assertEquals(80..100, result.brewTempRange)
        assertEquals(120..140, result.steamTempRange)
        assertEquals(constraints.pressureRange, result.pressureRange)
    }

    @Test
    fun `when unit is FAHRENHEIT then returns converted ranges`() = runTest {
        every { settings.temperatureUnit(7) } returns flowOf(TemperatureUnit.FAHRENHEIT)
        val useCase = GetDeviceConstraintsUseCase(provider, settings, users)

        val result = useCase(9)

        assertEquals(176..212, result.brewTempRange)
        assertEquals(248..284, result.steamTempRange)
        assertEquals(constraints.pressureRange, result.pressureRange)
    }

    @Test
    fun `when no user is selected then returns original constraints`() = runTest {
        selected.value = null
        val useCase = GetDeviceConstraintsUseCase(provider, settings, users)

        val result = useCase(9)

        assertEquals(constraints, result)
        verify { settings wasNot Called }
    }
}
