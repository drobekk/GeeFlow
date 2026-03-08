package dev.drobek.geeflow.presentation.feature.device.dashboard

import dev.drobek.geeflow.data.device.api.DeviceController
import dev.drobek.geeflow.domain.device.model.MachineState
import dev.drobek.geeflow.domain.device.model.MachineState.ConnectionStatus
import dev.drobek.geeflow.domain.device.usecase.GetDeviceUseCase
import dev.drobek.geeflow.platform.permissions.DeniedException
import dev.drobek.geeflow.platform.permissions.PermissionBluetoothConnect
import dev.drobek.geeflow.platform.permissions.PermissionBluetoothScan
import dev.drobek.geeflow.platform.permissions.PermissionsController
import dev.drobek.geeflow.presentation.feature.device.DeviceDestinations
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
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.UserClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device.BrewStatus.Idle
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device.BrewStatus.Manual
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device.BrewStatus.Profile
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device.ConnectionStatus.Connected
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device.ConnectionStatus.Connecting
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device.ConnectionStatus.Disconnected
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Dialog.BluetoothPermissionMissing
import dev.drobek.geeflow.viewmodel.BaseViewModel
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class DeviceDashboardViewModel(
    getDevice: GetDeviceUseCase,
    private val args: DeviceDestinations.DeviceDashboard,
    private val deviceController: DeviceController,
    private val permissionsController: PermissionsController
) : BaseViewModel<DeviceDashboardViewState, DeviceLitViewModelEvent>(DeviceDashboardViewState()) {

    private var macAddress: String? = null

    init {
        val machine = getDevice(args.id)
        macAddress = machine?.macAddress
        modify { copy(device = device.copy(id = args.id, name = machine?.name ?: args.id)) }
        launch {
            deviceController.machineState.collect { state -> updateMachineStateUi(state) }
        }
    }

    fun handleEvent(event: DeviceDashboardEvent) = when (event) {
        is ConnectionButtonClicked -> toggleConnection()
        is DeviceClicked -> emitEvent(Navigation.DeviceList)
        is SettingsClicked -> emitEvent(Navigation.Settings(args.id))
        is UserClicked -> Unit
        is ConnectedDevicesClicked -> Unit
        is CleaningClicked -> Unit
        is DialogDismissed -> modify { copy(dialog = null) }
        is OpenSystemSettingsClicked -> permissionsController.openAppSettings()
        is ManualBrewClicked -> launch { deviceController.manualBrewToggle() }
        is StopBrewClicked -> launch { deviceController.manualBrewToggle() }
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
}
