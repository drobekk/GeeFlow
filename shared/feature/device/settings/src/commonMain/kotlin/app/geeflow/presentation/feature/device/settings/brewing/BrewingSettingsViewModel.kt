package app.geeflow.presentation.feature.device.settings.brewing

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.core.presentation.launchCatching
import app.geeflow.data.device.model.DeviceBrewingSettings
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.DeviceState.BoilerType
import app.geeflow.domain.device.usecase.GetDeviceConstraintsUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceBrewingSettingsUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import app.geeflow.domain.device.usecase.SaveDeviceBrewingSettingsUseCase
import app.geeflow.domain.device.usecase.SetBoilerSettingsUseCase
import app.geeflow.domain.device.usecase.SetManualBrewSettingsUseCase
import app.geeflow.domain.device.usecase.SetPulseHeatingModeUseCase
import app.geeflow.navigation.NavEvent
import app.geeflow.presentation.feature.device.settings.BrewingSettings
import app.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.ApplyClicked
import app.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.AutoFlushDelayChanged
import app.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.AutoFlushToggled
import app.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.BrewBoilerToggled
import app.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.BrewTempChanged
import app.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.CloseClicked
import app.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.PaddlePressureChanged
import app.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.PaddleTimeChanged
import app.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.PulseHeatingToggled
import app.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.SteamBoilerToggled
import app.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.SteamTempChanged
import app.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsViewModelEvent.ShowSnackbar
import co.touchlab.kermit.Logger
import geeflow.shared.core.ui.generated.resources.Res
import geeflow.shared.core.ui.generated.resources.common_settings_applied
import geeflow.shared.core.ui.generated.resources.error_generic
import kotlinx.coroutines.Job
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class BrewingSettingsViewModel(
    @InjectedParam val arguments: BrewingSettings,
    private val observeDeviceState: ObserveDeviceStateUseCase,
    private val getDeviceConstraints: GetDeviceConstraintsUseCase,
    private val setBoilerSettings: SetBoilerSettingsUseCase,
    private val setPulseHeatingMode: SetPulseHeatingModeUseCase,
    private val setManualBrewSettings: SetManualBrewSettingsUseCase,
    private val observeDeviceBrewingSettings: ObserveDeviceBrewingSettingsUseCase,
    private val saveDeviceBrewingSettings: SaveDeviceBrewingSettingsUseCase,
) : BaseViewModel<BrewingSettingsViewState, BrewingSettingsViewModelEvent>(BrewingSettingsViewState()) {

    private var deviceSnapshot: DeviceSnapshot? = null
    private var savedDeviceBrewingSettings = DeviceBrewingSettings()
    private var deviceBrewingSettingsEdited = false

    private var saveJob: Job? = null

    init {
        loadMachineState()
    }

    private fun loadMachineState() {
        launch {
            val constraints = getDeviceConstraints(arguments.deviceId)
            val brewTempList = constraints.brewTempRange.map { it.toString() }
            val steamTempList = constraints.steamTempRange.map { it.toString() }
            val pressureList = constraints.manualBrewPressureRange.map {
                "${it / DecimalScale}.${it % DecimalScale}"
            }
            val timeList = constraints.manualBrewTimeRange.map { it.toString() }
            modify {
                copy(
                    brewBoiler = brewBoiler.copy(tempList = brewTempList),
                    steamBoiler = steamBoiler.copy(tempList = steamTempList),
                    paddle = paddle.copy(pressureList = pressureList, timeList = timeList),
                )
            }
            var configApplied = false
            observeDeviceState(arguments.deviceId)
                .collect { state ->
                    if (!configApplied && state.config != null) {
                        configApplied = true
                        updateViewState(state)
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
        launch {
            observeDeviceBrewingSettings(arguments.deviceId).collect { settings ->
                savedDeviceBrewingSettings = settings
                deviceSnapshot = deviceSnapshot?.copy(
                    treatManualAsFlush = settings.treatManualAsFlush,
                    autoFlushEnabled = settings.autoFlushEnabled,
                    autoFlushDelaySeconds = settings.autoFlushDelaySeconds.toString(),
                ) ?: snapshotFromState(viewState.value).copy(
                    treatManualAsFlush = settings.treatManualAsFlush,
                    autoFlushEnabled = settings.autoFlushEnabled,
                    autoFlushDelaySeconds = settings.autoFlushDelaySeconds.toString(),
                )
                if (!deviceBrewingSettingsEdited) {
                    modify {
                        copy(
                            treatManualAsFlush = settings.treatManualAsFlush,
                            autoFlushEnabled = settings.autoFlushEnabled,
                            autoFlushDelaySeconds = settings.autoFlushDelaySeconds.toString(),
                        ).withApplyVisible()
                    }
                } else {
                    modify { withApplyVisible() }
                }
            }
        }
    }

    private fun updateViewState(state: DeviceState) {
        val config = state.config
        modify {
            copy(
                steamBoiler = steamBoiler.copy(
                    enabled = config?.steamBoilerEnabled ?: false,
                    actualTemp = state.steamBoilerTemp ?: 0f,
                    selectedTemp = config?.targetSteamTemp?.toInt()?.toString() ?: "0",
                ),
                brewBoiler = brewBoiler.copy(
                    enabled = config?.brewBoilerEnabled ?: false,
                    actualTemp = state.brewBoilerTemp ?: 0f,
                    selectedTemp = config?.targetBrewTemp?.toInt()?.toString() ?: "0",
                ),
                pulseHeatingEnabled = config?.heatingMode == DeviceState.HeatingMode.Pulse,
                paddle = paddle.copy(
                    pressure = config?.manualBrewPressure?.toString() ?: "0.0",
                    time = config?.manualBrewTimeSec?.toInt()?.toString() ?: "0",
                ),
            )
        }
        deviceSnapshot = snapshotFromState(viewState.value).copy(
            treatManualAsFlush = savedDeviceBrewingSettings.treatManualAsFlush,
            autoFlushEnabled = savedDeviceBrewingSettings.autoFlushEnabled,
            autoFlushDelaySeconds = savedDeviceBrewingSettings.autoFlushDelaySeconds.toString(),
        )
        modify { withApplyVisible() }
    }

    fun handleEvent(event: BrewingSettingsEvent) = when (event) {
        is SteamBoilerToggled -> modify {
            copy(
                steamBoiler = steamBoiler.copy(enabled = event.enabled),
            ).withApplyVisible()
        }

        is BrewBoilerToggled -> modify {
            copy(
                brewBoiler = brewBoiler.copy(enabled = event.enabled),
            ).withApplyVisible()
        }

        is SteamTempChanged -> modify {
            copy(
                steamBoiler = steamBoiler.copy(selectedTemp = event.temp),
            ).withApplyVisible()
        }

        is BrewTempChanged -> modify {
            copy(
                brewBoiler = brewBoiler.copy(selectedTemp = event.temp),
            ).withApplyVisible()
        }

        is PulseHeatingToggled -> modify { copy(pulseHeatingEnabled = event.enabled).withApplyVisible() }
        is PaddlePressureChanged -> modify { copy(paddle = paddle.copy(pressure = event.pressure)).withApplyVisible() }
        is PaddleTimeChanged -> modify { copy(paddle = paddle.copy(time = event.time)).withApplyVisible() }
        is AutoFlushToggled -> {
            deviceBrewingSettingsEdited = true
            modify { copy(autoFlushEnabled = event.enabled).withApplyVisible() }
        }
        is AutoFlushDelayChanged -> {
            deviceBrewingSettingsEdited = true
            modify { copy(autoFlushDelaySeconds = event.seconds).withApplyVisible() }
        }
        is BrewingSettingsEvent.TreatManualAsFlushChanged -> {
            deviceBrewingSettingsEdited = true
            modify { copy(treatManualAsFlush = event.enabled).withApplyVisible() }
        }
        is ApplyClicked -> saveSettings()
        is CloseClicked -> navigate(NavEvent.Back)
    }

    private fun saveSettings() = with(viewState.value) {
        val submittedState = this
        saveJob?.cancel()
        saveJob = launchCatching(
            onError = (::showError),
            block = {
                modify { copy(applyButtonLoading = true) }
                setBoilerSettings(
                    deviceId = arguments.deviceId,
                    boilerType = BoilerType.Steam,
                    enabled = steamBoiler.enabled,
                    temp = steamBoiler.selectedTemp.toInt(),
                )
                setBoilerSettings(
                    deviceId = arguments.deviceId,
                    boilerType = BoilerType.Brew,
                    enabled = brewBoiler.enabled,
                    temp = brewBoiler.selectedTemp.toInt(),
                )
                setPulseHeatingMode(arguments.deviceId, pulseHeatingEnabled)
                setManualBrewSettings(
                    deviceId = arguments.deviceId,
                    pressure = paddle.pressure.toFloat(),
                    timeSec = paddle.time.toFloat(),
                )
                val settings = DeviceBrewingSettings(
                    treatManualAsFlush = treatManualAsFlush,
                    autoFlushEnabled = autoFlushEnabled,
                    autoFlushDelaySeconds = autoFlushDelaySeconds.toInt(),
                )
                saveDeviceBrewingSettings(arguments.deviceId, settings)
                savedDeviceBrewingSettings = settings
                deviceBrewingSettingsEdited = viewState.value.treatManualAsFlush != settings.treatManualAsFlush ||
                    viewState.value.autoFlushEnabled != settings.autoFlushEnabled ||
                    viewState.value.autoFlushDelaySeconds != settings.autoFlushDelaySeconds.toString()
                deviceSnapshot = snapshotFromState(submittedState)
                modify { copy(applyButtonLoading = false).withApplyVisible() }
                emitEvent(ShowSnackbar(getString(Res.string.common_settings_applied)))
            },
        )
    }

    private fun showError(throwable: Throwable) {
        Logger.e(throwable) { "Error while saving brewing settings" }
        modify { copy(applyButtonLoading = false) }
        launch { emitEvent(ShowSnackbar(getString(Res.string.error_generic))) }
    }

    private fun snapshotFromState(state: BrewingSettingsViewState) = DeviceSnapshot(
        brewBoilerEnabled = state.brewBoiler.enabled,
        brewTemp = state.brewBoiler.selectedTemp,
        steamBoilerEnabled = state.steamBoiler.enabled,
        steamTemp = state.steamBoiler.selectedTemp,
        pulseHeatingEnabled = state.pulseHeatingEnabled,
        paddlePressure = state.paddle.pressure,
        paddleTime = state.paddle.time,
        treatManualAsFlush = state.treatManualAsFlush,
        autoFlushEnabled = state.autoFlushEnabled,
        autoFlushDelaySeconds = state.autoFlushDelaySeconds,
    )

    private fun BrewingSettingsViewState.withApplyVisible(): BrewingSettingsViewState {
        val snapshot = deviceSnapshot ?: return this
        val changed = brewBoiler.enabled != snapshot.brewBoilerEnabled ||
            brewBoiler.selectedTemp != snapshot.brewTemp ||
            steamBoiler.enabled != snapshot.steamBoilerEnabled ||
            steamBoiler.selectedTemp != snapshot.steamTemp ||
            pulseHeatingEnabled != snapshot.pulseHeatingEnabled ||
            paddle.pressure != snapshot.paddlePressure ||
            paddle.time != snapshot.paddleTime ||
            treatManualAsFlush != snapshot.treatManualAsFlush ||
            autoFlushEnabled != snapshot.autoFlushEnabled ||
            autoFlushDelaySeconds != snapshot.autoFlushDelaySeconds
        return copy(applyButtonVisible = changed)
    }

    private data class DeviceSnapshot(
        val brewBoilerEnabled: Boolean,
        val brewTemp: String,
        val steamBoilerEnabled: Boolean,
        val steamTemp: String,
        val pulseHeatingEnabled: Boolean,
        val paddlePressure: String,
        val paddleTime: String,
        val treatManualAsFlush: Boolean,
        val autoFlushEnabled: Boolean,
        val autoFlushDelaySeconds: String,
    )

    companion object {
        private const val DecimalScale = 10
    }
}
