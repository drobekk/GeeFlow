package app.geeflow.presentation.feature.device.dashboard.profiles

sealed interface ProfileListEvent {
    data class ProfileSelected(val id: String) : ProfileListEvent
    data class EditProfileClicked(val id: String) : ProfileListEvent
    data class RemoveProfileClicked(val id: String) : ProfileListEvent
    data class BindProfileClicked(val id: String) : ProfileListEvent
    data class Reordered(val from: Int, val to: Int) : ProfileListEvent
    data object HistoryClicked : ProfileListEvent
    data object AddProfileClicked : ProfileListEvent
}
