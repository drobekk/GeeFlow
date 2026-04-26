package app.geeflow.presentation.feature.intro

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.domain.user.usecase.AddUserUseCase
import app.geeflow.navigation.NavEvent.ClearBackStack
import app.geeflow.navigation.NavEvent.To
import app.geeflow.navigation.destination.AddDevice
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class IntroViewModel(
    private val addUserUseCase: AddUserUseCase,
) : BaseViewModel<Unit, Unit>(Unit) {

    fun handleEvent(event: IntroEvent) = when (event) {
        is IntroEvent.ConfirmClicked -> addUser(event.userName)
    }

    private fun addUser(name: String) {
        addUserUseCase(name = name, isSelected = true)
        navigate(ClearBackStack)
        navigate(To(AddDevice))
    }
}
