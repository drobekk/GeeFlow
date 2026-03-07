package dev.drobek.geeflow.presentation.feature.device.dashboard

sealed interface DeviceDashboardEvent {
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
}
