package dev.drobek.geeflow.presentation.feature.device.settings.connectivity

sealed interface ConnectivitySettingsViewModelEvent

sealed interface Navigation : ConnectivitySettingsViewModelEvent {
    data object Back : Navigation
}
