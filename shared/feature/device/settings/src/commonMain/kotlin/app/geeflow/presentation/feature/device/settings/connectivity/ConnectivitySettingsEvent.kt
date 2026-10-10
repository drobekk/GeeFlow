package app.geeflow.presentation.feature.device.settings.connectivity

sealed interface ConnectivitySettingsEvent {
    data class TypeSelected(val type: ConnectivityAccessoryType) : ConnectivitySettingsEvent
    data class SearchToggled(val enabled: Boolean) : ConnectivitySettingsEvent
    data class ConnectionClicked(val name: String) : ConnectivitySettingsEvent
    data object RescanClicked : ConnectivitySettingsEvent
    data object CloseClicked : ConnectivitySettingsEvent
}
