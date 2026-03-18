package dev.drobek.geeflow.presentation.feature.device.dashboard

import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.DashboardChartType

sealed interface DeviceDashboardEvent {
    data class ToggleChartVisibility(val type: DashboardChartType) : DeviceDashboardEvent
    data object UserClicked : DeviceDashboardEvent
    data object DeviceClicked : DeviceDashboardEvent
    data object ManualBrewClicked : DeviceDashboardEvent
    data object FlowControlClicked : DeviceDashboardEvent
    data object StopBrewClicked : DeviceDashboardEvent
    data object BrewClicked : DeviceDashboardEvent
    data object SettingsClicked : DeviceDashboardEvent
    data object CleaningClicked : DeviceDashboardEvent
    data object ConnectedDevicesClicked : DeviceDashboardEvent
    data object ConnectionButtonClicked : DeviceDashboardEvent
    data object DialogDismissed : DeviceDashboardEvent
    data object PermissionDialogResumed : DeviceDashboardEvent
    data object OpenSystemSettingsClicked : DeviceDashboardEvent
    data class ProfileSelected(val id: String) : DeviceDashboardEvent
}
