package app.geeflow.presentation.feature.user.settings.profile

sealed interface ProfileSettingsViewModelEvent {
    data object OpenPhotoPicker : ProfileSettingsViewModelEvent
}
