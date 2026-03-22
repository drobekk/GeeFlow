package dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings

sealed interface QuickSettingsViewModelEvent

sealed interface Navigation : QuickSettingsViewModelEvent {
    data object Back : Navigation
    data class DeviceSettings(val deviceId: String) : Navigation
}
