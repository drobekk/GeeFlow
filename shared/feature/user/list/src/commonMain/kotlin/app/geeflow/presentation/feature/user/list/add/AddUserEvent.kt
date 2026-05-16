package app.geeflow.presentation.feature.user.list.add

sealed interface AddUserEvent {
    data object BackClicked : AddUserEvent
    data class SaveNameClicked(val name: String) : AddUserEvent
}
