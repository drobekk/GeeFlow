package app.geeflow.presentation.feature.device.dashboard.profileeditor

internal sealed interface ProfileEditorViewModelEvent {
    data class ShowSnackbar(val message: String) : ProfileEditorViewModelEvent
}
