package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class RenameUserUseCaseTest {
    private val users = mockk<UserRepository>(relaxUnitFun = true)

    @Test
    fun `when invoked then updates requested user`() = runTest {
        val useCase = RenameUserUseCase(users)

        useCase(7, "New name")

        verify(exactly = 1) { users.renameUser(7, "New name") }
    }
}
