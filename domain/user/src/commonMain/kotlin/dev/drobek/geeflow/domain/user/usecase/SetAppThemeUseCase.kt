package dev.drobek.geeflow.domain.user.usecase

import dev.drobek.geeflow.data.user.UserRepository
import dev.drobek.geeflow.data.user.UserSettingsRepository
import dev.drobek.geeflow.data.user.model.AppTheme
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
