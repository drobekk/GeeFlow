package dev.drobek.geeflow.presentation.feature.user.settings.main

sealed interface UserSettingsEvent {
    data object BackClicked : UserSettingsEvent
    data object ChangeUserClicked : UserSettingsEvent
}
