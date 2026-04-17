package dev.drobek.geeflow.domain.user.usecase

import dev.drobek.geeflow.data.user.UserRepository
import org.koin.core.annotation.Factory

@Factory
class RenameUserUseCase(private val userRepository: UserRepository) {
    operator fun invoke(userId: Long, name: String) {
        userRepository.renameUser(userId, name)
    }
}
