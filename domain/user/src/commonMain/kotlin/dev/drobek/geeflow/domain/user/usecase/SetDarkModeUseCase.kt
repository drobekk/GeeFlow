package dev.drobek.geeflow.domain.user.usecase

import dev.drobek.geeflow.data.user.UserSettingsRepository
import dev.drobek.geeflow.data.user.model.DarkMode
import org.koin.core.annotation.Factory

@Factory
class SetDarkModeUseCase(
    private val repository: UserSettingsRepository,
) {
    suspend operator fun invoke(mode: DarkMode) = repository.setDarkMode(mode)
}
