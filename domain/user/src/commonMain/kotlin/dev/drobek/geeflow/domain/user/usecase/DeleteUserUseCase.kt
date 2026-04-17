package dev.drobek.geeflow.domain.user.usecase

import dev.drobek.geeflow.data.user.UserRepository
import dev.drobek.geeflow.data.user.UserSettingsRepository
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
