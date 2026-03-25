package dev.drobek.geeflow.presentation.feature.device.settings.brewing

sealed interface BrewingSettingsViewModelEvent

sealed interface Navigation : BrewingSettingsViewModelEvent {
    data object Back : Navigation
}
