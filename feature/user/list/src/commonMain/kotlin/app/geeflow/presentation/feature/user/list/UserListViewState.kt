package app.geeflow.presentation.feature.user.list

data class UserListViewState(
    val users: List<User> = emptyList(),
) {
    data class User(
        val id: Long,
        val name: String,
        val selected: Boolean,
    )
}
