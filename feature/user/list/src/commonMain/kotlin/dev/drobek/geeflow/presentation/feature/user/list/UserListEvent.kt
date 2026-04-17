package dev.drobek.geeflow.presentation.feature.user.list

import dev.drobek.geeflow.presentation.feature.user.list.UserListViewState.User

sealed interface UserListEvent {
    data object BackClicked : UserListEvent
    data object AddClicked : UserListEvent
    data class UserClicked(val user: User) : UserListEvent
}
