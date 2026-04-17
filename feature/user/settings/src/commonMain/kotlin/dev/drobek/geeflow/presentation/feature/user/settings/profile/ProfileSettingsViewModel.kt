package dev.drobek.geeflow.presentation.feature.user.settings.profile

import dev.drobek.geeflow.core.presentation.BaseViewModel
import dev.drobek.geeflow.core.presentation.launch
import dev.drobek.geeflow.domain.user.usecase.DeleteUserUseCase
import dev.drobek.geeflow.domain.user.usecase.GetSelectedUserUseCase
import dev.drobek.geeflow.domain.user.usecase.GetUsersUseCase
import dev.drobek.geeflow.domain.user.usecase.RenameUserUseCase
import dev.drobek.geeflow.navigation.NavEvent
import dev.drobek.geeflow.navigation.destination.Intro
import dev.drobek.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.BackClicked
import dev.drobek.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.DeleteClicked
import dev.drobek.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.DeleteConfirmed
import dev.drobek.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.DialogDismissed
import dev.drobek.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.RenameClicked
import dev.drobek.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.RenameConfirmed
import dev.drobek.geeflow.presentation.feature.user.settings.profile.ProfileSettingsViewState.Dialog
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class ProfileSettingsViewModel(
    private val getSelectedUser: GetSelectedUserUseCase,
    private val getUsers: GetUsersUseCase,
    private val renameUser: RenameUserUseCase,
    private val deleteUser: DeleteUserUseCase,
) : BaseViewModel<ProfileSettingsViewState, Unit>(ProfileSettingsViewState()) {

    init {
        launch {
            getSelectedUser().collect { user ->
                if (user != null) {
                    modify { copy(userId = user.id, name = user.name) }
                }
            }
        }
    }

    fun handleEvent(event: ProfileSettingsEvent) = when (event) {
        is BackClicked -> navigate(NavEvent.Back)
        is RenameClicked -> modify { copy(dialog = Dialog.Rename) }
        is RenameConfirmed -> {
            if (event.name.isNotBlank()) {
                renameUser(viewState.value.userId, event.name.trim())
            }
            modify { copy(dialog = null) }
        }
        is DeleteClicked -> modify { copy(dialog = Dialog.Delete) }
        is DeleteConfirmed -> launch {
            deleteUser(viewState.value.userId)
            if (getUsers().value.isEmpty()) {
                navigate(NavEvent.To(Intro))
            } else {
                navigate(NavEvent.Back)
            }
        }
        is DialogDismissed -> modify { copy(dialog = null) }
    }
}
