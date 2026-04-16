package dev.drobek.geeflow.domain.user.model

import dev.drobek.geeflow.data.user.model.TemperatureUnit

data class BrewingPreferencesSettings(
    val autoConnect: Boolean = false,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
)
