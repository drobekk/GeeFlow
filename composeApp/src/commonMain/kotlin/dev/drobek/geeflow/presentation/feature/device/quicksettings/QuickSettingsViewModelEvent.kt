package dev.drobek.geeflow.presentation.feature.device.quicksettings

sealed interface QuickSettingsViewModelEvent

sealed interface Navigation : QuickSettingsViewModelEvent {
    data object Back : Navigation
}
