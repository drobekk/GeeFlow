package app.geeflow.domain.user.usecase

import app.geeflow.data.brew.BrewProfileRepository
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.model.User
import org.koin.core.annotation.Factory

@Factory
class AddUserUseCase(
    private val userRepository: UserRepository,
    private val brewProfileRepository: BrewProfileRepository,
) {
    operator fun invoke(name: String, photoUri: String? = null, isSelected: Boolean = false) {
        val userId = userRepository.addUser(
            User(
                name = name,
                photoUri = photoUri,
                isSelected = false,
            ),
        )
        if (isSelected) userRepository.setSelectedUser(userId)
        brewProfileRepository.seedDefaultProfilesIfEmpty(userId)
    }
}
