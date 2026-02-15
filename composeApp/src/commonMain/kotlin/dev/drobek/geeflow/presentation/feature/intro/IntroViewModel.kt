package dev.drobek.geeflow.presentation.feature.intro

import dev.drobek.geeflow.domain.user.usecase.AddUserUseCase
import dev.drobek.geeflow.viewmodel.BaseViewModel
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class IntroViewModel(
    private val addUserUseCase: AddUserUseCase
) : BaseViewModel<Unit, StartViewModelEvent>(Unit) {

    fun handleEvent(event: IntroEvent) = when (event) {
        is IntroEvent.ConfirmClicked -> addUser(event.userName)
    }

    private fun addUser(name: String) {
        addUserUseCase(name = name)
        emitEvent(Navigation.AddDevice)
    }
}
