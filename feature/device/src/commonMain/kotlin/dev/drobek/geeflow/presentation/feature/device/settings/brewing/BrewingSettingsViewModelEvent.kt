package dev.drobek.geeflow.presentation.feature.device.settings.brewing

sealed interface BrewingSettingsViewModelEvent {
    data class ShowSnackbar(val message: String) : BrewingSettingsViewModelEvent
}

