package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.data.user.model.TemperatureUnit
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class SetTemperatureUnitUseCase(
    private val repository: UserSettingsRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(unit: TemperatureUnit) {
        val userId = userRepository.selectedUser.first()?.id ?: return
        repository.setTemperatureUnit(userId, unit)
    }
}
