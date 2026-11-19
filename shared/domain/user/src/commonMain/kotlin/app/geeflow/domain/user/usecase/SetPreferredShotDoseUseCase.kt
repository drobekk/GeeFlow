package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class SetPreferredShotDoseUseCase(
    private val settings: UserSettingsRepository,
    private val users: UserRepository,
) {
    suspend operator fun invoke(doseGrams: Float) {
        users.selectedUser.first()?.id?.let { settings.setPreferredShotDoseGrams(it, doseGrams) }
    }
}
