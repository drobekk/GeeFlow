package app.geeflow.presentation.feature.device.dashboard.quicksettings

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.core.presentation.launchCatching
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.DeviceState.BoilerType
import app.geeflow.domain.device.usecase.GetDeviceConstraintsUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import app.geeflow.domain.device.usecase.SetBoilerSettingsUseCase
import app.geeflow.navigation.NavEvent
import app.geeflow.navigation.destination.DeviceSettings
import app.geeflow.navigation.destination.DeviceSettings.EntryPoint
import app.geeflow.presentation.feature.device.dashboard.QuickSettings
import app.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.BrewBoilerToggled
import app.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.BrewTempChanged
import app.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.CloseClicked
import app.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.ConfirmClicked
import app.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.MoreSettingsClicked
import app.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.SteamBoilerToggled
import app.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.SteamTempChanged
import app.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsViewModelEvent.ShowSnackbar
import co.touchlab.kermit.Logger
import geeflow.core.ui.generated.resources.Res
import geeflow.core.ui.generated.resources.error_generic
import kotlinx.coroutines.Job
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
            var configApplied = false
            observeDeviceState(arguments.deviceId)
                .collect { state ->
                    if (!configApplied && state.config != null) {
                        configApplied = true
                        updateMachineState(state)
                    } else {
                        modify {
                            copy(
                                brewBoiler = brewBoiler.copy(actualTemp = state.brewBoilerTemp ?: 0f),
                                steamBoiler = steamBoiler.copy(actualTemp = state.steamBoilerTemp ?: 0f),
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
