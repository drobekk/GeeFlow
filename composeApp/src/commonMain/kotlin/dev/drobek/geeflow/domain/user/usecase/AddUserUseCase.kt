package dev.drobek.geeflow.domain.user.usecase

import dev.drobek.geeflow.data.users.api.UserRepository
import dev.drobek.geeflow.domain.user.model.User
import org.koin.core.annotation.Factory

@Factory
class AddUserUseCase(
    private val userRepository: UserRepository
) {
    operator fun invoke(name: String, photoUri: String? = null, isSelected: Boolean = false) {
        userRepository.addUser(
            User(
                name = name,
                photoUri = photoUri,
                isSelected = isSelected
            )
        )
    }
}
