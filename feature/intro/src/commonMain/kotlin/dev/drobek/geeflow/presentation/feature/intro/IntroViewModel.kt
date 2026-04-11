package dev.drobek.geeflow.presentation.feature.intro

import dev.drobek.geeflow.core.presentation.BaseViewModel
import dev.drobek.geeflow.domain.user.usecase.AddUserUseCase
import dev.drobek.geeflow.navigation.NavEvent.ClearBackStack
import dev.drobek.geeflow.navigation.NavEvent.To
import dev.drobek.geeflow.navigation.destination.AddDevice
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class IntroViewModel(
    private val addUserUseCase: AddUserUseCase
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
