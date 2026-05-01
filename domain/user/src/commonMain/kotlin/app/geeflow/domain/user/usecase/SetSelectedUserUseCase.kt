package app.geeflow.domain.user.usecase

import app.geeflow.data.brew.BrewProfileRepository
import app.geeflow.data.user.UserRepository
import org.koin.core.annotation.Factory

@Factory
class SetSelectedUserUseCase(
    private val userRepository: UserRepository,
    private val brewProfileRepository: BrewProfileRepository,
) {
    operator fun invoke(userId: Long) {
        userRepository.setSelectedUser(userId)
        brewProfileRepository.seedDefaultProfilesIfEmpty(userId)
    }
}
