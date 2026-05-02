package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.filesDir
import org.koin.core.annotation.Factory

@Factory
class DeleteUserUseCase(
    private val userRepository: UserRepository,
    private val userSettingsRepository: UserSettingsRepository,
) {
    suspend operator fun invoke(userId: Long) {
        val photoFileName = userRepository.getUserById(userId)?.photoUri
        userSettingsRepository.clearUserSettings(userId)
        userRepository.removeUser(userId)
        if (photoFileName != null) (FileKit.filesDir / photoFileName).delete()
    }
}
