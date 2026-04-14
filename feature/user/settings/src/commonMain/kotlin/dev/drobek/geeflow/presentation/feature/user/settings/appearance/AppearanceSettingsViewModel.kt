package dev.drobek.geeflow.presentation.feature.user.settings.appearance

import dev.drobek.geeflow.core.presentation.BaseViewModel
import dev.drobek.geeflow.core.presentation.launch
import dev.drobek.geeflow.domain.user.usecase.GetAppearanceSettingsUseCase
import dev.drobek.geeflow.domain.user.usecase.SetAppThemeUseCase
import dev.drobek.geeflow.domain.user.usecase.SetDarkModeUseCase
import dev.drobek.geeflow.domain.user.usecase.SetFullScreenModeUseCase
import dev.drobek.geeflow.domain.user.usecase.SetKeepScreenOnUseCase
import dev.drobek.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.AppThemeChanged
import dev.drobek.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.AppThemeClicked
import dev.drobek.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.BackClicked
import dev.drobek.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.DarkModeChanged
import dev.drobek.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.DarkModeClicked
import dev.drobek.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.DialogDismissed
import dev.drobek.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.FullScreenChanged
import dev.drobek.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.KeepScreenOnChanged
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class AppearanceSettingsViewModel(
    private val getAppearanceSettings: GetAppearanceSettingsUseCase,
    private val setDarkMode: SetDarkModeUseCase,
    private val setAppTheme: SetAppThemeUseCase,
    private val setFullScreenMode: SetFullScreenModeUseCase,
    private val setKeepScreenOn: SetKeepScreenOnUseCase,
) : BaseViewModel<AppearanceSettingsViewState, Unit>(AppearanceSettingsViewState()) {

    init {
        launch {
            getAppearanceSettings().collect { settings ->
                modify {
                    copy(
                        darkMode = settings.darkMode,
                        appTheme = settings.appTheme,
                        fullScreenMode = settings.fullScreenMode,
                        keepScreenOn = settings.keepScreenOn,
                    )
                }
            }
        }
    }

    fun handleEvent(event: AppearanceSettingsEvent) = when (event) {
        is BackClicked -> navigateBack()
        is DarkModeClicked -> modify { copy(dialog = AppearanceSettingsViewState.Dialog.DarkMode) }
        is AppThemeClicked -> modify { copy(dialog = AppearanceSettingsViewState.Dialog.AppTheme) }
        is DialogDismissed -> modify { copy(dialog = null) }
        is DarkModeChanged -> launch {
            modify { copy(dialog = null) }
            setDarkMode(event.mode)
        }
        is AppThemeChanged -> launch {
            modify { copy(dialog = null) }
            setAppTheme(event.theme)
        }
        is FullScreenChanged -> launch { setFullScreenMode(event.enabled) }
        is KeepScreenOnChanged -> launch { setKeepScreenOn(event.enabled) }
    }
}
