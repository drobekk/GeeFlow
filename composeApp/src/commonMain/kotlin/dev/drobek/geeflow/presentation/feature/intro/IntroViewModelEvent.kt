package dev.drobek.geeflow.presentation.feature.intro

internal sealed interface StartViewModelEvent

internal sealed interface Navigation : StartViewModelEvent {
    data object AddDevice : Navigation
}
