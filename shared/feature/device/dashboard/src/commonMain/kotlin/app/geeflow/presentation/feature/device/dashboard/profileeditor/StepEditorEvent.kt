package app.geeflow.presentation.feature.device.dashboard.profileeditor

import app.geeflow.data.brew.model.RampStart
import app.geeflow.data.brew.model.RampStyle

internal sealed interface StepEditorEvent {
    data class TypeChanged(val type: StepType) : StepEditorEvent
    data class NameChanged(val value: String) : StepEditorEvent
    data class TargetChanged(val value: String) : StepEditorEvent
    data class RampChanged(val style: RampStyle) : StepEditorEvent
    data class RampStartChanged(val start: RampStart) : StepEditorEvent
    data class RampDurationChanged(val value: String) : StepEditorEvent
    data object RenameClicked : StepEditorEvent
    data object RenameDismissed : StepEditorEvent
    data class ConditionChanged(val condition: ConditionDraft) : StepEditorEvent
    data class ConditionRemoved(val id: Long) : StepEditorEvent
    data object ConditionAdded : StepEditorEvent
    data object ConditionOperatorToggled : StepEditorEvent
    data object ExperimentClicked : StepEditorEvent
    data class InputClicked(val target: StepInput) : StepEditorEvent
    data class InputConfirmed(val value: Float) : StepEditorEvent
    data object InputDismissed : StepEditorEvent
    data object Save : StepEditorEvent
    data object Cancel : StepEditorEvent
}
