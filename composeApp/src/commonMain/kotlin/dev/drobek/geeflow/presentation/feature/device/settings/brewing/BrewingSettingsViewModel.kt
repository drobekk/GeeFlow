package dev.drobek.geeflow.presentation.feature.device.settings.brewing

import dev.drobek.geeflow.domain.device.model.MachineState
import dev.drobek.geeflow.domain.device.model.MachineState.BoilerType
import dev.drobek.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import dev.drobek.geeflow.domain.device.usecase.SetBoilerStateUseCase
import dev.drobek.geeflow.domain.device.usecase.SetBrewTemperatureUseCase
import dev.drobek.geeflow.domain.device.usecase.SetSteamTemperatureUseCase
import dev.drobek.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.BrewBoilerToggled
import dev.drobek.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.BrewTempChanged
import dev.drobek.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.CloseClicked
import dev.drobek.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.MoreSettingsClicked
import dev.drobek.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.SaveClicked
import dev.drobek.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.SteamBoilerToggled
import dev.drobek.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.SteamTempChanged
import dev.drobek.geeflow.presentation.feature.device.settings.navigation.DeviceSettingsDestinations.BrewingSettings
import dev.drobek.geeflow.viewmodel.BaseViewModel
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class BrewingSettingsViewModel(
    @InjectedParam val arguments: BrewingSettings,
    private val observeDeviceState: ObserveDeviceStateUseCase,
    private val setBoilerState: SetBoilerStateUseCase,
    private val setBrewTemperature: SetBrewTemperatureUseCase,
    private val setSteamTemperature: SetSteamTemperatureUseCase
) : BaseViewModel<BrewingSettingsViewState, BrewingSettingsViewModelEvent>(BrewingSettingsViewState()) {

    init {
        launch {
            observeDeviceState(arguments.deviceId).collect { state ->
                updateMachineState(state)
            }
        }
    }

    private fun updateMachineState(state: MachineState) = modify {
        val config = state.config
        copy(
            steamBoiler = steamBoiler.copy(
                enabled = config?.steamBoilerEnabled ?: false,
                actualTemp = state.steamBoilerTemp ?: 0f,
                selectedTemp = if (steamBoiler.selectedTemp == "0") config?.targetSteamTemp?.toInt()?.toString()
                    ?: "0" else steamBoiler.selectedTemp
            ),
            brewBoiler = brewBoiler.copy(
                enabled = config?.brewBoilerEnabled ?: false,
                actualTemp = state.brewBoilerTemp ?: 0f,
                selectedTemp = if (brewBoiler.selectedTemp == "0") config?.targetBrewTemp?.toInt()?.toString()
                    ?: "0" else brewBoiler.selectedTemp
            )
        )
    }

    fun handleEvent(event: BrewingSettingsEvent) = when (event) {
        is SteamBoilerToggled -> launch {
            setBoilerState(arguments.deviceId, BoilerType.Steam, event.enabled)
        }

        is BrewBoilerToggled -> launch {
            setBoilerState(arguments.deviceId, BoilerType.Brew, event.enabled)
        }

        is SteamTempChanged -> modify {
            copy(steamBoiler = steamBoiler.copy(selectedTemp = event.temp))
        }

        is BrewTempChanged -> modify {
            copy(brewBoiler = brewBoiler.copy(selectedTemp = event.temp))
        }

        is SaveClicked -> launch {
            val steamTemp = viewState.value.steamBoiler.selectedTemp.toIntOrNull()
            val brewTemp = viewState.value.brewBoiler.selectedTemp.toIntOrNull()
            steamTemp?.let { setSteamTemperature(arguments.deviceId, it) }
            brewTemp?.let { setBrewTemperature(arguments.deviceId, it) }
            emitEvent(Navigation.Back)
        }

        is CloseClicked -> emitEvent(Navigation.Back)
        is MoreSettingsClicked -> Unit
        is BrewingSettingsEvent.PulseHeatingToggled -> Unit
        is BrewingSettingsEvent.PaddlePressureChanged -> Unit
        is BrewingSettingsEvent.PaddleTimeChanged -> Unit
    }
}
