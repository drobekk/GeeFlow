package app.geeflow.presentation.feature.user.settings.brewing

import app.geeflow.data.user.model.TemperatureUnit
import geeflow.feature.user.settings.generated.resources.Res
import geeflow.feature.user.settings.generated.resources.user_settings_brewing_temperature_unit_celsius
import geeflow.feature.user.settings.generated.resources.user_settings_brewing_temperature_unit_fahrenheit
import org.jetbrains.compose.resources.StringResource

internal val TemperatureUnit.titleRes: StringResource
    get() = when (this) {
        TemperatureUnit.CELSIUS -> Res.string.user_settings_brewing_temperature_unit_celsius
        TemperatureUnit.FAHRENHEIT -> Res.string.user_settings_brewing_temperature_unit_fahrenheit
    }
