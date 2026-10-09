package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.data.user.model.User
import io.github.vinceglb.filekit.FileKit
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse

class DeleteUserUseCaseTest {
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val settings = mockk<UserSettingsRepository>(relaxUnitFun = true)
    private val directory: File = Files.createTempDirectory("geeflow-photos-").toFile()
    private val oldPhoto = File(directory, "old.jpg")

    init {
        FileKit.init(filesDir = directory, cacheDir = directory)
        oldPhoto.writeBytes(byteArrayOf(1, 2, 3))
        every { users.getUserById(7) } returns User(id = 7, name = "Ada", photoUri = "old.jpg")
    }

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun `when user has no photo then clears settings before removing user`() = runTest {
        every { users.getUserById(7) } returns User(id = 7, name = "Ada")
        val useCase = DeleteUserUseCase(users, settings)

        useCase(7)

        coVerifyOrder {
            settings.clearUserSettings(7)
            users.removeUser(7)
        }
    }

    @Test
    fun `when user has photo then removes settings user and photo`() = runTest {
        val useCase = DeleteUserUseCase(users, settings)

        useCase(7)

        coVerifyOrder {
            settings.clearUserSettings(7)
            users.removeUser(7)
        }
        assertFalse(oldPhoto.exists())
    }
}
