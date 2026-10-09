package app.geeflow.domain.user.usecase

import app.geeflow.data.brew.BrewProfileRepository
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.model.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class AddUserUseCaseTest {
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val profiles = mockk<BrewProfileRepository>(relaxUnitFun = true)

    @Test
    fun `when user is added and selected then seeds profiles after selecting returned id`() = runTest {
        every { users.addUser(any()) } returns 7
        val useCase = AddUserUseCase(users, profiles)

        useCase("Ada", "photo.jpg", isSelected = true)

        verifyOrder {
            users.addUser(User(name = "Ada", photoUri = "photo.jpg", isSelected = false))
            users.setSelectedUser(7)
            profiles.seedDefaultProfilesIfEmpty(7)
        }
    }

    @Test
    fun `when user is added without selection then seeds profiles without selecting user`() = runTest {
        every { users.addUser(any()) } returns 7
        val useCase = AddUserUseCase(users, profiles)

        useCase("Ada", "photo.jpg", isSelected = false)

        verifyOrder {
            users.addUser(User(name = "Ada", photoUri = "photo.jpg", isSelected = false))
            profiles.seedDefaultProfilesIfEmpty(7)
        }
        verify(exactly = 0) { users.setSelectedUser(any()) }
    }
}
