package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.model.User
import io.github.vinceglb.filekit.FileKit
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UpdateUserPhotoUseCaseTest {
    private val directory: File = Files.createTempDirectory("geeflow-photos-").toFile()
    private val users = mockk<UserRepository>(relaxUnitFun = true)
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

    private fun imageBytes(): ByteArray = ByteArrayOutputStream().use { output ->
        ImageIO.write(BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB), "png", output)
        output.toByteArray()
    }

    @Test
    fun `when photo is replaced then stores JPEG updates URI and deletes previous photo`() = runTest {
        val photoUri = slot<String>()
        every { users.updatePhotoUri(7, capture(photoUri)) } returns Unit
        val useCase = UpdateUserPhotoUseCase(users)
        val bytes = imageBytes()

        useCase(7, bytes)

        verify(exactly = 1) { users.updatePhotoUri(7, any()) }
        assertTrue(photoUri.captured.startsWith("user_7_"))
        assertTrue(photoUri.captured.endsWith(".jpg"))
        assertNotNull(ImageIO.read(File(directory, photoUri.captured)))
        assertFalse(oldPhoto.exists())
    }

    @Test
    fun `when image bytes are invalid then preserves previous photo and URI`() = runTest {
        val useCase = UpdateUserPhotoUseCase(users)

        val result = runCatching { useCase(7, byteArrayOf(0)) }

        assertTrue(result.isFailure)
        assertTrue(oldPhoto.exists())
        verify(exactly = 0) { users.updatePhotoUri(any(), any()) }
    }
}
