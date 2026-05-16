package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.domain.user.model.AppearanceSettings
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
                combine(
                    repository.darkMode(userId),
                    repository.appTheme(userId),
                    repository.paletteStyle(userId),
                    repository.customSeedColor(userId),
                ) { darkMode, appTheme, paletteStyle, customSeedColor ->
                    AppearanceSettings(
                        themeMode = darkMode,
                        appTheme = appTheme,
                        paletteStyle = paletteStyle,
                        customSeedColor = customSeedColor,
                    )
                },
                repository.fullScreenMode(userId),
                repository.keepScreenOn(userId),
            ) { settings, fullScreenMode, keepScreenOn ->
                settings.copy(fullScreenMode = fullScreenMode, keepScreenOn = keepScreenOn)
            }
        }
}
