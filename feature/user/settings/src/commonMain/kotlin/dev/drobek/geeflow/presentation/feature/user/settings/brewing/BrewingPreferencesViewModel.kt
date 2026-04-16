package dev.drobek.geeflow.presentation.feature.user.settings.brewing

import dev.drobek.geeflow.core.presentation.BaseViewModel
import dev.drobek.geeflow.core.presentation.launch
import dev.drobek.geeflow.domain.brew.usecase.RestoreDefaultProfilesUseCase
import dev.drobek.geeflow.domain.user.usecase.GetBrewingPreferencesUseCase
import dev.drobek.geeflow.domain.user.usecase.SetAutoConnectUseCase
import dev.drobek.geeflow.domain.user.usecase.SetTemperatureUnitUseCase
import dev.drobek.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.AutoConnectChanged
import dev.drobek.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.BackClicked
import dev.drobek.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.DialogDismissed
import dev.drobek.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.RestoreDefaultProfilesClicked
import dev.drobek.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.RestoreDefaultProfilesConfirmed
import dev.drobek.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.TemperatureUnitChanged
import dev.drobek.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.TemperatureUnitClicked
import dev.drobek.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesViewState.Dialog
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class BrewingPreferencesViewModel(
    private val getBrewingPreferences: GetBrewingPreferencesUseCase,
    private val setAutoConnect: SetAutoConnectUseCase,
    private val setTemperatureUnit: SetTemperatureUnitUseCase,
    private val restoreDefaultProfiles: RestoreDefaultProfilesUseCase,
) : BaseViewModel<BrewingPreferencesViewState, Unit>(BrewingPreferencesViewState()) {

    init {
        launch {
            getBrewingPreferences().collect { settings ->
                modify {
                    copy(
                        autoConnect = settings.autoConnect,
                        temperatureUnit = settings.temperatureUnit,
                    )
                }
            }
        }
    }

    fun handleEvent(event: BrewingPreferencesEvent) = when (event) {
        is BackClicked -> navigateBack()
        is AutoConnectChanged -> launch { setAutoConnect(event.enabled) }
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
