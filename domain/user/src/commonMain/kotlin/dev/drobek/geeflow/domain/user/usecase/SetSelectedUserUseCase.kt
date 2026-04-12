package dev.drobek.geeflow.domain.user.usecase

import dev.drobek.geeflow.data.user.UserRepository
import org.koin.core.annotation.Factory

@Factory
class SetSelectedUserUseCase(
    private val userRepository: UserRepository,
) {
    operator fun invoke(userId: Long) {
        userRepository.setSelectedUser(userId)
    }
}
