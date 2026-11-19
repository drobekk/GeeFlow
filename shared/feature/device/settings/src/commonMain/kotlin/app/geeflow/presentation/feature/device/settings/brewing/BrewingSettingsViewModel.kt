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
import app.geeflow.domain.device.usecase.SetAgileRingUseCase
import app.geeflow.domain.device.usecase.SetBoilerSettingsUseCase
import app.geeflow.domain.device.usecase.SetManualBrewSettingsUseCase
import app.geeflow.domain.device.usecase.SetPulseHeatingModeUseCase
import app.geeflow.navigation.NavEvent
import app.geeflow.presentation.feature.device.settings.BrewingSettings
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
@Suppress("LongParameterList")
internal class BrewingSettingsViewModel(
    @InjectedParam val arguments: BrewingSettings,
    private val observeDeviceState: ObserveDeviceStateUseCase,
    private val getDeviceConstraints: GetDeviceConstraintsUseCase,
    private val setAgileRing: SetAgileRingUseCase,
    private val setBoilerSettings: SetBoilerSettingsUseCase,
    private val setPulseHeatingMode: SetPulseHeatingModeUseCase,
    private val setManualBrewSettings: SetManualBrewSettingsUseCase,
    private val observeDeviceBrewingSettings: ObserveDeviceBrewingSettingsUseCase,
    private val saveDeviceBrewingSettings: SaveDeviceBrewingSettingsUseCase,
) : BaseViewModel<BrewingSettingsViewState, BrewingSettingsViewModelEvent>(BrewingSettingsViewState()) {

    private var deviceSnapshot: DeviceSnapshot? = null
    private var savedDeviceBrewingSettings = DeviceBrewingSettings()
    private var deviceBrewingSettingsEdited = false

    private var agileRingEdited = false

    private var saveJob: Job? = null
    private var machineSettingsLoaded = false
    private var preferencesLoaded = false

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
                    updateFirmwareState(state)
                    if (!configApplied && state.config != null) {
                        configApplied = true
                        updateViewState(state)
                        machineSettingsLoaded = true
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
                preferencesLoaded = true
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

    private fun updateFirmwareState(state: DeviceState) {
        val ring = state.config?.agileRingEnabled.takeIf { state.agileRingSupported }
        if (!agileRingEdited) deviceSnapshot = deviceSnapshot?.copy(agileRingEnabled = ring)
        modify {
            copy(
                firmwareVersion = state.firmwareVersion,
                agileRingEnabled = if (agileRingEdited && ring != null) agileRingEnabled else ring,
            )
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

    fun handleEvent(event: BrewingSettingsEvent) {
        if (event is CloseClicked) {
            navigate(NavEvent.Back)
            return
        }
        val localPreference = event is AutoFlushToggled || event is AutoFlushDelayChanged ||
            event is BrewingSettingsEvent.TreatManualAsFlushChanged
        if (localPreference && !preferencesLoaded || !localPreference && !machineSettingsLoaded) return
        when (event) {
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
            is BrewingSettingsEvent.AgileRingToggled -> {
                if (viewState.value.agileRingEnabled != null) {
                    agileRingEdited = true
                    modify { copy(agileRingEnabled = event.enabled).withApplyVisible() }
                }
                Unit
            }
            is CloseClicked -> Unit
        }
        saveSettings()
    }

    private fun saveSettings() {
        if (saveJob?.isActive == true || !viewState.value.applyButtonVisible) return
        saveJob = launchCatching(onError = ::showError) {
            withContext(NonCancellable) {
                modify { copy(applyButtonLoading = true) }
                try {
                    while (viewState.value.applyButtonVisible) {
                        val draft = viewState.value
                        val previous = deviceSnapshot ?: break
                        val next = snapshotFromState(draft)
                        if (next.steamBoilerEnabled != previous.steamBoilerEnabled || next.steamTemp != previous.steamTemp) {
                            setBoilerSettings(arguments.deviceId, BoilerType.Steam, next.steamBoilerEnabled, next.steamTemp.toInt())
                            deviceSnapshot = deviceSnapshot?.copy(steamBoilerEnabled = next.steamBoilerEnabled, steamTemp = next.steamTemp)
                        }
                        if (next.brewBoilerEnabled != previous.brewBoilerEnabled || next.brewTemp != previous.brewTemp) {
                            setBoilerSettings(arguments.deviceId, BoilerType.Brew, next.brewBoilerEnabled, next.brewTemp.toInt())
                            deviceSnapshot = deviceSnapshot?.copy(brewBoilerEnabled = next.brewBoilerEnabled, brewTemp = next.brewTemp)
                        }
                        if (next.pulseHeatingEnabled != previous.pulseHeatingEnabled) {
                            setPulseHeatingMode(arguments.deviceId, next.pulseHeatingEnabled)
                            deviceSnapshot = deviceSnapshot?.copy(pulseHeatingEnabled = next.pulseHeatingEnabled)
                        }
                        if (next.paddlePressure != previous.paddlePressure || next.paddleTime != previous.paddleTime) {
                            setManualBrewSettings(arguments.deviceId, next.paddlePressure.toFloat(), next.paddleTime.toFloat())
                            deviceSnapshot = deviceSnapshot?.copy(paddlePressure = next.paddlePressure, paddleTime = next.paddleTime)
                        }
                        next.agileRingEnabled?.takeIf { it != previous.agileRingEnabled }?.let { enabled ->
                            setAgileRing(arguments.deviceId, enabled)
                            deviceSnapshot = deviceSnapshot?.copy(agileRingEnabled = enabled)
                        }
                        agileRingEdited = viewState.value.agileRingEnabled != next.agileRingEnabled
                        val settings = DeviceBrewingSettings(
                            treatManualAsFlush = draft.treatManualAsFlush,
                            autoFlushEnabled = draft.autoFlushEnabled,
                            autoFlushDelaySeconds = draft.autoFlushDelaySeconds.toInt(),
                        )
                        if (settings != savedDeviceBrewingSettings) {
                            saveDeviceBrewingSettings(arguments.deviceId, settings)
                            savedDeviceBrewingSettings = settings
                        }
                        deviceBrewingSettingsEdited = viewState.value.treatManualAsFlush != settings.treatManualAsFlush ||
                            viewState.value.autoFlushEnabled != settings.autoFlushEnabled ||
                            viewState.value.autoFlushDelaySeconds != settings.autoFlushDelaySeconds.toString()
                        deviceSnapshot = next
                        modify { withApplyVisible() }
                    }
                } finally {
                    modify { copy(applyButtonLoading = false) }
                }
            }
        }
    }

    private fun showError(throwable: Throwable) {
        Logger.e(throwable) { "Error while saving brewing settings" }
        val saved = deviceSnapshot
        agileRingEdited = false
        deviceBrewingSettingsEdited = false
        if (saved != null) modify {
            copy(
                brewBoiler = brewBoiler.copy(enabled = saved.brewBoilerEnabled, selectedTemp = saved.brewTemp),
                steamBoiler = steamBoiler.copy(enabled = saved.steamBoilerEnabled, selectedTemp = saved.steamTemp),
                pulseHeatingEnabled = saved.pulseHeatingEnabled,
                paddle = paddle.copy(pressure = saved.paddlePressure, time = saved.paddleTime),
                agileRingEnabled = saved.agileRingEnabled,
                treatManualAsFlush = saved.treatManualAsFlush,
                autoFlushEnabled = saved.autoFlushEnabled,
                autoFlushDelaySeconds = saved.autoFlushDelaySeconds,
            ).withApplyVisible()
        }
        launch { emitEvent(ShowSnackbar(getString(Res.string.error_generic))) }
    }

    private fun snapshotFromState(state: BrewingSettingsViewState) = DeviceSnapshot(
        agileRingEnabled = state.agileRingEnabled,
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
        val changed = agileRingEnabled != snapshot.agileRingEnabled ||
            brewBoiler.enabled != snapshot.brewBoilerEnabled ||
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
        val agileRingEnabled: Boolean?,
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
