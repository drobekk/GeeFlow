package dev.drobek.geeflow.presentation.feature.device.settings.clean

sealed interface CleanViewModelEvent

sealed interface Navigation : CleanViewModelEvent {
    data object Back : Navigation
}
