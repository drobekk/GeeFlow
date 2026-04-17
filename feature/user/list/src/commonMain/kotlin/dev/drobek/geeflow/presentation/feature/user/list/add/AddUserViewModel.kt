package dev.drobek.geeflow.presentation.feature.user.list.add

import dev.drobek.geeflow.core.presentation.BaseViewModel
import dev.drobek.geeflow.domain.user.usecase.AddUserUseCase
import dev.drobek.geeflow.navigation.NavEvent
import dev.drobek.geeflow.presentation.feature.user.list.add.AddUserEvent.BackClicked
import dev.drobek.geeflow.presentation.feature.user.list.add.AddUserEvent.SaveNameClicked
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
