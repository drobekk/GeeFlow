package dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings

sealed interface QuickSettingsViewModelEvent {
    data class ShowSnackbar(val message: String) : QuickSettingsViewModelEvent
}

sealed interface Navigation : QuickSettingsViewModelEvent {
    data object Back : Navigation
    data class DeviceSettings(val deviceId: String) : Navigation
}
