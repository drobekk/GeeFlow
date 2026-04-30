package app.geeflow.presentation.feature.user.settings.appearance

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.domain.user.usecase.GetAppearanceSettingsUseCase
import app.geeflow.domain.user.usecase.SetAppThemeUseCase
import app.geeflow.domain.user.usecase.SetDarkModeUseCase
import app.geeflow.domain.user.usecase.SetFullScreenModeUseCase
import app.geeflow.domain.user.usecase.SetKeepScreenOnUseCase
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.AppThemeChanged
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.AppThemeClicked
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.BackClicked
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.DarkModeChanged
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.DarkModeClicked
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.DialogDismissed
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.FullScreenChanged
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.KeepScreenOnChanged
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
                        themeMode = settings.themeMode,
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
