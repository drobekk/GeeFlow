package dev.drobek.geeflow.presentation.feature.intro

internal sealed interface IntroEvent {
    data class ConfirmClicked(val userName: String) : IntroEvent
}
