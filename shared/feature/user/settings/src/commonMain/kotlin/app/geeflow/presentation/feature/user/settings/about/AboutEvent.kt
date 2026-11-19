package app.geeflow.presentation.feature.user.settings.about

sealed interface AboutEvent {
    data object BackClicked : AboutEvent
    data object LicensesClicked : AboutEvent
}
