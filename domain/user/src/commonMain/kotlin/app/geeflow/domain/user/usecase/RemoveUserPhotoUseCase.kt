package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.filesDir
import org.koin.core.annotation.Factory

@Factory
class RemoveUserPhotoUseCase(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(userId: Long) {
        val fileName = userRepository.getUserById(userId)?.photoUri ?: return
        userRepository.updatePhotoUri(userId, null)
        (FileKit.filesDir / fileName).delete()
    }
}
