@file:Suppress("TooManyFunctions", "LongParameterList")

package app.geeflow.presentation.feature.device.dashboard.main

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.core.presentation.launchCatching
import app.geeflow.core.presentation.toUserMessage
import app.geeflow.data.brew.model.BrewSession
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.DeviceState.BrewStatus
import app.geeflow.data.device.model.DeviceState.ConnectionStatus
import app.geeflow.data.device.model.isDemo
import app.geeflow.data.user.model.ChartType
import app.geeflow.domain.brew.usecase.GetBrewProfileUseCase
import app.geeflow.domain.brew.usecase.ObserveBrewDataUseCase
import app.geeflow.domain.device.usecase.ConnectDeviceUseCase
import app.geeflow.domain.device.usecase.DisconnectDeviceUseCase
import app.geeflow.domain.device.usecase.GetDeviceUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import app.geeflow.domain.device.usecase.StartManualBrewingUseCase
import app.geeflow.domain.device.usecase.StartProfileBrewingUseCase
import app.geeflow.domain.device.usecase.StopBrewingUseCase
import app.geeflow.domain.exception.DeviceNotConnectedException
import app.geeflow.domain.user.usecase.GetVisibleChartsUseCase
import app.geeflow.domain.user.usecase.ToggleChartVisibilityUseCase
import app.geeflow.navigation.NavEvent.To
import app.geeflow.navigation.destination.DeviceDashboard
import app.geeflow.navigation.destination.DeviceList
import app.geeflow.navigation.destination.DeviceSettings
import app.geeflow.navigation.destination.DeviceSettings.EntryPoint
import app.geeflow.navigation.destination.UserSettings
import app.geeflow.platform.permissions.DeniedException
import app.geeflow.platform.permissions.PermissionBluetoothConnect
import app.geeflow.platform.permissions.PermissionBluetoothScan
import app.geeflow.platform.permissions.PermissionsController
import app.geeflow.presentation.feature.device.dashboard.QuickMaintenance
import app.geeflow.presentation.feature.device.dashboard.QuickSettings
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Brew
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Device
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Dialog
import app.geeflow.presentation.feature.device.dashboard.model.ChartData
import co.touchlab.kermit.Logger
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.math.pow
import kotlin.math.round
import app.geeflow.data.device.model.Device as Machine

@KoinViewModel
internal class DeviceDashboardViewModel(
    @InjectedParam private val args: DeviceDashboard,
    @InjectedParam val permissionsController: PermissionsController,
    getDevice: GetDeviceUseCase,
    private val observeDeviceState: ObserveDeviceStateUseCase,
    private val connectDevice: ConnectDeviceUseCase,
    private val disconnectDevice: DisconnectDeviceUseCase,
    private val startManualBrewing: StartManualBrewingUseCase,
    private val startProfileBrewing: StartProfileBrewingUseCase,
    private val stopBrewing: StopBrewingUseCase,
    private val observeBrewData: ObserveBrewDataUseCase,
    private val getVisibleCharts: GetVisibleChartsUseCase,
    private val toggleChartVisibility: ToggleChartVisibilityUseCase,
    private val getBrewProfileUseCase: GetBrewProfileUseCase,
) : BaseViewModel<DeviceDashboardViewState, DeviceDashboardViewModelEvent>(DeviceDashboardViewState()) {

    private var selectedProfileId: String? = null
    private var machine: Machine? = null

    init {
        machine = getDevice(args.deviceId)
        modify { copy(device = device.copy(id = args.deviceId, name = machine?.name ?: args.deviceId.toString())) }
        launch { observeDeviceState(args.deviceId).collect { state -> updateMachineStateUi(state) } }
        launch { getVisibleCharts().collect(::chartsVisibilityChanged) }
        launch { observeBrewData(args.deviceId).collect(::brewSessionDataChanged) }
    }

    @Suppress("CyclomaticComplexMethod")
    fun handleEvent(event: DeviceDashboardEvent) = when (event) {
        is DeviceDashboardEvent.ToggleChartVisibility -> launch { toggleChartVisibility(event.type.toDomain()) }
        is DeviceDashboardEvent.ConnectionButtonClicked -> toggleConnection()
        is DeviceDashboardEvent.DeviceClicked -> navigate(To(DeviceList))
        is DeviceDashboardEvent.QuickSettingsClicked -> withDeviceConnected {
            navigate(To(QuickSettings(args.deviceId)))
        }

        is DeviceDashboardEvent.UserClicked -> navigate(To(UserSettings))
        is DeviceDashboardEvent.ConnectedDevicesClicked -> withDeviceConnected {
            navigate(To(DeviceSettings(args.deviceId, EntryPoint.Connectivity)))
        }

        is DeviceDashboardEvent.CleaningClicked -> withDeviceConnected { navigate(To(QuickMaintenance(args.deviceId))) }
        is DeviceDashboardEvent.DialogDismissed -> modify { copy(dialog = null) }
        is DeviceDashboardEvent.OpenSystemSettingsClicked -> permissionsController.openAppSettings()
        is DeviceDashboardEvent.ManualBrewClicked -> launchCatching(::onError) { startManualBrewing(args.deviceId) }
        is DeviceDashboardEvent.StopBrewClicked -> launchCatching(::onError) { stopBrewing(args.deviceId) }
        is DeviceDashboardEvent.FlowControlClicked -> Unit // TODO
        is DeviceDashboardEvent.BrewClicked -> startProfile()
        is DeviceDashboardEvent.PermissionDialogResumed -> withBluetoothPermissions { modify { copy(dialog = null) } }
        is DeviceDashboardEvent.ProfileSelected -> onProfileSelected(event.id)
        is DeviceDashboardEvent.Resumed -> connect()
        is DeviceDashboardEvent.AlarmClicked -> navigate(To(QuickMaintenance(args.deviceId)))
    }

    private fun toggleConnection() {
        if (viewState.value.device.connectionStatus == Device.ConnectionStatus.Connected) {
            disconnectDevice(args.deviceId)
        } else {
            withBluetoothPermissions {
                connectDevice(args.deviceId)
            }
        }
    }

    private fun connect() {
        withBluetoothPermissions {
            if (viewState.value.device.connectionStatus == Device.ConnectionStatus.Disconnected) {
                connectDevice(args.deviceId)
            }
        }
    }

    private fun startProfile() = launchCatching(::onError) {
        selectedProfileId
            ?.toLongOrNull()
            ?.let { getBrewProfileUseCase(it) }
            ?.let { profile ->
                startProfileBrewing(args.deviceId, profile)
                emitEvent(DeviceDashboardViewModelEvent.SwitchToDetails)
            }
    }

    private fun onProfileSelected(id: String?) {
        selectedProfileId = id
        selectedProfileId
            ?.toLongOrNull()
            ?.let { getBrewProfileUseCase(it) }
            ?.let { modify { copy(brew = Brew(it.name)) } }
    }

    private fun updateMachineStateUi(state: DeviceState) {
        val wasConnected = viewState.value.device.connectionStatus == Device.ConnectionStatus.Connected
        val isNowConnected = state.connectionStatus == ConnectionStatus.Connected
        if (!wasConnected && isNowConnected && state.waterLevelAlarm) {
            navigate(To(QuickMaintenance(args.deviceId)))
        }
        modify {
            val newBrewStatus = when (state.brewStatus) {
                BrewStatus.Manual -> Device.BrewStatus.Manual
                BrewStatus.Profile -> Device.BrewStatus.Profile
                else -> Device.BrewStatus.Idle
            }
            copy(
                device = device.copy(
                    brewBoilerTemp = state.brewBoilerTemp?.roundDecimalsTo(1)?.toString(),
                    steamBoilerTemp = state.steamBoilerTemp?.roundDecimalsTo(1)?.toString(),
                    pressure = state.pressure?.roundDecimalsTo(1)?.toString(),
                    connectionStatus = when (state.connectionStatus) {
                        ConnectionStatus.Disconnected -> Device.ConnectionStatus.Disconnected
                        ConnectionStatus.Connecting -> Device.ConnectionStatus.Connecting
                        ConnectionStatus.Synchronizing -> Device.ConnectionStatus.Synchronizing
                        ConnectionStatus.Connected -> Device.ConnectionStatus.Connected
                    },
                    brewStatus = newBrewStatus,
                    smartScaleConnected = state.smartScale?.isConnected == true,
                    alarm = state.waterLevelAlarm,
                ),
            )
        }
    }

    private fun brewSessionDataChanged(session: BrewSession) = modify {
        copy(
            brew = brew.copy(
                time = session.elapsedSeconds,
                data = session.dataPoints.mapValues { (_, point) ->
                    ChartData(
                        pressure = point.pressure,
                        weight = point.weight,
                        weightPerSecond = point.weightRate,
                        volume = point.volume,
                        volumePerSecond = point.flowRate,
                    )
                },
            ),
        )
    }

    private fun chartsVisibilityChanged(charts: Set<ChartType>) = modify {
        copy(
            visibleCharts = charts.map {
                when (it) {
                    ChartType.PRESSURE -> DashboardChartType.Pressure
                    ChartType.FLOW_RATE -> DashboardChartType.FlowRate
                    ChartType.WEIGHT_RATE -> DashboardChartType.WeightRate
                    ChartType.VOLUME -> DashboardChartType.Volume
                    ChartType.WEIGHT -> DashboardChartType.Weight
                }
            }.toSet(),
        )
    }

    private fun withBluetoothPermissions(block: suspend () -> Unit) = launch {
        try {
            if (machine?.isDemo == false) {
                permissionsController.providePermission(PermissionBluetoothScan)
                permissionsController.providePermission(PermissionBluetoothConnect)
            }
            block()
        } catch (_: DeniedException) {
            showBluetoothPermissionMissingDialog()
        }
    }

    private fun withDeviceConnected(block: () -> Unit) {
        if (viewState.value.device.connectionStatus == Device.ConnectionStatus.Connected) {
            block()
        } else {
            onError(DeviceNotConnectedException(args.deviceId))
        }
    }

    private fun showBluetoothPermissionMissingDialog() {
        modify { copy(dialog = Dialog.BluetoothPermissionMissing) }
    }

    private fun onError(throwable: Throwable) {
        Logger.e(throwable = throwable) { "${this::class.simpleName}" }
        launch { emitEvent(DeviceDashboardViewModelEvent.ShowSnackbar(throwable.toUserMessage())) }
    }

    private fun DashboardChartType.toDomain() = when (this) {
        DashboardChartType.Pressure -> ChartType.PRESSURE
        DashboardChartType.FlowRate -> ChartType.FLOW_RATE
        DashboardChartType.WeightRate -> ChartType.WEIGHT_RATE
        DashboardChartType.Volume -> ChartType.VOLUME
        DashboardChartType.Weight -> ChartType.WEIGHT
    }
}

private fun Float.roundDecimalsTo(decimals: Int): Float {
    val factor = 10f.pow(decimals)
    return round(this * factor) / factor
}
