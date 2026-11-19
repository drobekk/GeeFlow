package app.geeflow.presentation.feature.device.dashboard.quicksettings

sealed interface QuickSettingsEvent {
    data class SteamBoilerToggled(val enabled: Boolean) : QuickSettingsEvent
    data class BrewBoilerToggled(val enabled: Boolean) : QuickSettingsEvent
    data class SteamTempChanged(val temp: String) : QuickSettingsEvent
    data class BrewTempChanged(val temp: String) : QuickSettingsEvent
    data object CloseClicked : QuickSettingsEvent
}
