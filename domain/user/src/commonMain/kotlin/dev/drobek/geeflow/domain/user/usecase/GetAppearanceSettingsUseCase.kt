package dev.drobek.geeflow.domain.user.usecase

import dev.drobek.geeflow.data.user.UserSettingsRepository
import dev.drobek.geeflow.domain.user.model.AppearanceSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.koin.core.annotation.Factory

@Factory
class GetAppearanceSettingsUseCase(
    private val repository: UserSettingsRepository,
) {
    operator fun invoke(): Flow<AppearanceSettings> = combine(
        repository.darkMode,
        repository.appTheme,
        repository.fullScreenMode,
        repository.keepScreenOn,
    ) { darkMode, appTheme, fullScreenMode, keepScreenOn ->
        AppearanceSettings(
            darkMode = darkMode,
            appTheme = appTheme,
            fullScreenMode = fullScreenMode,
            keepScreenOn = keepScreenOn,
        )
    }
}
