package dev.drobek.geeflow.domain.user.usecase

import dev.drobek.geeflow.data.user.UserSettingsRepository
import dev.drobek.geeflow.domain.user.model.BrewingPreferencesSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.koin.core.annotation.Factory

@Factory
class GetBrewingPreferencesUseCase(
    private val repository: UserSettingsRepository,
) {
    operator fun invoke(): Flow<BrewingPreferencesSettings> = combine(
        repository.autoConnect,
        repository.temperatureUnit,
    ) { autoConnect, temperatureUnit ->
        BrewingPreferencesSettings(
            autoConnect = autoConnect,
            temperatureUnit = temperatureUnit,
        )
    }
}
