package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.model.User
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
class GetSelectedUserUseCase(
    private val userRepository: UserRepository,
) {
    operator fun invoke(): Flow<User?> = userRepository.selectedUser
}
