package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewProfileRepository
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.ProfileStep
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class UpdateBrewProfilesPositionsUseCaseTest {
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
    fun `when profile order changes then forwards ordered profiles`() = runTest {
        val orderedProfiles = listOf(profile.copy(id = 4), profile)
        val useCase = UpdateBrewProfilesPositionsUseCase(profiles)

        useCase(orderedProfiles)

        verify(exactly = 1) { profiles.updateBrewProfilesPositions(orderedProfiles) }
    }
}
