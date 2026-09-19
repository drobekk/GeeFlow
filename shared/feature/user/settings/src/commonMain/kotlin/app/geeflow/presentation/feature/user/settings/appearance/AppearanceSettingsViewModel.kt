@file:Suppress("LongParameterList")

package app.geeflow.presentation.feature.user.settings.appearance

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.data.user.model.AppTheme
import app.geeflow.domain.user.usecase.GetAppearanceSettingsUseCase
import app.geeflow.domain.user.usecase.SetAppThemeUseCase
import app.geeflow.domain.user.usecase.SetCustomSeedColorUseCase
import app.geeflow.domain.user.usecase.SetDarkModeUseCase
import app.geeflow.domain.user.usecase.SetFullScreenModeUseCase
import app.geeflow.domain.user.usecase.SetKeepScreenOnUseCase
import app.geeflow.domain.user.usecase.SetPaletteStyleUseCase
import app.geeflow.domain.user.usecase.SetScreensaverEnabledUseCase
import app.geeflow.domain.user.usecase.SetScreensaverTimeoutMinutesUseCase
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.AppThemeChanged
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.AppThemeClicked
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.BackClicked
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.CustomColorChanged
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.CustomColorClicked
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.CustomColorConfirmed
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.DarkModeChanged
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.DarkModeClicked
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.DialogDismissed
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.FullScreenChanged
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.KeepScreenOnChanged
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.PaletteStyleChanged
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.PaletteStyleClicked
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.ScreensaverChanged
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.ScreensaverTimeoutClicked
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.ScreensaverTimeoutConfirmed
import kotlinx.coroutines.Job
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class AppearanceSettingsViewModel(
    private val getAppearanceSettings: GetAppearanceSettingsUseCase,
    private val setDarkMode: SetDarkModeUseCase,
    private val setAppTheme: SetAppThemeUseCase,
    private val setPaletteStyle: SetPaletteStyleUseCase,
    private val setCustomSeedColor: SetCustomSeedColorUseCase,
    private val setFullScreenMode: SetFullScreenModeUseCase,
    private val setKeepScreenOn: SetKeepScreenOnUseCase,
    private val setScreensaverEnabled: SetScreensaverEnabledUseCase,
    private val setScreensaverTimeoutMinutes: SetScreensaverTimeoutMinutesUseCase,
) : BaseViewModel<AppearanceSettingsViewState, Unit>(AppearanceSettingsViewState()) {

    private var colorPickerJob: Job? = null

    init {
        launch {
            getAppearanceSettings().collect { settings ->
                modify {
                    copy(
                        themeMode = settings.themeMode,
                        appTheme = settings.appTheme,
                        paletteStyle = settings.paletteStyle,
                        customSeedColor = settings.customSeedColor,
                        fullScreenMode = settings.fullScreenMode,
                        keepScreenOn = settings.keepScreenOn,
                        screensaverEnabled = settings.screensaverEnabled,
                        screensaverTimeoutMinutes = settings.screensaverTimeoutMinutes,
                    )
                }
            }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    fun handleEvent(event: AppearanceSettingsEvent) = when (event) {
        is BackClicked -> navigateBack()
        is DarkModeClicked -> modify { copy(dialog = AppearanceSettingsViewState.Dialog.DarkMode) }
        is AppThemeClicked -> modify { copy(dialog = AppearanceSettingsViewState.Dialog.AppTheme) }
        is PaletteStyleClicked -> modify { copy(dialog = AppearanceSettingsViewState.Dialog.PaletteStyle) }
        is CustomColorClicked -> modify { copy(dialog = AppearanceSettingsViewState.Dialog.CustomColor) }
        is DialogDismissed -> modify { copy(dialog = null) }
        is DarkModeChanged -> launch {
            modify { copy(dialog = null) }
            setDarkMode(event.mode)
        }
        is AppThemeChanged -> launch {
            setAppTheme(event.theme)
            modify {
                copy(dialog = if (event.theme == AppTheme.CUSTOM) AppearanceSettingsViewState.Dialog.CustomColor else null)
            }
        }
        is PaletteStyleChanged -> launch {
            modify { copy(dialog = null) }
            setPaletteStyle(event.style)
        }
        is CustomColorChanged -> {
            modify { copy(customSeedColor = event.colorArgb) }
            colorPickerJob?.cancel()
            colorPickerJob = launch {
                setCustomSeedColor(event.colorArgb)
            }
        }
        is CustomColorConfirmed -> {
            colorPickerJob?.cancel()
            launch {
                modify { copy(dialog = null) }
                setCustomSeedColor(event.colorArgb)
            }
        }
        is FullScreenChanged -> launch { setFullScreenMode(event.enabled) }
        is KeepScreenOnChanged -> launch { setKeepScreenOn(event.enabled) }
        is ScreensaverChanged -> launch { setScreensaverEnabled(event.enabled) }
        is ScreensaverTimeoutClicked -> modify { copy(dialog = AppearanceSettingsViewState.Dialog.ScreensaverTimeout) }
        is ScreensaverTimeoutConfirmed -> launch {
            modify { copy(dialog = null) }
            setScreensaverTimeoutMinutes(event.minutes)
        }
    }
}
