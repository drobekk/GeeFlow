package dev.drobek.geeflow.presentation.feature.user.settings.profile

sealed interface ProfileSettingsEvent {
    data object BackClicked : ProfileSettingsEvent
    data object RenameClicked : ProfileSettingsEvent
    data class RenameConfirmed(val name: String) : ProfileSettingsEvent
    data object DeleteClicked : ProfileSettingsEvent
    data object DeleteConfirmed : ProfileSettingsEvent
    data object DialogDismissed : ProfileSettingsEvent
}
