package dev.drobek.geeflow.presentation.feature.device.dashboard

import dev.drobek.geeflow.presentation.feature.device.DeviceDestinations
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.ConnectedDevicesClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.CleaningClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.ConnectionButtonClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.DeviceClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.SettingsClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.UserClicked
import dev.drobek.geeflow.viewmodel.BaseViewModel
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class DeviceDashboardViewModel(
    deviceDashboard: DeviceDestinations.DeviceDashboard
) : BaseViewModel<DeviceDashboardViewState, DeviceLitViewModelEvent>(DeviceDashboardViewState()) {

    init {
        modify { copy(device = device.copy(name = deviceDashboard.id)) }
    }

    fun handleEvent(event: DeviceDashboardEvent) = when (event) {
        is ConnectionButtonClicked -> Unit
        is DeviceClicked -> emitEvent(Navigation.DeviceList)
        is SettingsClicked -> Unit
        is UserClicked -> Unit
        is ConnectedDevicesClicked -> Unit
        is CleaningClicked -> Unit
    }
}
