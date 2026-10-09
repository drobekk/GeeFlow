package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewProfileRepository
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.model.User
import app.geeflow.domain.user.usecase.GetSelectedUserUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveUserProfilesUseCaseTest {
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
    }

    @Test
    fun `when dependency contains data then returns requested data`() = runTest {
        every {
            profiles.brewProfiles
        } returns MutableStateFlow(listOf(profile, profile.copy(id = 4, userId = 8)))
        val useCase = ObserveUserProfilesUseCase(getSelected, profiles)

        val result = useCase().first()

        assertEquals(listOf(profile), result)
    }
}
