package dev.drobek.geeflow.presentation.feature.device.dashboard.clean

sealed interface CleaningEvent {
    data object ToggleCleaningClicked : CleaningEvent
    data object MoreSettingsClicked : CleaningEvent
    data object CloseClicked : CleaningEvent
}
