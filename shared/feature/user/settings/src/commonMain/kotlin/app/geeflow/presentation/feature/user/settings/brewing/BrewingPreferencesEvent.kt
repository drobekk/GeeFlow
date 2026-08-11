package app.geeflow.presentation.feature.user.settings.brewing

import app.geeflow.data.user.model.TemperatureUnit

sealed interface BrewingPreferencesEvent {
    data object BackClicked : BrewingPreferencesEvent
    data class AutoConnectChanged(val enabled: Boolean) : BrewingPreferencesEvent
    data class SkipManualBrewHistoryChanged(val enabled: Boolean) : BrewingPreferencesEvent
    data object TemperatureUnitClicked : BrewingPreferencesEvent
    data class TemperatureUnitChanged(val unit: TemperatureUnit) : BrewingPreferencesEvent
    data object RestoreDefaultProfilesClicked : BrewingPreferencesEvent
    data object RestoreDefaultProfilesConfirmed : BrewingPreferencesEvent
    data object DialogDismissed : BrewingPreferencesEvent
}
