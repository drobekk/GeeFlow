package app.geeflow.presentation.feature.device.settings.brewing

sealed interface BrewingSettingsEvent {
    data class SteamBoilerToggled(val enabled: Boolean) : BrewingSettingsEvent
    data class BrewBoilerToggled(val enabled: Boolean) : BrewingSettingsEvent
    data class PulseHeatingToggled(val enabled: Boolean) : BrewingSettingsEvent
    data class SteamTempChanged(val temp: String) : BrewingSettingsEvent
    data class BrewTempChanged(val temp: String) : BrewingSettingsEvent
    data class PaddlePressureChanged(val pressure: String) : BrewingSettingsEvent
    data class PaddleTimeChanged(val time: String) : BrewingSettingsEvent
    data object ApplyClicked : BrewingSettingsEvent
    data object CloseClicked : BrewingSettingsEvent
}
