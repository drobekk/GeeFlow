package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewProfileRepository
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.model.User
import app.geeflow.domain.user.usecase.GetSelectedUserUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DuplicateProfileUseCaseTest {
    private val profiles = mockk<BrewProfileRepository>(relaxUnitFun = true)
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val selected = MutableStateFlow<User?>(User(id = 7, name = "Ada"))
    private val getSelected = GetSelectedUserUseCase(users)
    private val profile = BrewProfile(
        id = 3,
        userId = 7,
        name = "Shot",
        description = "Recipe",
        finishCondition = null,
        steps = listOf(ProfileStep.Pressure(30, 9f)),
        autoLinkOpen = true,
        position = 4
    )

    init {
        every { users.selectedUser } returns selected
        every { profiles.getBrewProfilesForUser(7) } returns listOf(profile)
    }

    @Test
    fun `when source profile exists then copies it to selected user at last position`() = runTest {
        every { profiles.getBrewProfileById(3) } returns profile
        val useCase = DuplicateProfileUseCase(profiles, getSelected)

        useCase(3, "Copy")

        verify(
            exactly = 1
        ) {
            profiles.addBrewProfile(
                profile.copy(id = BrewProfile.NEW_ID, userId = 7, name = "Copy", autoLinkOpen = false, position = 1)
            )
        }
    }

    @Test
    fun `when source profile is missing then does not create copy`() = runTest {
        every { profiles.getBrewProfileById(3) } returns null
        val useCase = DuplicateProfileUseCase(profiles, getSelected)

        useCase(3, "Copy")

        verify(exactly = 0) { profiles.addBrewProfile(any()) }
    }
}
