package app.geeflow.presentation.feature.device.settings.maintenance

sealed interface MaintenanceSettingsViewModelEvent {
    data class ShowSnackbar(val message: String) : MaintenanceSettingsViewModelEvent
}
