package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewProfileRepository
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.data.brew.model.SingleDoseSettings
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
import kotlin.test.assertTrue

class SaveBrewProfileUseCaseTest {
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
    fun `when existing profile is edited then preserves fields absent from editor`() = runTest {
        every { profiles.getBrewProfileById(3) } returns profile
        val useCase = SaveBrewProfileUseCase(profiles, getSelected)

        useCase(3, "Edited", "Description", Condition.Volume(40f), profile.program)

        verify(
            exactly = 1
        ) {
            profiles.addBrewProfile(
                profile.copy(name = "Edited", description = "Description", finishCondition = Condition.Volume(40f))
            )
        }
    }

    @Test
    fun `when new profile is saved then assigns selected user and last position`() = runTest {
        every { profiles.getBrewProfileById(0) } returns null
        val useCase = SaveBrewProfileUseCase(profiles, getSelected)

        useCase(0, "New", "Description", null, profile.program)

        verify(
            exactly = 1
        ) {
            profiles.addBrewProfile(
                BrewProfile(
                    userId = 7,
                    name = "New",
                    description = "Description",
                    program = profile.program,
                    position = 1,
                )
            )
        }
    }

    @Test
    fun `when Single Dose settings are saved then they belong to the selected profile`() = runTest {
        every { profiles.getBrewProfileById(3) } returns profile
        val settings = SingleDoseSettings(enabled = true, grindingSize = 450, grindingSpeed = 720)
        val useCase = SaveBrewProfileUseCase(profiles, getSelected)

        useCase(3, profile.name, profile.description, profile.finishCondition, profile.program, settings)

        verify(exactly = 1) {
            profiles.addBrewProfile(profile.copy(singleDoseSettings = settings))
        }
    }

    @Test
    fun `when program has no phases then rejects before saving`() = runTest {
        val useCase = SaveBrewProfileUseCase(profiles, getSelected)

        val result = runCatching { useCase(0, "Invalid", "", null, BrewProgram.Phases(emptyList())) }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        verify { profiles wasNot Called }
    }
}
