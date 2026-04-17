package dev.drobek.geeflow.presentation.feature.user.settings.profile

data class ProfileSettingsViewState(
    val userId: Long = 0L,
    val name: String = "",
    val dialog: Dialog? = null,
) {
    sealed interface Dialog {
        data object Rename : Dialog
        data object Delete : Dialog
    }
}
