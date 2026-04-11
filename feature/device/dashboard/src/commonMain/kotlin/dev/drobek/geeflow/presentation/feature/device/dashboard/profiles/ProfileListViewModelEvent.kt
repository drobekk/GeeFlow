package dev.drobek.geeflow.presentation.feature.device.dashboard.profiles

sealed interface ProfileListViewModelEvent {
    data class SelectProfile(val id: String?) : ProfileListViewModelEvent
    data class ShowSnackbar(val message: String) : ProfileListViewModelEvent
}
