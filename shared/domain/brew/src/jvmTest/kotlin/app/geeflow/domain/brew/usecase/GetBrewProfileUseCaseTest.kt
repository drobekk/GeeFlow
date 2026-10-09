package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewProfileRepository
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.ProfileStep
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetBrewProfileUseCaseTest {
    private val profiles = mockk<BrewProfileRepository>(relaxUnitFun = true)
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

    @Test
    fun `when dependency contains data then returns requested data`() = runTest {
        every { profiles.getBrewProfileById(3) } returns profile
        val useCase = GetBrewProfileUseCase(profiles)

        val result = useCase(3)

        assertEquals(profile, result)
    }
}
