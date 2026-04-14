package dev.drobek.geeflow.domain.user.usecase

import dev.drobek.geeflow.data.user.UserSettingsRepository
import org.koin.core.annotation.Factory

@Factory
class SetKeepScreenOnUseCase(
    private val repository: UserSettingsRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setKeepScreenOn(enabled)
}
