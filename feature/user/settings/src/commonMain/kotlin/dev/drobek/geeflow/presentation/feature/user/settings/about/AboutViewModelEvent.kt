package dev.drobek.geeflow.presentation.feature.user.settings.about

sealed interface AboutViewModelEvent {
    data class OpenUrl(val url: String) : AboutViewModelEvent
}
