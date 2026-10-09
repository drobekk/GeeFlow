package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.data.user.model.TemperatureUnit
import app.geeflow.data.user.model.User
import app.geeflow.domain.user.model.BrewingPreferencesSettings
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetBrewingPreferencesUseCaseTest {
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val settings = mockk<UserSettingsRepository>(relaxUnitFun = true)
    private val selected = MutableStateFlow<User?>(User(id = 7, name = "Ada"))

    init {
        every { users.selectedUser } returns selected
    }

    @Test
    fun `when user is selected then combines settings for that user`() = runTest {
        every { settings.autoConnect(7) } returns flowOf(true)
        every { settings.temperatureUnit(7) } returns flowOf(TemperatureUnit.FAHRENHEIT)
        val useCase = GetBrewingPreferencesUseCase(settings, users)

        val result = useCase().first()

        assertEquals(BrewingPreferencesSettings(autoConnect = true, temperatureUnit = TemperatureUnit.FAHRENHEIT), result)
    }

    @Test
    fun `when selected user changes then observes new user settings`() = runTest {
        every { settings.autoConnect(7) } returns flowOf(true)
        every { settings.temperatureUnit(7) } returns flowOf(TemperatureUnit.FAHRENHEIT)
        val useCase = GetBrewingPreferencesUseCase(settings, users)
        selected.value = User(id = 8, name = "Grace")
        every { settings.autoConnect(8) } returns flowOf(true)
        every { settings.temperatureUnit(8) } returns flowOf(TemperatureUnit.FAHRENHEIT)

        val result = useCase().first()

        assertEquals(BrewingPreferencesSettings(autoConnect = true, temperatureUnit = TemperatureUnit.FAHRENHEIT), result)
        // MockK verifies this call without collecting its returned Flow.
        @Suppress("IgnoredReturnValue")
        verify(exactly = 0) { settings.autoConnect(7) }
    }
}
