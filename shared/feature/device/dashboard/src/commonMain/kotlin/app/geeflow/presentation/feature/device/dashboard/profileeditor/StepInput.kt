package app.geeflow.presentation.feature.device.dashboard.profileeditor

internal sealed interface StepInput {
    data object Target : StepInput
    data object RampDuration : StepInput
    data class Condition(val id: Long) : StepInput
}
