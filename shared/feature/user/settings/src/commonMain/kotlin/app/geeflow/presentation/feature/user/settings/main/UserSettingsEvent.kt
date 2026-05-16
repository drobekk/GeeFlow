package app.geeflow.presentation.feature.user.settings.main

sealed interface UserSettingsEvent {
    data object BackClicked : UserSettingsEvent
    data object ChangeUserClicked : UserSettingsEvent
    data class ItemClicked(val item: UserSettingsViewState.Item) : UserSettingsEvent
}
