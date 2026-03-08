package dev.drobek.geeflow.presentation.feature.device.settings.quick

sealed interface QuickSettingsEvent {
    data class SteamBoilerToggled(val enabled: Boolean) : QuickSettingsEvent
    data class BrewBoilerToggled(val enabled: Boolean) : QuickSettingsEvent
    data class SteamTempChanged(val temp: String) : QuickSettingsEvent
    data class BrewTempChanged(val temp: String) : QuickSettingsEvent
    data object SaveClicked : QuickSettingsEvent
    data object MoreSettingsClicked : QuickSettingsEvent
    data object DismissClicked : QuickSettingsEvent
}
