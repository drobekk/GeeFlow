package app.geeflow.domain.user.usecase

import org.koin.core.annotation.Factory

@Factory
class EnsureDefaultUserUseCase(
    private val getUsers: GetUsersUseCase,
    private val addUser: AddUserUseCase,
) {
    operator fun invoke() {
        if (getUsers().value.isEmpty()) {
            addUser(name = DefaultUserName, isSelected = true)
        }
    }
}

private const val DefaultUserName = "User"
