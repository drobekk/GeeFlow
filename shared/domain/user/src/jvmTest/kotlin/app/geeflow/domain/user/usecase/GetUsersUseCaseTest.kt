package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.model.User
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertSame

class GetUsersUseCaseTest {
    private val users = mockk<UserRepository>(relaxUnitFun = true)

    @Test
    fun `when repository emits then returns repository flow`() = runTest {
        val expected = MutableStateFlow(listOf(User(id = 7, name = "Ada")))
        every { users.users } returns expected
        val useCase = GetUsersUseCase(users)

        val result = useCase()

        assertSame(expected, result)
    }
}
