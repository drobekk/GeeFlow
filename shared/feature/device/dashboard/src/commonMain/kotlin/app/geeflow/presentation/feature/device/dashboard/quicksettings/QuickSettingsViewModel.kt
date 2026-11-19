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
import app.geeflow.presentation.feature.device.dashboard.QuickSettings
import app.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.BrewBoilerToggled
import app.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.BrewTempChanged
import app.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.CloseClicked
import app.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.SteamBoilerToggled
import app.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsEvent.SteamTempChanged
import app.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsViewModelEvent.ShowSnackbar
import co.touchlab.kermit.Logger
import geeflow.shared.core.ui.generated.resources.Res
import geeflow.shared.core.ui.generated.resources.error_generic
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
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
    private val applied = mutableMapOf<BoilerType, Pair<Boolean, String>>()

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
                        applied[BoilerType.Brew] = desired(BoilerType.Brew)
                        applied[BoilerType.Steam] = desired(BoilerType.Steam)
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

    fun handleEvent(event: QuickSettingsEvent) {
        if (event is CloseClicked) {
            navigate(NavEvent.Back)
            return
        }
        if (applied.isEmpty()) return
        when (event) {
            is SteamBoilerToggled -> modify { copy(steamBoiler = steamBoiler.copy(enabled = event.enabled)) }
            is BrewBoilerToggled -> modify { copy(brewBoiler = brewBoiler.copy(enabled = event.enabled)) }
            is SteamTempChanged -> modify { copy(steamBoiler = steamBoiler.copy(selectedTemp = event.temp)) }
            is BrewTempChanged -> modify { copy(brewBoiler = brewBoiler.copy(selectedTemp = event.temp)) }
            is CloseClicked -> Unit
        }
        saveSettings()
    }

    private fun desired(type: BoilerType): Pair<Boolean, String> = with(viewState.value) {
        val boiler = if (type == BoilerType.Brew) brewBoiler else steamBoiler
        boiler.enabled to boiler.selectedTemp
    }

    private fun saveSettings() {
        if (applyJob?.isActive == true) return
        applyJob = launchCatching(onError = ::showError) {
            withContext(NonCancellable) {
                while (true) {
                    val type = applied.keys.firstOrNull { applied[it] != desired(it) } ?: break
                    val request = desired(type)
                    setBoilerSettings(arguments.deviceId, type, request.first, request.second.toInt())
                    applied[type] = request
                }
            }
        }
    }

    private fun showError(throwable: Throwable) {
        modify {
            val brew = applied[BoilerType.Brew]
            val steam = applied[BoilerType.Steam]
            copy(
                brewBoiler = brew?.let { brewBoiler.copy(enabled = it.first, selectedTemp = it.second) } ?: brewBoiler,
                steamBoiler = steam?.let { steamBoiler.copy(enabled = it.first, selectedTemp = it.second) } ?: steamBoiler,
            )
        }
        Logger.e(throwable) { "Error while updating quick settings" }
        launch { emitEvent(ShowSnackbar(getString(Res.string.error_generic))) }
    }
}
