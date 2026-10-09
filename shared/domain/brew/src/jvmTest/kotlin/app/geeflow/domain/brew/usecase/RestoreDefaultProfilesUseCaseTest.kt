package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewProfileRepository
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.model.User
import app.geeflow.domain.user.usecase.GetSelectedUserUseCase
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class RestoreDefaultProfilesUseCaseTest {
    private val profiles = mockk<BrewProfileRepository>(relaxUnitFun = true)
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val selected = MutableStateFlow<User?>(User(id = 7, name = "Ada"))
    private val getSelected = GetSelectedUserUseCase(users)

    init {
        every { users.selectedUser } returns selected
    }

    @Test
    fun `when user is selected then resets only selected user profiles`() = runTest {
        val useCase = RestoreDefaultProfilesUseCase(getSelected, profiles)

        useCase()

        verify(exactly = 1) { profiles.resetProfilesForUser(7) }
    }

    @Test
    fun `when no user is selected then leaves profiles unchanged`() = runTest {
        selected.value = null
        val useCase = RestoreDefaultProfilesUseCase(getSelected, profiles)

        useCase()

        verify { profiles wasNot Called }
    }
}
