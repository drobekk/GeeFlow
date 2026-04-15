package dev.drobek.geeflow.presentation.feature.user.settings.about

sealed interface AboutEvent {
    data object BackClicked : AboutEvent
    data object PrivacyPolicyClicked : AboutEvent
    data object SourceCodeClicked : AboutEvent
    data object LicensesClicked : AboutEvent
}
