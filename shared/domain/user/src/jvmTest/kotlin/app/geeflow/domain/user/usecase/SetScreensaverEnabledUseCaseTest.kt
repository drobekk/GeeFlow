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

class SetScreensaverEnabledUseCaseTest {
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val settings = mockk<UserSettingsRepository>(relaxUnitFun = true)
    private val selected = MutableStateFlow<User?>(User(id = 7, name = "Ada"))

    init {
        every { users.selectedUser } returns selected
    }

    @Test
    fun `when screensaver is enabled then also keeps screen on`() = runTest {
        val useCase = SetScreensaverEnabledUseCase(settings, users)

        useCase(true)

        coVerifyOrder {
            settings.setScreensaverEnabled(7, true)
            settings.setKeepScreenOn(7, true)
        }
    }

    @Test
    fun `when no user is selected then does not save settings`() = runTest {
        val useCase = SetScreensaverEnabledUseCase(settings, users)
        selected.value = null

        useCase(true)

        coVerify { settings wasNot Called }
    }

    @Test
    fun `when screensaver is disabled then leaves screen setting unchanged`() = runTest {
        val useCase = SetScreensaverEnabledUseCase(settings, users)

        useCase(false)

        coVerify(exactly = 1) { settings.setScreensaverEnabled(7, false) }
        coVerify(exactly = 0) { settings.setKeepScreenOn(any(), any()) }
    }
}
