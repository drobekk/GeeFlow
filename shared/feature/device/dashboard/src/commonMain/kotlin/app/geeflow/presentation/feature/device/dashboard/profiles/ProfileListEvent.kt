package app.geeflow.presentation.feature.device.dashboard.profiles

sealed interface ProfileListEvent {
    data class OpenLiveRecipe(val id: String? = null) : ProfileListEvent
    data class ToggleFavoriteClicked(val id: String) : ProfileListEvent
    data class ProfileSelected(val id: String) : ProfileListEvent
    data class ProfilePreviewSelected(val id: String) : ProfileListEvent
    data class EditProfileClicked(val id: String) : ProfileListEvent
    data class DuplicateProfileClicked(val id: String) : ProfileListEvent
    data class RemoveProfileClicked(val id: String) : ProfileListEvent
    data class BindProfileClicked(val id: String) : ProfileListEvent
    data class Reordered(val from: Int, val to: Int) : ProfileListEvent
    data class SearchQueryChanged(val query: String) : ProfileListEvent
    data class HistoryBrewSelected(val id: String) : ProfileListEvent
    data class HistoryGrindSettingChanged(val id: String, val grindSetting: String?) : ProfileListEvent
    data object LoadMoreHistory : ProfileListEvent
    data object HistoryClicked : ProfileListEvent
    data object AddProfileClicked : ProfileListEvent
    data object SyncProfileLibraryNow : ProfileListEvent
}
