package app.geeflow.domain.user.usecase

import app.geeflow.data.brew.BrewProfileRepository
import app.geeflow.data.user.UserRepository
import io.mockk.mockk
import io.mockk.verifyOrder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SetSelectedUserUseCaseTest {
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val profiles = mockk<BrewProfileRepository>(relaxUnitFun = true)

    @Test
    fun `when invoked then updates requested user`() = runTest {
        val useCase = SetSelectedUserUseCase(users, profiles)

        useCase(7)

        verifyOrder {
            users.setSelectedUser(7)
            profiles.seedDefaultProfilesIfEmpty(7)
        }
    }
}
