package dev.drobek.geeflow.presentation.feature.device.settings.quick

import dev.drobek.geeflow.data.device.api.DeviceController
import dev.drobek.geeflow.domain.device.model.MachineState
import dev.drobek.geeflow.domain.device.model.MachineState.BoilerType
import dev.drobek.geeflow.presentation.feature.device.settings.quick.QuickSettingsEvent.BrewBoilerToggled
import dev.drobek.geeflow.presentation.feature.device.settings.quick.QuickSettingsEvent.BrewTempChanged
import dev.drobek.geeflow.presentation.feature.device.settings.quick.QuickSettingsEvent.CloseClicked
import dev.drobek.geeflow.presentation.feature.device.settings.quick.QuickSettingsEvent.MoreSettingsClicked
import dev.drobek.geeflow.presentation.feature.device.settings.quick.QuickSettingsEvent.SaveClicked
import dev.drobek.geeflow.presentation.feature.device.settings.quick.QuickSettingsEvent.SteamBoilerToggled
import dev.drobek.geeflow.presentation.feature.device.settings.quick.QuickSettingsEvent.SteamTempChanged
import dev.drobek.geeflow.viewmodel.BaseViewModel
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class QuickSettingsViewModel(
    private val deviceController: DeviceController
) : BaseViewModel<QuickSettingsViewState, QuickSettingsViewModelEvent>(QuickSettingsViewState()) {

    init {
        launch {
            deviceController.machineState.collect { state ->
                updateMachineState(state)
            }
        }
    }

    private fun updateMachineState(state: MachineState) = modify {
        val config = state.config
        copy(
            steamBoilerEnabled = config?.steamBoilerEnabled ?: false,
            brewBoilerEnabled = config?.brewBoilerEnabled ?: false,
            targetSteamTemp = config?.targetSteamTemp?.toInt() ?: 0,
            targetBrewTemp = config?.targetBrewTemp?.toInt() ?: 0,
            actualSteamTemp = state.steamBoilerTemp ?: 0f,
            actualBrewTemp = state.brewBoilerTemp ?: 0f,
            selectedSteamTemp = if (selectedSteamTemp == "0") config?.targetSteamTemp?.toInt()?.toString() ?: "0" else selectedSteamTemp,
            selectedBrewTemp = if (selectedBrewTemp == "0") config?.targetBrewTemp?.toInt()?.toString() ?: "0" else selectedBrewTemp
        )
    }

    fun handleEvent(event: QuickSettingsEvent) = when (event) {
        is SteamBoilerToggled -> launch {
            deviceController.setBoilerState(BoilerType.Steam, event.enabled)
        }
        is BrewBoilerToggled -> launch {
            deviceController.setBoilerState(BoilerType.Brew, event.enabled)
        }
        is SteamTempChanged -> modify {
            copy(selectedSteamTemp = event.temp)
        }
        is BrewTempChanged -> modify {
            copy(selectedBrewTemp = event.temp)
        }
        is SaveClicked -> launch {
            val steamTemp = viewState.value.selectedSteamTemp.toIntOrNull()
            val brewTemp = viewState.value.selectedBrewTemp.toIntOrNull()
            steamTemp?.let { deviceController.setSteamTemperature(it) }
            brewTemp?.let { deviceController.setBrewTemperature(it) }
            emitEvent(Navigation.Back)
        }
        is CloseClicked -> emitEvent(Navigation.Back)
        is MoreSettingsClicked -> Unit
    }
}
