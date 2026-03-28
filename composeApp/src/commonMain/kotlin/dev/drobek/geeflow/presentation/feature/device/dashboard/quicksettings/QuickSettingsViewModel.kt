package dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings

import co.touchlab.kermit.Logger
import dev.drobek.geeflow.domain.device.model.DeviceState
import dev.drobek.geeflow.domain.device.model.DeviceState.BoilerType
import dev.drobek.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import dev.drobek.geeflow.domain.device.usecase.SetBoilerSettingsUseCase
import dev.drobek.geeflow.presentation.feature.device.dashboard.navigation.DeviceDashboardDestinations.QuickSettings
import dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.BrewBoilerToggled
import dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.BrewTempChanged
import dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.CloseClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.ConfirmClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.MoreSettingsClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.SteamBoilerToggled
import dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.SteamTempChanged
import dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsViewModelEvent.ShowSnackbar
import dev.drobek.geeflow.viewmodel.BaseViewModel
import geeflow.composeapp.generated.resources.Res
import geeflow.composeapp.generated.resources.error_generic
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.withIndex
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class QuickSettingsViewModel(
    @InjectedParam val arguments: QuickSettings,
    private val observeDeviceState: ObserveDeviceStateUseCase,
    private val setBoilerSettings: SetBoilerSettingsUseCase
) : BaseViewModel<QuickSettingsViewState, QuickSettingsViewModelEvent>(QuickSettingsViewState()) {

    private var applyJob: Job? = null

    init {
        loadMachineState()
    }

    private fun loadMachineState() {
        launch {
            observeDeviceState(arguments.deviceId)
                .withIndex()
                .collect {
                    if (it.index == 0) {
                        updateMachineState(it.value)
                    } else {
                        modify {
                            copy(
                                brewBoiler = brewBoiler.copy(actualTemp = it.value.brewBoilerTemp ?: 0f),
                                steamBoiler = steamBoiler.copy(actualTemp = it.value.steamBoilerTemp ?: 0f)
                            )
                        }
                    }
                }
        }
    }

    private fun updateMachineState(state: DeviceState) = modify {
        val config = state.config
        copy(
            steamBoiler = steamBoiler.copy(
                enabled = config?.steamBoilerEnabled ?: false,
                actualTemp = state.steamBoilerTemp ?: 0f,
                selectedTemp = config?.targetSteamTemp?.toInt().toString()
            ),
            brewBoiler = brewBoiler.copy(
                enabled = config?.brewBoilerEnabled ?: false,
                actualTemp = state.brewBoilerTemp ?: 0f,
                selectedTemp = config?.targetBrewTemp?.toInt().toString()
            )
        )
    }

    fun handleEvent(event: QuickSettingsEvent) = when (event) {
        is SteamBoilerToggled -> modify { copy(steamBoiler = steamBoiler.copy(enabled = event.enabled)) }
        is BrewBoilerToggled -> modify { copy(brewBoiler = brewBoiler.copy(enabled = event.enabled)) }
        is SteamTempChanged -> modify { copy(steamBoiler = steamBoiler.copy(selectedTemp = event.temp)) }
        is BrewTempChanged -> modify { copy(brewBoiler = brewBoiler.copy(selectedTemp = event.temp)) }
        is ConfirmClicked -> saveSettings()
        is CloseClicked -> emitEvent(Navigation.Back)
        is MoreSettingsClicked -> emitEvent(Navigation.DeviceSettings(arguments.deviceId))
    }

    private fun saveSettings() {
        applyJob?.cancel()
        applyJob = launchCatching(
            onError = (::showError),
            block = {
                modify { copy(applying = true) }
                setBoilerSettings(
                    deviceId = arguments.deviceId,
                    boilerType = BoilerType.Steam,
                    enabled = viewState.value.steamBoiler.enabled,
                    temp = viewState.value.steamBoiler.selectedTemp.toInt()
                )
                setBoilerSettings(
                    deviceId = arguments.deviceId,
                    boilerType = BoilerType.Brew,
                    enabled = viewState.value.brewBoiler.enabled,
                    temp = viewState.value.brewBoiler.selectedTemp.toInt()
                )
                modify { copy(applying = false) }
                emitEvent(Navigation.Back)
            }
        )
    }

    private fun showError(throwable: Throwable) {
        Logger.e(throwable) { "Error while updating quick settings" }
        emitEvent { ShowSnackbar(getString(Res.string.error_generic)) }
    }
}
