package dev.drobek.geeflow.presentation.feature.device.settings.quick

sealed interface QuickSettingsViewModelEvent

sealed interface Navigation : QuickSettingsViewModelEvent {
    data object Back : Navigation
}
