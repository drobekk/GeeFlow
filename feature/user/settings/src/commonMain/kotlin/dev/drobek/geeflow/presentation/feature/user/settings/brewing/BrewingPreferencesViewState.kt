package dev.drobek.geeflow.presentation.feature.user.settings.brewing

import dev.drobek.geeflow.data.user.model.TemperatureUnit

data class BrewingPreferencesViewState(
    val autoConnect: Boolean = false,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val dialog: Dialog? = null,
) {
    sealed interface Dialog {
        data object TemperatureUnit : Dialog
        data object RestoreDefaultProfiles : Dialog
    }
}
