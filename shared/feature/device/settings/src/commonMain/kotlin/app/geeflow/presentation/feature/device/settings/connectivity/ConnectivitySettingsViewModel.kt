package app.geeflow.presentation.feature.device.settings.connectivity

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.core.presentation.launchCatching
import app.geeflow.domain.device.usecase.ConnectSingleDoseGrinderUseCase
import app.geeflow.domain.device.usecase.ConnectSmartScaleUseCase
import app.geeflow.domain.device.usecase.DisconnectSingleDoseGrinderUseCase
import app.geeflow.domain.device.usecase.DisconnectSmartScaleUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import app.geeflow.domain.device.usecase.ObserveFoundScalesUseCase
import app.geeflow.domain.device.usecase.ObserveFoundSingleDoseGrindersUseCase
import app.geeflow.domain.device.usecase.RequestSingleDoseGrinderListUseCase
import app.geeflow.domain.device.usecase.RequestSmartScaleListUseCase
import app.geeflow.domain.device.usecase.SetSingleDoseGrinderConnectivityUseCase
import app.geeflow.domain.device.usecase.SetSmartScaleConnectivityUseCase
import app.geeflow.navigation.NavEvent
import app.geeflow.presentation.feature.device.settings.ConnectivitySettings
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivityAccessoryType.SingleDose
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivityAccessoryType.SmartScale
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.CloseClicked
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.ConnectionClicked
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.RescanClicked
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.SearchToggled
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.TypeSelected
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsViewModelEvent.ShowSnackbar
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsViewState.AccessoryConnectionStatus
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsViewState.AccessoryState
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsViewState.AccessoryViewItem
import geeflow.shared.feature.device.settings.generated.resources.Res
import geeflow.shared.feature.device.settings.generated.resources.device_settings_connectivity_connection_help
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
@Suppress("LongParameterList")
internal class ConnectivitySettingsViewModel(
    @InjectedParam val arguments: ConnectivitySettings,
    private val observeDeviceState: ObserveDeviceStateUseCase,
    private val observeFoundScales: ObserveFoundScalesUseCase,
    private val setSmartScaleConnectivity: SetSmartScaleConnectivityUseCase,
    private val requestSmartScaleList: RequestSmartScaleListUseCase,
    private val connectSmartScale: ConnectSmartScaleUseCase,
    private val disconnectSmartScale: DisconnectSmartScaleUseCase,
    private val observeFoundGrinders: ObserveFoundSingleDoseGrindersUseCase,
    private val setGrinderConnectivity: SetSingleDoseGrinderConnectivityUseCase,
    private val requestGrinderList: RequestSingleDoseGrinderListUseCase,
    private val connectGrinder: ConnectSingleDoseGrinderUseCase,
    private val disconnectGrinder: DisconnectSingleDoseGrinderUseCase,
) : BaseViewModel<ConnectivitySettingsViewState, ConnectivitySettingsViewModelEvent>(ConnectivitySettingsViewState()) {

    private val connecting = MutableStateFlow<Map<ConnectivityAccessoryType, String>>(emptyMap())
    private val connectionJobs = mutableMapOf<ConnectivityAccessoryType, Job>()

    init {
        observeState()
    }

    private fun observeState() {
        launch {
            combine(
                observeDeviceState(arguments.deviceId),
                observeFoundScales(arguments.deviceId),
                observeFoundGrinders(arguments.deviceId),
                connecting,
            ) { state, scales, grinders, pending ->
                val scaleName = state.smartScale?.name
                val grinderName = state.singleDoseGrinder?.name
                finishConnection(SmartScale, state.smartScaleEnabled, scaleName)
                finishConnection(SingleDose, state.singleDoseGrinderEnabled, grinderName)
                AccessoryState(
                    enabled = state.smartScaleEnabled,
                    isSearching = state.smartScaleSearchActive,
                    devices = if (state.smartScaleEnabled) {
                        scales.map { viewItem(it.name, pending[SmartScale], scaleName) }
                    } else {
                        emptyList()
                    },
                ) to AccessoryState(
                    enabled = state.singleDoseGrinderEnabled,
                    isSearching = state.singleDoseGrinderSearchActive,
                    devices = if (state.singleDoseGrinderEnabled) {
                        grinders.map { viewItem(it.name, pending[SingleDose], grinderName) }
                    } else {
                        emptyList()
                    },
                )
            }.collect { (scale, grinder) -> modify { copy(scale = scale, grinder = grinder) } }
        }
    }

    fun handleEvent(event: ConnectivitySettingsEvent) {
        val type = viewState.value.selectedType
        when (event) {
            is TypeSelected -> modify { copy(selectedType = event.type) }
            is SearchToggled -> {
                if (!event.enabled) stopConnecting(type)
                launchCatching(onError = { showConnectionHelp() }) {
                    when (type) {
                        SmartScale -> setSmartScaleConnectivity(arguments.deviceId, event.enabled)
                        SingleDose -> setGrinderConnectivity(arguments.deviceId, event.enabled)
                    }
                }
            }
            is ConnectionClicked -> onConnectionClicked(type, event.name)
            RescanClicked -> launchCatching(onError = { showConnectionHelp() }) {
                when (type) {
                    SmartScale -> requestSmartScaleList(arguments.deviceId)
                    SingleDose -> requestGrinderList(arguments.deviceId)
                }
            }
            CloseClicked -> navigate(NavEvent.Back)
        }
    }

    private fun onConnectionClicked(type: ConnectivityAccessoryType, name: String) {
        val accessory = viewState.value.selectedAccessory
        if (!accessory.enabled || connecting.value.containsKey(type)) return
        val device = accessory.devices.firstOrNull { it.name == name } ?: return
        if (device.connectionStatus == AccessoryConnectionStatus.Connected) {
            launchCatching(onError = { showConnectionHelp() }) {
                when (type) {
                    SmartScale -> disconnectSmartScale(arguments.deviceId)
                    SingleDose -> disconnectGrinder(arguments.deviceId)
                }
            }
        } else {
            connecting.update { it + (type to name) }
            connectionJobs[type] = launchCatching(onError = {
                stopConnecting(type)
                showConnectionHelp()
            }) {
                when (type) {
                    SmartScale -> connectSmartScale(arguments.deviceId, name)
                    SingleDose -> connectGrinder(arguments.deviceId, name)
                }
                delay(ConnectionTimeoutMs)
                connecting.update { it - type }
                showConnectionHelp()
            }
        }
    }

    private fun finishConnection(type: ConnectivityAccessoryType, enabled: Boolean, connectedName: String?) {
        val name = connecting.value[type] ?: return
        if (!enabled || name == connectedName) stopConnecting(type)
    }

    private fun stopConnecting(type: ConnectivityAccessoryType) {
        connecting.update { it - type }
        connectionJobs.remove(type)?.cancel()
    }

    private fun showConnectionHelp() {
        launch { emitEvent(ShowSnackbar(getString(Res.string.device_settings_connectivity_connection_help))) }
    }

    private fun viewItem(name: String, connectingName: String?, connectedName: String?) = AccessoryViewItem(
        name = name,
        connectionStatus = when (name) {
            connectedName -> AccessoryConnectionStatus.Connected
            connectingName -> AccessoryConnectionStatus.Connecting
            else -> AccessoryConnectionStatus.Disconnected
        },
    )

    private companion object {
        const val ConnectionTimeoutMs = 10_000L
    }
}
