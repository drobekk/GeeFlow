package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.compressImage
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.write
import org.koin.core.annotation.Factory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Factory
class UpdateUserPhotoUseCase(
    private val userRepository: UserRepository,
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(userId: Long, bytes: ByteArray) {
        val oldFileName = userRepository.getUserById(userId)?.photoUri
        val newFileName = "user_${userId}_${Uuid.random()}.jpg"
        val compressed = FileKit.compressImage(
            bytes = bytes,
            quality = 85,
            maxWidth = 1024,
            maxHeight = 1024,
        )
        (FileKit.filesDir / newFileName).write(compressed)
        userRepository.updatePhotoUri(userId, newFileName)
        if (oldFileName != null) (FileKit.filesDir / oldFileName).delete()
    }
}
