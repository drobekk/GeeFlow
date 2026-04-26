package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class SetFullScreenModeUseCase(
    private val repository: UserSettingsRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(enabled: Boolean) {
        val userId = userRepository.selectedUser.first()?.id ?: return
        repository.setFullScreenMode(userId, enabled)
    }
}
