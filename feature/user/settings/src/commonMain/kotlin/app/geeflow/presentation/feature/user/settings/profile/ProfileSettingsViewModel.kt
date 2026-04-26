package app.geeflow.presentation.feature.user.settings.profile

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.domain.user.usecase.DeleteUserUseCase
import app.geeflow.domain.user.usecase.GetSelectedUserUseCase
import app.geeflow.domain.user.usecase.GetUsersUseCase
import app.geeflow.domain.user.usecase.RenameUserUseCase
import app.geeflow.navigation.NavEvent
import app.geeflow.navigation.destination.Intro
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.BackClicked
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.DeleteClicked
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.DeleteConfirmed
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.DialogDismissed
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.RenameClicked
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.RenameConfirmed
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsViewState.Dialog
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
