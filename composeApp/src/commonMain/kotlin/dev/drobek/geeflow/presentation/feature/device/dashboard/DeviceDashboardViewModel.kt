package dev.drobek.geeflow.presentation.feature.device.dashboard

import dev.drobek.geeflow.data.device.api.DeviceController
import dev.drobek.geeflow.domain.brew.usecase.ObserveBrewDataUseCase
import dev.drobek.geeflow.domain.device.model.MachineState
import dev.drobek.geeflow.domain.device.model.MachineState.ConnectionStatus
import dev.drobek.geeflow.domain.device.usecase.GetDeviceUseCase
import dev.drobek.geeflow.domain.user.ChartType
import dev.drobek.geeflow.domain.user.usecase.GetVisibleChartsUseCase
import dev.drobek.geeflow.domain.user.usecase.ToggleChartVisibilityUseCase
import dev.drobek.geeflow.platform.permissions.DeniedException
import dev.drobek.geeflow.platform.permissions.PermissionBluetoothConnect
import dev.drobek.geeflow.platform.permissions.PermissionBluetoothScan
import dev.drobek.geeflow.platform.permissions.PermissionsController
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.CleaningClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.ConnectedDevicesClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.ConnectionButtonClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.DeviceClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.DialogDismissed
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.FlowControlClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.ManualBrewClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.OpenSystemSettingsClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.PermissionDialogResumed
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.SettingsClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.StopBrewClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.ToggleChartVisibility
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.UserClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Brew.Data
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.DashboardChartType
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device.BrewStatus.Idle
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device.BrewStatus.Manual
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device.BrewStatus.Profile
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device.ConnectionStatus.Connected
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device.ConnectionStatus.Connecting
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device.ConnectionStatus.Disconnected
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Dialog.BluetoothPermissionMissing
import dev.drobek.geeflow.presentation.feature.device.dashboard.navigation.DeviceDestinations.DeviceDashboard
import dev.drobek.geeflow.viewmodel.BaseViewModel
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class DeviceDashboardViewModel(
    getDevice: GetDeviceUseCase,
    val permissionsController: PermissionsController,
    private val args: DeviceDashboard,
    private val deviceController: DeviceController,
    private val observeBrewData: ObserveBrewDataUseCase,
    private val getVisibleCharts: GetVisibleChartsUseCase,
    private val toggleChartVisibility: ToggleChartVisibilityUseCase
) : BaseViewModel<DeviceDashboardViewState, DeviceLitViewModelEvent>(DeviceDashboardViewState()) {

    private var macAddress: String? = null

    init {
        val machine = getDevice(args.id)
        macAddress = machine?.macAddress
        modify { copy(device = device.copy(id = args.id, name = machine?.name ?: args.id)) }
        launch {
            deviceController.machineState.collect { state -> updateMachineStateUi(state) }
        }
        launch {
            getVisibleCharts().collect { charts ->
                modify { copy(visibleCharts = charts.map { it.toPresentation() }.toSet()) }
            }
        }
        launch {
            observeBrewData().collect { dataMap ->
                modify {
                    copy(
                        brew = brew.copy(
                            data = dataMap.mapValues { (_, point) ->
                                Data(
                                    pressure = point.pressure,
                                    weight = point.weight,
                                    weightPerSecond = point.weightRate,
                                    volume = point.volume,
                                    volumePerSecond = point.flowRate
                                )
                            }
                        )
                    )
                }
            }
        }
    }

    fun handleEvent(event: DeviceDashboardEvent) = when (event) {
        is ToggleChartVisibility -> launch { toggleChartVisibility(event.type.toDomain()) }
        is ConnectionButtonClicked -> toggleConnection()
        is DeviceClicked -> emitEvent(Navigation.DeviceList)
        is SettingsClicked -> emitEvent(Navigation.Settings(args.id))
        is UserClicked -> Unit
        is ConnectedDevicesClicked -> Unit
        is CleaningClicked -> emitEvent(Navigation.Clean(args.id))
        is DialogDismissed -> modify { copy(dialog = null) }
        is OpenSystemSettingsClicked -> permissionsController.openAppSettings()
        is ManualBrewClicked -> launch { deviceController.startManualBrewing() }
        is StopBrewClicked -> launch { deviceController.stopManualBrewing() }
        is FlowControlClicked -> Unit // TODO
        is DeviceDashboardEvent.BrewClicked -> launch { deviceController.triggerShortPress() }
        is PermissionDialogResumed -> withBluetoothPermissions { modify { copy(dialog = null) } }
    }

    private fun updateMachineStateUi(state: MachineState) = modify {
        copy(
            device = device.copy(
                brewBoilerTemp = state.brewBoilerTemp?.toString(),
                steamBoilerTemp = state.steamBoilerTemp?.toString(),
                pressure = state.pressure?.toString(),
                connectionStatus = when (state.connectionStatus) {
                    ConnectionStatus.Disconnected -> Disconnected
                    ConnectionStatus.Connecting -> Connecting
                    ConnectionStatus.Connected -> Connected
                },
                brewStatus = when (state.brewStatus) {
                    MachineState.BrewStatus.Manual -> Manual
                    MachineState.BrewStatus.Profile -> Profile
                    else -> Idle
                }
            )
        )
    }

    private fun toggleConnection() {
        if (viewState.value.device.connectionStatus == Connected) {
            deviceController.disconnect()
        } else {
            withBluetoothPermissions {
                macAddress?.let { deviceController.connect(it) }
            }
        }
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
        modify { copy(dialog = BluetoothPermissionMissing) }
    }

    private fun ChartType.toPresentation() = when (this) {
        ChartType.PRESSURE -> DashboardChartType.Pressure
        ChartType.FLOW_RATE -> DashboardChartType.FlowRate
        ChartType.WEIGHT_RATE -> DashboardChartType.WeightRate
        ChartType.VOLUME -> DashboardChartType.Volume
        ChartType.WEIGHT -> DashboardChartType.Weight
    }

    private fun DashboardChartType.toDomain() = when (this) {
        DashboardChartType.Pressure -> ChartType.PRESSURE
        DashboardChartType.FlowRate -> ChartType.FLOW_RATE
        DashboardChartType.WeightRate -> ChartType.WEIGHT_RATE
        DashboardChartType.Volume -> ChartType.VOLUME
        DashboardChartType.Weight -> ChartType.WEIGHT
    }
}
