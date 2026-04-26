package app.geeflow.presentation.feature.user.list

import androidx.lifecycle.viewModelScope
import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.domain.user.usecase.GetUsersUseCase
import app.geeflow.domain.user.usecase.SetSelectedUserUseCase
import app.geeflow.navigation.NavEvent
import app.geeflow.presentation.feature.user.list.UserListEvent.AddClicked
import app.geeflow.presentation.feature.user.list.UserListEvent.BackClicked
import app.geeflow.presentation.feature.user.list.UserListEvent.UserClicked
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class UserListViewModel(
    getUsersUseCase: GetUsersUseCase,
    private val setSelectedUserUseCase: SetSelectedUserUseCase,
) : BaseViewModel<UserListViewState, Unit>(UserListViewState()) {

    init {
        getUsersUseCase().onEach { users ->
            modify {
                copy(
                    users = users.map { user ->
                        UserListViewState.User(
                            id = user.id,
                            name = user.name,
                            selected = user.isSelected,
                        )
                    },
                )
            }
        }.launchIn(viewModelScope)
    }

    fun handleEvent(event: UserListEvent) = when (event) {
        is BackClicked -> navigate(NavEvent.Back)
        is AddClicked -> navigate(NavEvent.To(AddUser))
        is UserClicked -> {
            setSelectedUserUseCase(event.user.id)
            navigate(NavEvent.Back)
        }
    }
}
