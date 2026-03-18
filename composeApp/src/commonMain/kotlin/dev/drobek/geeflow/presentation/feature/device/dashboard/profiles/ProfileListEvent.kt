package dev.drobek.geeflow.presentation.feature.device.dashboard.profiles

sealed interface ProfileListEvent {
    data class ProfileSelected(val id: String) : ProfileListEvent
    data object HistoryClicked : ProfileListEvent
    data object AddProfileClicked : ProfileListEvent
}
