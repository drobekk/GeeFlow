package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewHistoryRepository
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.model.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetBrewHistoryUseCaseTest {
    private val history = mockk<BrewHistoryRepository>(relaxUnitFun = true)
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val selected = MutableStateFlow<User?>(User(id = 7, name = "Ada"))

    init {
        every { users.selectedUser } returns selected
    }

    @Test
    fun `when dependency contains data then returns requested data`() = runTest {
        every { history.getBrews(7, "Shot", 20, 40) } returns emptyList()
        val useCase = GetBrewHistoryUseCase(history, users)

        val result = useCase("Shot", 20, 40)

        assertEquals(emptyList(), result)
        verify(exactly = 1) { history.getBrews(7, "Shot", 20, 40) }
    }
}
