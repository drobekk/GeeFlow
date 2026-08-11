package app.geeflow.presentation.feature.device.settings.connectivity

sealed interface ConnectivitySettingsViewModelEvent {
    data class ShowSnackbar(val message: String) : ConnectivitySettingsViewModelEvent
}
