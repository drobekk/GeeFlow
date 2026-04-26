package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import org.koin.core.annotation.Factory

@Factory
class SetSelectedUserUseCase(
    private val userRepository: UserRepository,
) {
    operator fun invoke(userId: Long) {
        userRepository.setSelectedUser(userId)
    }
}
