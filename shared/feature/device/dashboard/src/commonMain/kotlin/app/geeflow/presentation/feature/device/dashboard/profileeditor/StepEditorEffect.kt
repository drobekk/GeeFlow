package app.geeflow.presentation.feature.device.dashboard.profileeditor

internal sealed interface StepEditorEffect {
    data class Saved(val step: ProfileEditorViewState.Step) : StepEditorEffect
    data object Cancelled : StepEditorEffect
    data object ExplainExperimental : StepEditorEffect
}
