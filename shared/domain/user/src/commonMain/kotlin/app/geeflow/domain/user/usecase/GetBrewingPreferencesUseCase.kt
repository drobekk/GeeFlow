package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.domain.user.model.BrewingPreferencesSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import org.koin.core.annotation.Factory

@Factory
class GetBrewingPreferencesUseCase(
    private val repository: UserSettingsRepository,
    private val userRepository: UserRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<BrewingPreferencesSettings> = userRepository.selectedUser
        .mapNotNull { it?.id }
        .flatMapLatest { userId ->
            combine(
                repository.autoConnect(userId),
                repository.temperatureUnit(userId),
            ) { autoConnect, temperatureUnit ->
                BrewingPreferencesSettings(
                    autoConnect = autoConnect,
                    temperatureUnit = temperatureUnit,
                )
            }
        }
}
