package app.geeflow.presentation.feature.device.settings.connectivity

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.core.presentation.launchCatching
import app.geeflow.data.device.model.SmartScale
import app.geeflow.domain.device.usecase.ConnectSmartScaleUseCase
import app.geeflow.domain.device.usecase.DisconnectSmartScaleUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import app.geeflow.domain.device.usecase.ObserveFoundScalesUseCase
import app.geeflow.domain.device.usecase.RequestSmartScaleListUseCase
import app.geeflow.domain.device.usecase.SetSmartScaleConnectivityUseCase
import app.geeflow.navigation.NavEvent
import app.geeflow.presentation.feature.device.settings.ConnectivitySettings
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.CloseClicked
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.RescanClicked
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.ScaleConnectionClicked
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.SmartScaleToggled
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsViewState.ScaleConnectionStatus
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsViewState.ScaleViewItem
import kotlinx.coroutines.flow.combine
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

    private var connectingScaleName: String? = null

    init {
        observeState()
    }

    private fun observeState() {
        launch {
            combine(
                observeDeviceState(arguments.deviceId),
                observeFoundScales(arguments.deviceId),
            ) { state, scales -> state to scales }
                .collect { (state, scales) ->
                    val connectedName = state.smartScale?.name
                    if (connectingScaleName != null && connectedName == connectingScaleName) {
                        connectingScaleName = null
                    }
                    if (!state.smartScaleEnabled) connectingScaleName = null
                    modify {
                        copy(
                            smartScaleEnabled = state.smartScaleEnabled,
                            isSearching = state.smartScaleSearchActive,
                            scales = if (state.smartScaleEnabled) {
                                scales.map { it.toViewItem(connectingScaleName, connectedName) }
                            } else {
                                emptyList()
                            },
                        )
                    }
                }
        }
    }

    fun handleEvent(event: ConnectivitySettingsEvent) = when (event) {
        is SmartScaleToggled -> launchCatching { setSmartScaleConnectivity(arguments.deviceId, event.enabled) }
        is ScaleConnectionClicked -> onScaleConnectionClicked(event.scaleName)
        is RescanClicked -> launchCatching { requestSmartScaleList(arguments.deviceId) }
        is CloseClicked -> navigate(NavEvent.Back)
    }

    private fun onScaleConnectionClicked(scaleName: String) {
        val scale = viewState.value.scales.firstOrNull { it.name == scaleName } ?: return
        when (scale.connectionStatus) {
            ScaleConnectionStatus.Connected -> launchCatching {
                disconnectSmartScale(arguments.deviceId)
            }

            ScaleConnectionStatus.Disconnected -> {
                connectingScaleName = scaleName
                modify {
                    copy(
                        scales = scales.map {
                            if (it.name == scaleName) {
                                it.copy(
                                    connectionStatus = ScaleConnectionStatus.Connecting,
                                )
                            } else {
                                it
                            }
                        },
                    )
                }
                launchCatching { connectSmartScale(arguments.deviceId, scaleName) }
            }

            ScaleConnectionStatus.Connecting -> Unit
        }
    }

    private fun SmartScale.toViewItem(connectingName: String?, connectedName: String?) = ScaleViewItem(
        name = name,
        connectionStatus = when (name) {
            connectedName -> ScaleConnectionStatus.Connected
            connectingName -> ScaleConnectionStatus.Connecting
            else -> ScaleConnectionStatus.Disconnected
        },
    )
}
