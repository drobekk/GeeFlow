package app.geeflow.presentation.feature.user.list.add

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.domain.user.usecase.AddUserUseCase
import app.geeflow.navigation.NavEvent
import app.geeflow.presentation.feature.user.list.add.AddUserEvent.BackClicked
import app.geeflow.presentation.feature.user.list.add.AddUserEvent.SaveNameClicked
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class AddUserViewModel(
    private val addUser: AddUserUseCase,
) : BaseViewModel<Unit, Unit>(Unit) {

    fun handleEvent(event: AddUserEvent) = when (event) {
        is BackClicked -> navigate(NavEvent.Back)
        is SaveNameClicked -> {
            addUser(event.name.trim())
            navigate(NavEvent.Back)
        }
    }
}
