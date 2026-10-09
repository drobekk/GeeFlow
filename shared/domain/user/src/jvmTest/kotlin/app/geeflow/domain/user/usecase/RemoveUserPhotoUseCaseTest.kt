package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.model.User
import io.github.vinceglb.filekit.FileKit
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse

class RemoveUserPhotoUseCaseTest {
    private val users = mockk<UserRepository>(relaxUnitFun = true)
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
    fun `when user has photo then clears URI and deletes file`() = runTest {
        val useCase = RemoveUserPhotoUseCase(users)

        useCase(7)

        verify(exactly = 1) { users.updatePhotoUri(7, null) }
        assertFalse(oldPhoto.exists())
    }

    @Test
    fun `when user has no photo then leaves repository unchanged`() = runTest {
        every { users.getUserById(7) } returns User(id = 7, name = "Ada")
        val useCase = RemoveUserPhotoUseCase(users)

        useCase(7)

        verify(exactly = 0) { users.updatePhotoUri(any(), any()) }
    }

    @Test
    fun `when user is missing then leaves repository unchanged`() = runTest {
        every { users.getUserById(7) } returns null
        val useCase = RemoveUserPhotoUseCase(users)

        useCase(7)

        verify(exactly = 0) { users.updatePhotoUri(any(), any()) }
    }
}
