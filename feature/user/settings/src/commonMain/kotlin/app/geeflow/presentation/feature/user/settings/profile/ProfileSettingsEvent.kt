package app.geeflow.presentation.feature.user.settings.profile

sealed interface ProfileSettingsEvent {
    data object BackClicked : ProfileSettingsEvent
    data object ChangePictureClicked : ProfileSettingsEvent
    data class PhotoFilePicked(val bytes: ByteArray) : ProfileSettingsEvent
    data object RenameClicked : ProfileSettingsEvent
    data class RenameConfirmed(val name: String) : ProfileSettingsEvent
    data object RemovePhotoClicked : ProfileSettingsEvent
    data object RemovePhotoConfirmed : ProfileSettingsEvent
    data object DeleteClicked : ProfileSettingsEvent
    data object DeleteConfirmed : ProfileSettingsEvent
    data object DialogDismissed : ProfileSettingsEvent
}
