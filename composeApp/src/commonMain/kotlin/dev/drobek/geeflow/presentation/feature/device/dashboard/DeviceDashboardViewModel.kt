package dev.drobek.geeflow.presentation.feature.device.dashboard

import dev.drobek.geeflow.domain.brew.model.BrewSession
import dev.drobek.geeflow.domain.brew.usecase.GetBrewProfileUseCase
import dev.drobek.geeflow.domain.brew.usecase.ObserveBrewDataUseCase
import dev.drobek.geeflow.domain.device.model.MachineState
import dev.drobek.geeflow.domain.device.usecase.ConnectDeviceUseCase
import dev.drobek.geeflow.domain.device.usecase.DisconnectDeviceUseCase
import dev.drobek.geeflow.domain.device.usecase.GetDeviceUseCase
import dev.drobek.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import dev.drobek.geeflow.domain.device.usecase.StartManualBrewingUseCase
import dev.drobek.geeflow.domain.device.usecase.StartProfileBrewingUseCase
import dev.drobek.geeflow.domain.device.usecase.StopBrewingUseCase
import dev.drobek.geeflow.domain.user.ChartType
import dev.drobek.geeflow.domain.user.usecase.GetVisibleChartsUseCase
import dev.drobek.geeflow.domain.user.usecase.ToggleChartVisibilityUseCase
import dev.drobek.geeflow.platform.permissions.DeniedException
import dev.drobek.geeflow.platform.permissions.PermissionBluetoothConnect
import dev.drobek.geeflow.platform.permissions.PermissionBluetoothScan
import dev.drobek.geeflow.platform.permissions.PermissionsController
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.BrewClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.CleaningClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.ConnectedDevicesClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.ConnectionButtonClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.DeviceClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.DialogDismissed
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.FlowControlClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.ManualBrewClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.OpenSystemSettingsClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.PermissionDialogResumed
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.ProfileSelected
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.Resumed
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.SettingsClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.StopBrewClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.ToggleChartVisibility
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.UserClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewModelEvent.ShowSnackbar
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Brew
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.DashboardChartType
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device.BrewStatus.Idle
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device.BrewStatus.Manual
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device.BrewStatus.Profile
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Dialog
import dev.drobek.geeflow.presentation.feature.device.dashboard.model.ChartData
import dev.drobek.geeflow.presentation.feature.device.dashboard.navigation.DeviceDashboardDestinations.Dashboard
import dev.drobek.geeflow.viewmodel.BaseViewModel
import geeflow.composeapp.generated.resources.Res
import geeflow.composeapp.generated.resources.error_generic
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.math.pow
import kotlin.math.round

@KoinViewModel
internal class DeviceDashboardViewModel(
    @InjectedParam private val args: Dashboard,
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
    private val getBrewProfileUseCase: GetBrewProfileUseCase
) : BaseViewModel<DeviceDashboardViewState, DeviceDashboardViewModelEvent>(DeviceDashboardViewState()) {

    private var selectedProfileId: String? = null

    init {
        val machine = getDevice(args.deviceId)
        modify { copy(device = device.copy(id = args.deviceId, name = machine?.name ?: args.deviceId)) }
        launch { observeDeviceState(args.deviceId).collect { state -> updateMachineStateUi(state) } }
        launch { getVisibleCharts().collect(::chartsVisibilityChanged) }
        launch { observeBrewData(args.deviceId).collect(::brewSessionDataChanged) }
    }

    fun handleEvent(event: DeviceDashboardEvent) = when (event) {
        is ToggleChartVisibility -> launch { toggleChartVisibility(event.type.toDomain()) }
        is ConnectionButtonClicked -> toggleConnection()
        is DeviceClicked -> emitEvent(Navigation.DeviceList)
        is SettingsClicked -> emitEvent(Navigation.Settings(args.deviceId))
        is UserClicked -> Unit
        is ConnectedDevicesClicked -> Unit
        is CleaningClicked -> emitEvent(Navigation.Clean(args.deviceId))
        is DialogDismissed -> modify { copy(dialog = null) }
        is OpenSystemSettingsClicked -> permissionsController.openAppSettings()
        is ManualBrewClicked -> launchCatching(::onError) { startManualBrewing(args.deviceId) }
        is StopBrewClicked -> launchCatching(::onError) { stopBrewing(args.deviceId) }
        is FlowControlClicked -> Unit // TODO
        is BrewClicked -> startProfile()
        is PermissionDialogResumed -> withBluetoothPermissions { modify { copy(dialog = null) } }
        is ProfileSelected -> onProfileSelected(event.id)
        is Resumed -> connect()
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
            ?.let { profile -> startProfileBrewing(args.deviceId, profile) }
    }

    private fun onProfileSelected(id: String?) {
        selectedProfileId = id
        selectedProfileId
            ?.toLongOrNull()
            ?.let { getBrewProfileUseCase(it) }
            ?.let { modify { copy( brew = Brew(it.name)) } }
    }

    private fun updateMachineStateUi(state: MachineState) = modify {
        val newBrewStatus = when (state.brewStatus) {
            MachineState.BrewStatus.Manual -> Manual
            MachineState.BrewStatus.Profile -> Profile
            else -> Idle
        }
        copy(
            device = device.copy(
                brewBoilerTemp = state.brewBoilerTemp?.roundDecimalsTo(1)?.toString(),
                steamBoilerTemp = state.steamBoilerTemp?.roundDecimalsTo(1)?.toString(),
                pressure = state.pressure?.roundDecimalsTo(1)?.toString(),
                connectionStatus = when (state.connectionStatus) {
                    MachineState.ConnectionStatus.Disconnected -> Device.ConnectionStatus.Disconnected
                    MachineState.ConnectionStatus.Connecting -> Device.ConnectionStatus.Connecting
                    MachineState.ConnectionStatus.Connected -> Device.ConnectionStatus.Connected
                },
                brewStatus = newBrewStatus
            )
        )
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
                }
            )
        )
    }

    private fun chartsVisibilityChanged(charts: Set<ChartType>) = modify {
        copy(visibleCharts = charts.map {
            when (it) {
                ChartType.PRESSURE -> DashboardChartType.Pressure
                ChartType.FLOW_RATE -> DashboardChartType.FlowRate
                ChartType.WEIGHT_RATE -> DashboardChartType.WeightRate
                ChartType.VOLUME -> DashboardChartType.Volume
                ChartType.WEIGHT -> DashboardChartType.Weight

            }
        }.toSet())
    }

    private fun withBluetoothPermissions(block: suspend () -> Unit) = launch {
        try {
            permissionsController.providePermission(PermissionBluetoothScan)
            permissionsController.providePermission(PermissionBluetoothConnect)
            block()
        } catch (_: DeniedException) {
            showBluetoothPermissionMissingDialog()
        }
    }

    private fun showBluetoothPermissionMissingDialog() {
        modify { copy(dialog = Dialog.BluetoothPermissionMissing) }
    }

    private fun onError(throwable: Throwable) {
        emitEvent { ShowSnackbar(getString(Res.string.error_generic)) }
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
