package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class ToggleFavoriteProfileUseCase(private val repository: UserSettingsRepository, private val users: UserRepository) {
    suspend operator fun invoke(profileId: String) {
        users.selectedUser.first()?.id?.let { repository.toggleFavoriteProfile(it, profileId) }
    }
}
