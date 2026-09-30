package app.geeflow.domain.user.model

import app.geeflow.data.user.model.TemperatureUnit

data class BrewingPreferencesSettings(
    val autoConnect: Boolean = false,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
)
