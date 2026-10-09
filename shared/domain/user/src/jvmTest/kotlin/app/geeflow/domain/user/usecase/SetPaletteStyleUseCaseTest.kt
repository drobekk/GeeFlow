package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.data.user.model.AppPaletteStyle
import app.geeflow.data.user.model.User
import io.mockk.Called
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SetPaletteStyleUseCaseTest {
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val settings = mockk<UserSettingsRepository>(relaxUnitFun = true)
    private val selected = MutableStateFlow<User?>(User(id = 7, name = "Ada"))

    init {
        every { users.selectedUser } returns selected
    }

    @Test
    fun `when user is selected then saves setting for that user`() = runTest {
        val useCase = SetPaletteStyleUseCase(settings, users)

        useCase(AppPaletteStyle.VIBRANT)

        coVerify(exactly = 1) { settings.setPaletteStyle(7, AppPaletteStyle.VIBRANT) }
    }

    @Test
    fun `when no user is selected then does not save settings`() = runTest {
        val useCase = SetPaletteStyleUseCase(settings, users)
        selected.value = null

        useCase(AppPaletteStyle.VIBRANT)

        coVerify { settings wasNot Called }
    }
}
