package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import org.koin.core.annotation.Factory

@Factory
class DeleteUserUseCase(
    private val userRepository: UserRepository,
    private val userSettingsRepository: UserSettingsRepository,
) {
    suspend operator fun invoke(userId: Long) {
        userSettingsRepository.clearUserSettings(userId)
        userRepository.removeUser(userId)
    }
}
