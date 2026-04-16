package dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings

import co.touchlab.kermit.Logger
import dev.drobek.geeflow.core.presentation.BaseViewModel
import dev.drobek.geeflow.core.presentation.launch
import dev.drobek.geeflow.core.presentation.launchCatching
import dev.drobek.geeflow.data.device.model.DeviceState
import dev.drobek.geeflow.data.device.model.DeviceState.BoilerType
import dev.drobek.geeflow.domain.device.usecase.GetDeviceConstraintsUseCase
import dev.drobek.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import dev.drobek.geeflow.domain.device.usecase.SetBoilerSettingsUseCase
import dev.drobek.geeflow.navigation.NavEvent
import dev.drobek.geeflow.navigation.destination.DeviceSettings
import dev.drobek.geeflow.navigation.destination.DeviceSettings.EntryPoint
import dev.drobek.geeflow.presentation.feature.device.dashboard.QuickSettings
import dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.BrewBoilerToggled
import dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.BrewTempChanged
import dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.CloseClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.ConfirmClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.MoreSettingsClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.SteamBoilerToggled
import dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.SteamTempChanged
import dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsViewModelEvent.ShowSnackbar
import geeflow.core.ui.generated.resources.Res
import geeflow.core.ui.generated.resources.error_generic
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.withIndex
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class QuickSettingsViewModel(
    @InjectedParam val arguments: QuickSettings,
    private val observeDeviceState: ObserveDeviceStateUseCase,
    private val getDeviceConstraints: GetDeviceConstraintsUseCase,
    private val setBoilerSettings: SetBoilerSettingsUseCase,
) : BaseViewModel<QuickSettingsViewState, QuickSettingsViewModelEvent>(QuickSettingsViewState()) {

    private var applyJob: Job? = null

    init {
        loadMachineState()
    }

    private fun loadMachineState() {
        launch {
            val constraints = getDeviceConstraints(arguments.deviceId)
            modify {
                copy(
                    brewBoiler = brewBoiler.copy(tempList = constraints.brewTempRange.map { it.toString() }),
                    steamBoiler = steamBoiler.copy(tempList = constraints.steamTempRange.map { it.toString() }),
                )
            }
            observeDeviceState(arguments.deviceId)
                .withIndex()
                .collect {
                    if (it.index == 0) {
                        updateMachineState(it.value)
                    } else {
                        modify {
                            copy(
                                brewBoiler = brewBoiler.copy(actualTemp = it.value.brewBoilerTemp ?: 0f),
                                steamBoiler = steamBoiler.copy(actualTemp = it.value.steamBoilerTemp ?: 0f),
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
                selectedTemp = config?.targetSteamTemp?.toInt().toString(),
            ),
            brewBoiler = brewBoiler.copy(
                enabled = config?.brewBoilerEnabled ?: false,
                actualTemp = state.brewBoilerTemp ?: 0f,
                selectedTemp = config?.targetBrewTemp?.toInt().toString(),
            ),
        )
    }

    fun handleEvent(event: QuickSettingsEvent) = when (event) {
        is SteamBoilerToggled -> modify { copy(steamBoiler = steamBoiler.copy(enabled = event.enabled)) }
        is BrewBoilerToggled -> modify { copy(brewBoiler = brewBoiler.copy(enabled = event.enabled)) }
        is SteamTempChanged -> modify { copy(steamBoiler = steamBoiler.copy(selectedTemp = event.temp)) }
        is BrewTempChanged -> modify { copy(brewBoiler = brewBoiler.copy(selectedTemp = event.temp)) }
        is ConfirmClicked -> saveSettings()
        is CloseClicked -> navigate(NavEvent.Back)
        is MoreSettingsClicked -> {
            navigate(NavEvent.Back)
            navigate(NavEvent.To(DeviceSettings(arguments.deviceId, if (event.isExpanded) EntryPoint.Brewing else null)))
        }
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
                    temp = viewState.value.steamBoiler.selectedTemp.toInt(),
                )
                setBoilerSettings(
                    deviceId = arguments.deviceId,
                    boilerType = BoilerType.Brew,
                    enabled = viewState.value.brewBoiler.enabled,
                    temp = viewState.value.brewBoiler.selectedTemp.toInt(),
                )
                modify { copy(applying = false) }
                navigate(NavEvent.Back)
            },
        )
    }

    private fun showError(throwable: Throwable) {
        Logger.e(throwable) { "Error while updating quick settings" }
        launch { emitEvent(ShowSnackbar(getString(Res.string.error_generic))) }
    }
}
