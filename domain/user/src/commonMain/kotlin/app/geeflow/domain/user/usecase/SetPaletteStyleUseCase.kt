package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.data.user.model.AppPaletteStyle
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class SetPaletteStyleUseCase(
    private val repository: UserSettingsRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(style: AppPaletteStyle) {
        val userId = userRepository.selectedUser.first()?.id ?: return
        repository.setPaletteStyle(userId, style)
    }
}
