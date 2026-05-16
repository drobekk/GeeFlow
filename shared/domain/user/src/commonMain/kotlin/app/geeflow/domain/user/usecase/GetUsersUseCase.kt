package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.model.User
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Factory

@Factory
class GetUsersUseCase(
    private val userRepository: UserRepository,
) {
    operator fun invoke(): StateFlow<List<User>> = userRepository.users
}
