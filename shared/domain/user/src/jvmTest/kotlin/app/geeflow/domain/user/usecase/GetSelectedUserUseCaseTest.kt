package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.model.User
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertSame

class GetSelectedUserUseCaseTest {
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val selected = MutableStateFlow<User?>(User(id = 7, name = "Ada"))

    init {
        every { users.selectedUser } returns selected
    }

    @Test
    fun `when repository emits then returns repository flow`() = runTest {
        val expected = MutableStateFlow(null)
        every { users.selectedUser } returns expected
        val useCase = GetSelectedUserUseCase(users)

        val result = useCase()

        assertSame(expected, result)
    }
}
