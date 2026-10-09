package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SetFavoriteDeviceUseCaseTest {
    private val users = mockk<UserRepository>(relaxUnitFun = true)

    @Test
    fun `when invoked then updates requested user`() = runTest {
        val useCase = SetFavoriteDeviceUseCase(users)

        useCase(7, 9)

        verify(exactly = 1) { users.setFavoriteDevice(7, 9) }
    }
}
