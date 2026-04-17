package dev.drobek.geeflow.domain.user.usecase

import dev.drobek.geeflow.data.user.UserRepository
import dev.drobek.geeflow.data.user.UserSettingsRepository
import dev.drobek.geeflow.data.user.model.TemperatureUnit
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
