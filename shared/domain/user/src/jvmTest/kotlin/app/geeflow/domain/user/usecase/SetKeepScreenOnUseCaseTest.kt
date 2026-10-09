package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.data.user.model.User
import io.mockk.Called
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SetKeepScreenOnUseCaseTest {
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val settings = mockk<UserSettingsRepository>(relaxUnitFun = true)
    private val selected = MutableStateFlow<User?>(User(id = 7, name = "Ada"))

    init {
        every { users.selectedUser } returns selected
    }

    @Test
    fun `when screen is enabled then leaves screensaver unchanged`() = runTest {
        val useCase = SetKeepScreenOnUseCase(settings, users)

        useCase(true)

        coVerify(exactly = 1) { settings.setKeepScreenOn(7, true) }
        coVerify(exactly = 0) { settings.setScreensaverEnabled(any(), any()) }
    }

    @Test
    fun `when no user is selected then does not save settings`() = runTest {
        val useCase = SetKeepScreenOnUseCase(settings, users)
        selected.value = null

        useCase(true)

        coVerify { settings wasNot Called }
    }

    @Test
    fun `when screen is disabled then also disables screensaver`() = runTest {
        val useCase = SetKeepScreenOnUseCase(settings, users)

        useCase(false)

        coVerifyOrder {
            settings.setKeepScreenOn(7, false)
            settings.setScreensaverEnabled(7, false)
        }
    }
}
