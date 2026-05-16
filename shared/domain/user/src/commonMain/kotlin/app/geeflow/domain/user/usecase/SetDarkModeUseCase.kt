package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.data.user.model.ThemeMode
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class SetDarkModeUseCase(
    private val repository: UserSettingsRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(mode: ThemeMode) {
        val userId = userRepository.selectedUser.first()?.id ?: return
        repository.setDarkMode(userId, mode)
    }
}
