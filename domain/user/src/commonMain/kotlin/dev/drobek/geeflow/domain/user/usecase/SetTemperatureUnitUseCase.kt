package dev.drobek.geeflow.domain.user.usecase

import dev.drobek.geeflow.data.user.UserSettingsRepository
import dev.drobek.geeflow.data.user.model.TemperatureUnit
import org.koin.core.annotation.Factory

@Factory
class SetTemperatureUnitUseCase(
    private val repository: UserSettingsRepository,
) {
    suspend operator fun invoke(unit: TemperatureUnit) = repository.setTemperatureUnit(unit)
}
