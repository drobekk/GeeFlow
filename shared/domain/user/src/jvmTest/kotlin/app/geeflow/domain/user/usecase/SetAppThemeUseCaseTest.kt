package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.data.user.model.AppTheme
import app.geeflow.data.user.model.User
import io.mockk.Called
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SetAppThemeUseCaseTest {
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val settings = mockk<UserSettingsRepository>(relaxUnitFun = true)
    private val selected = MutableStateFlow<User?>(User(id = 7, name = "Ada"))

    init {
        every { users.selectedUser } returns selected
    }

    @Test
    fun `when user is selected then saves setting for that user`() = runTest {
        val useCase = SetAppThemeUseCase(settings, users)

        useCase(AppTheme.CUSTOM)

        coVerify(exactly = 1) { settings.setAppTheme(7, AppTheme.CUSTOM) }
    }

    @Test
    fun `when no user is selected then does not save settings`() = runTest {
        val useCase = SetAppThemeUseCase(settings, users)
        selected.value = null

        useCase(AppTheme.CUSTOM)

        coVerify { settings wasNot Called }
    }
}
