package dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings

sealed interface QuickSettingsViewModelEvent {
    data class ShowSnackbar(val message: String) : QuickSettingsViewModelEvent
}

