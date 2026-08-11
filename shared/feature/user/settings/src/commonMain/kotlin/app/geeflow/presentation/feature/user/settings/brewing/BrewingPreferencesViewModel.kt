package app.geeflow.presentation.feature.user.settings.brewing

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.domain.brew.usecase.RestoreDefaultProfilesUseCase
import app.geeflow.domain.user.usecase.GetBrewingPreferencesUseCase
import app.geeflow.domain.user.usecase.SetAutoConnectUseCase
import app.geeflow.domain.user.usecase.SetSkipManualBrewHistoryUseCase
import app.geeflow.domain.user.usecase.SetTemperatureUnitUseCase
import app.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.AutoConnectChanged
import app.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.BackClicked
import app.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.DialogDismissed
import app.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.RestoreDefaultProfilesClicked
import app.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.RestoreDefaultProfilesConfirmed
import app.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.SkipManualBrewHistoryChanged
import app.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.TemperatureUnitChanged
import app.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.TemperatureUnitClicked
import app.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesViewState.Dialog
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class BrewingPreferencesViewModel(
    private val getBrewingPreferences: GetBrewingPreferencesUseCase,
    private val setAutoConnect: SetAutoConnectUseCase,
    private val setTemperatureUnit: SetTemperatureUnitUseCase,
    private val setSkipManualBrewHistory: SetSkipManualBrewHistoryUseCase,
    private val restoreDefaultProfiles: RestoreDefaultProfilesUseCase,
) : BaseViewModel<BrewingPreferencesViewState, Unit>(BrewingPreferencesViewState()) {

    init {
        launch {
            getBrewingPreferences().collect { settings ->
                modify {
                    copy(
                        autoConnect = settings.autoConnect,
                        temperatureUnit = settings.temperatureUnit,
                        skipManualBrewHistory = settings.skipManualBrewHistory,
                    )
                }
            }
        }
    }

    fun handleEvent(event: BrewingPreferencesEvent) = when (event) {
        is BackClicked -> navigateBack()
        is AutoConnectChanged -> launch { setAutoConnect(event.enabled) }
        is SkipManualBrewHistoryChanged -> launch { setSkipManualBrewHistory(event.enabled) }
        is TemperatureUnitClicked -> modify { copy(dialog = Dialog.TemperatureUnit) }
        is TemperatureUnitChanged -> launch {
            modify { copy(dialog = null) }
            setTemperatureUnit(event.unit)
        }
        is RestoreDefaultProfilesClicked -> modify { copy(dialog = Dialog.RestoreDefaultProfiles) }
        is RestoreDefaultProfilesConfirmed -> launch {
            modify { copy(dialog = null) }
            restoreDefaultProfiles()
        }
        is DialogDismissed -> modify { copy(dialog = null) }
    }
}
