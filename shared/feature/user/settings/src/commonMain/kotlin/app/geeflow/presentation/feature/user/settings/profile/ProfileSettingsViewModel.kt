package app.geeflow.presentation.feature.user.settings.profile

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.core.presentation.launchCatching
import app.geeflow.domain.device.usecase.DisconnectCurrentDeviceUseCase
import app.geeflow.domain.user.usecase.DeleteUserUseCase
import app.geeflow.domain.user.usecase.GetSelectedUserUseCase
import app.geeflow.domain.user.usecase.GetUsersUseCase
import app.geeflow.domain.user.usecase.RemoveUserPhotoUseCase
import app.geeflow.domain.user.usecase.RenameUserUseCase
import app.geeflow.domain.user.usecase.UpdateUserPhotoUseCase
import app.geeflow.navigation.NavEvent
import app.geeflow.navigation.NavEvent.ClearBackStack
import app.geeflow.navigation.NavEvent.To
import app.geeflow.navigation.destination.Intro
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.BackClicked
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.ChangePictureClicked
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.DeleteClicked
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.DeleteConfirmed
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.DialogDismissed
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.PhotoFilePicked
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.RemovePhotoClicked
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.RemovePhotoConfirmed
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.RenameClicked
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.RenameConfirmed
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsViewModelEvent.OpenPhotoPicker
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsViewState.Dialog
import co.touchlab.kermit.Logger
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class ProfileSettingsViewModel(
    private val getSelectedUser: GetSelectedUserUseCase,
    private val getUsers: GetUsersUseCase,
    private val renameUser: RenameUserUseCase,
    private val deleteUser: DeleteUserUseCase,
    private val updateUserPhoto: UpdateUserPhotoUseCase,
    private val removeUserPhoto: RemoveUserPhotoUseCase,
    private val disconnectCurrentDevice: DisconnectCurrentDeviceUseCase,
) : BaseViewModel<ProfileSettingsViewState, ProfileSettingsViewModelEvent>(ProfileSettingsViewState()) {

    init {
        launch {
            getSelectedUser().collect { user ->
                if (user != null) {
                    modify { copy(userId = user.id, name = user.name, photoFileName = user.photoUri) }
                }
            }
        }
    }

    fun handleEvent(event: ProfileSettingsEvent) = when (event) {
        is BackClicked -> navigate(NavEvent.Back)
        is ChangePictureClicked -> launch { emitEvent(OpenPhotoPicker) }
        is PhotoFilePicked -> launchCatching(::onError) {
            updateUserPhoto(viewState.value.userId, event.bytes)
        }

        is RenameClicked -> modify { copy(dialog = Dialog.Rename) }
        is RenameConfirmed -> {
            if (event.name.isNotBlank()) {
                renameUser(viewState.value.userId, event.name.trim())
            }
            modify { copy(dialog = null) }
        }

        is DeleteClicked -> modify { copy(dialog = Dialog.Delete) }
        is DeleteConfirmed -> launch {
            disconnectCurrentDevice()
            deleteUser(viewState.value.userId)
            if (getUsers().value.isEmpty()) {
                navigate(ClearBackStack)
                navigate(To(Intro))
            } else {
                navigate(NavEvent.Back)
            }
        }

        is DialogDismissed -> modify { copy(dialog = null) }
        is RemovePhotoClicked -> modify { copy(dialog = Dialog.RemovePhoto) }
        is RemovePhotoConfirmed -> {
            launchCatching(::onError) { removeUserPhoto(viewState.value.userId) }
            modify { copy(dialog = null) }
        }
    }

    private fun onError(throwable: Throwable) {
        Logger.e(throwable = throwable) { "${this::class.simpleName}" }
    }
}
