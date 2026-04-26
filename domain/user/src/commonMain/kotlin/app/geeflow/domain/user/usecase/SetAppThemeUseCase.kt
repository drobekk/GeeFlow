package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.data.user.model.AppTheme
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class SetAppThemeUseCase(
    private val repository: UserSettingsRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(theme: AppTheme) {
        val userId = userRepository.selectedUser.first()?.id ?: return
        repository.setAppTheme(userId, theme)
    }
}
