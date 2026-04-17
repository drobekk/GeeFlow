package dev.drobek.geeflow.domain.user.usecase

import dev.drobek.geeflow.data.user.UserRepository
import dev.drobek.geeflow.data.user.UserSettingsRepository
import dev.drobek.geeflow.domain.user.model.AppearanceSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import org.koin.core.annotation.Factory

@Factory
class GetAppearanceSettingsUseCase(
    private val repository: UserSettingsRepository,
    private val userRepository: UserRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<AppearanceSettings> = userRepository.selectedUser
        .mapNotNull { it?.id }
        .flatMapLatest { userId ->
            combine(
                repository.darkMode(userId),
                repository.appTheme(userId),
                repository.fullScreenMode(userId),
                repository.keepScreenOn(userId),
            ) { darkMode, appTheme, fullScreenMode, keepScreenOn ->
                AppearanceSettings(
                    darkMode = darkMode,
                    appTheme = appTheme,
                    fullScreenMode = fullScreenMode,
                    keepScreenOn = keepScreenOn,
                )
            }
        }
}
