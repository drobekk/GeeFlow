package app.geeflow.presentation.feature.user.settings.brewing

import app.geeflow.data.user.model.TemperatureUnit

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
