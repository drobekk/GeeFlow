package app.geeflow.presentation.feature.device.settings.connectivity

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.core.presentation.launchCatching
import app.geeflow.domain.device.usecase.ConnectSmartScaleUseCase
import app.geeflow.domain.device.usecase.DisconnectSmartScaleUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import app.geeflow.domain.device.usecase.ObserveFoundScalesUseCase
import app.geeflow.domain.device.usecase.RequestSmartScaleListUseCase
import app.geeflow.domain.device.usecase.SetSmartScaleConnectivityUseCase
import app.geeflow.navigation.NavEvent
import app.geeflow.presentation.feature.device.settings.ConnectivitySettings
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.CloseClicked
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.ConnectionClicked
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.RescanClicked
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.SearchToggled
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
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class ConnectivitySettingsViewModel(
    @InjectedParam val arguments: ConnectivitySettings,
    private val observeDeviceState: ObserveDeviceStateUseCase,
    private val observeFoundScales: ObserveFoundScalesUseCase,
    private val setSmartScaleConnectivity: SetSmartScaleConnectivityUseCase,
    private val requestSmartScaleList: RequestSmartScaleListUseCase,
    private val connectSmartScale: ConnectSmartScaleUseCase,
    private val disconnectSmartScale: DisconnectSmartScaleUseCase,
) : BaseViewModel<ConnectivitySettingsViewState, ConnectivitySettingsViewModelEvent>(ConnectivitySettingsViewState()) {

    private val connecting = MutableStateFlow<String?>(null)
    private var connectionJob: Job? = null

    init {
        observeState()
    }

    private fun observeState() {
        launch {
            combine(
                observeDeviceState(arguments.deviceId),
                observeFoundScales(arguments.deviceId),
                connecting,
            ) { state, scales, pending ->
                val connectedName = state.smartScale?.name
                val pendingName = pending
                if (!state.smartScaleEnabled || pendingName == connectedName) stopConnecting()
                AccessoryState(
                    enabled = state.smartScaleEnabled,
                    isSearching = state.smartScaleSearchActive,
                    devices = if (state.smartScaleEnabled) {
                        scales.map { viewItem(it.name, pendingName, connectedName) }
                    } else {
                        emptyList()
                    },
                )
            }.collect { scale -> modify { copy(scale = scale) } }
        }
    }

    fun handleEvent(event: ConnectivitySettingsEvent) {
        when (event) {
            is SearchToggled -> {
                if (!event.enabled) stopConnecting()
                launchCatching(onError = { showConnectionHelp() }) {
                    setSmartScaleConnectivity(arguments.deviceId, event.enabled)
                }
            }
            is ConnectionClicked -> onConnectionClicked(event.name)
            RescanClicked -> launchCatching(onError = { showConnectionHelp() }) {
                requestSmartScaleList(arguments.deviceId)
            }
            CloseClicked -> navigate(NavEvent.Back)
        }
    }

    private fun onConnectionClicked(name: String) {
        val accessory = viewState.value.scale
        if (!accessory.enabled || connecting.value != null) return
        val device = accessory.devices.firstOrNull { it.name == name } ?: return
        if (device.connectionStatus == AccessoryConnectionStatus.Connected) {
            launchCatching(onError = { showConnectionHelp() }) {
                disconnectSmartScale(arguments.deviceId)
            }
        } else {
            connecting.value = name
            connectionJob = launchCatching(onError = {
                stopConnecting()
                showConnectionHelp()
            }) {
                connectSmartScale(arguments.deviceId, name)
                delay(ConnectionTimeoutMs)
                connecting.value = null
                showConnectionHelp()
            }
        }
    }

    private fun stopConnecting() {
        connecting.value = null
        connectionJob?.cancel()
        connectionJob = null
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
