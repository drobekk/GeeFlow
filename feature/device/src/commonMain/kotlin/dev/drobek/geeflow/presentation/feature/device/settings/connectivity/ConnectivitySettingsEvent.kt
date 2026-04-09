package dev.drobek.geeflow.presentation.feature.device.settings.connectivity

sealed interface ConnectivitySettingsEvent {
    data class SmartScaleToggled(val enabled: Boolean) : ConnectivitySettingsEvent
    data class ScaleConnectionClicked(val scaleName: String) : ConnectivitySettingsEvent
    data object RescanClicked : ConnectivitySettingsEvent
    data object CloseClicked : ConnectivitySettingsEvent
}
