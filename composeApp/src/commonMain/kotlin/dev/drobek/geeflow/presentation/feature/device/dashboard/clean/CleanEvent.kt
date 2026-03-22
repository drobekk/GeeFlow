package dev.drobek.geeflow.presentation.feature.device.dashboard.clean

sealed interface CleanEvent {
    data object ToggleCleaningClicked : CleanEvent
    data object MoreSettingsClicked : CleanEvent
    data object CloseClicked : CleanEvent
}
