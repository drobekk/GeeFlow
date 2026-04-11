package dev.drobek.geeflow.presentation.feature.device.settings.main

sealed interface DeviceSettingsEvent {
    data class ItemClicked(val item: DeviceSettingsViewState.Item) : DeviceSettingsEvent
    data object BackClicked : DeviceSettingsEvent
}
