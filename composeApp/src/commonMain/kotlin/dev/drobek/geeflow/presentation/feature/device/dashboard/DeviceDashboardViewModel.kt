package dev.drobek.geeflow.presentation.feature.device.dashboard

import dev.drobek.geeflow.data.device.api.DeviceController
import dev.drobek.geeflow.domain.brew.model.BrewDataPoint
import dev.drobek.geeflow.domain.brew.usecase.GetBrewProfileUseCase
import dev.drobek.geeflow.domain.brew.usecase.ObserveBrewDataUseCase
import dev.drobek.geeflow.domain.device.model.MachineState
import dev.drobek.geeflow.domain.device.usecase.GetDeviceUseCase
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
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.SettingsClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.StopBrewClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.ToggleChartVisibility
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.UserClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Brew
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.DashboardChartType
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device.BrewStatus.Idle
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device.BrewStatus.Manual
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device.BrewStatus.Profile
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Dialog
import dev.drobek.geeflow.presentation.feature.device.dashboard.model.ChartData
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
    private val toggleChartVisibility: ToggleChartVisibilityUseCase,
    private val getBrewProfileUseCase: GetBrewProfileUseCase
) : BaseViewModel<DeviceDashboardViewState, DeviceLitViewModelEvent>(DeviceDashboardViewState()) {

    private var macAddress: String? = null
    private var selectedProfileId: String? = null

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
            observeBrewData().collect { session ->
                modify {
                    copy(brew = brew.copy(data = session.dataPoints.mapValues { (_, point) -> mapToData(point) }))
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
        is StopBrewClicked -> launch {
            if (viewState.value.device.brewStatus == Profile) {
                deviceController.triggerShortPress()
            } else {
                deviceController.stopManualBrewing()
            }
        }

        is FlowControlClicked -> Unit // TODO
        is BrewClicked -> startProfile()
        is PermissionDialogResumed -> withBluetoothPermissions { modify { copy(dialog = null) } }
        is ProfileSelected -> onProfileSelected(event.id)
    }

    private fun startProfile() {
        launch {
            selectedProfileId
                ?.toLongOrNull()
                ?.let { getBrewProfileUseCase(it) }
                ?.let { profile -> deviceController.startProfileBrewing(profile) }
        }
    }

    private fun onProfileSelected(id: String?) {
        selectedProfileId = id
        modify { copy(showProfileDetails = true, brew = Brew()) }
    }

    private fun updateMachineStateUi(state: MachineState) = modify {
        val newBrewStatus = when (state.brewStatus) {
            MachineState.BrewStatus.Manual -> Manual
            MachineState.BrewStatus.Profile -> Profile
            else -> Idle
        }
        val isBrewingNow = newBrewStatus != Idle

        copy(
            showProfileDetails = if (isBrewingNow) false else showProfileDetails,
            device = device.copy(
                brewBoilerTemp = state.brewBoilerTemp?.toString(),
                steamBoilerTemp = state.steamBoilerTemp?.toString(),
                pressure = state.pressure?.toString(),
                connectionStatus = when (state.connectionStatus) {
                    MachineState.ConnectionStatus.Disconnected -> Device.ConnectionStatus.Disconnected
                    MachineState.ConnectionStatus.Connecting -> Device.ConnectionStatus.Connecting
                    MachineState.ConnectionStatus.Connected -> Device.ConnectionStatus.Connected
                },
                brewStatus = newBrewStatus
            )
        )
    }

    private fun toggleConnection() {
        if (viewState.value.device.connectionStatus == Device.ConnectionStatus.Connected) {
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
        modify { copy(dialog = Dialog.BluetoothPermissionMissing) }
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

    private fun mapToData(point: BrewDataPoint): ChartData = ChartData(
        pressure = point.pressure,
        weight = point.weight,
        weightPerSecond = point.weightRate,
        volume = point.volume,
        volumePerSecond = point.flowRate
    )
}
