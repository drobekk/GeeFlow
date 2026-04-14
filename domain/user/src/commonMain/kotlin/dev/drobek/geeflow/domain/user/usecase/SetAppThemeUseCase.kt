package dev.drobek.geeflow.domain.user.usecase

import dev.drobek.geeflow.data.user.UserSettingsRepository
import dev.drobek.geeflow.data.user.model.AppTheme
import org.koin.core.annotation.Factory

@Factory
class SetAppThemeUseCase(
    private val repository: UserSettingsRepository,
) {
    suspend operator fun invoke(theme: AppTheme) = repository.setAppTheme(theme)
}
