package dev.drobek.geeflow.domain.user.usecase

import dev.drobek.geeflow.data.user.UserRepository
import dev.drobek.geeflow.data.user.model.User
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Factory

@Factory
class GetUsersUseCase(
    private val userRepository: UserRepository
) {
    operator fun invoke(): StateFlow<List<User>> = userRepository.users
}
