package app.geeflow.presentation.feature.device.dashboard.profileeditor

import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorViewState.FinishTarget
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorViewState.Step

internal fun Step.toDomain(): ProfileStep = when (type) {
    StepType.Flow -> ProfileStep.Flow(time = timeSec, flow = value)
    StepType.Pressure -> ProfileStep.Pressure(time = timeSec, pressure = value)
    StepType.Wait -> ProfileStep.Wait(time = timeSec)
}

internal fun ProfileStep.toStep(id: Long): Step = when (this) {
    is ProfileStep.Flow -> Step(id = id, type = StepType.Flow, timeSec = time, value = flow)
    is ProfileStep.Pressure -> Step(id = id, type = StepType.Pressure, timeSec = time, value = pressure)
    is ProfileStep.Wait -> Step(id = id, type = StepType.Wait, timeSec = time, value = 0f)
}

internal fun FinishTarget.toCondition(): Condition = when (type) {
    FinishTargetType.Volume -> Condition.Volume(volume)
    FinishTargetType.Weight -> Condition.Weight(weight)
}

/** A stored profile only carries the target it finishes on; the other one falls back to its default. */
internal fun Condition.toFinishTarget(): FinishTarget = when (this) {
    is Condition.Volume -> FinishTarget(type = FinishTargetType.Volume, volume = target)
    is Condition.Weight -> FinishTarget(type = FinishTargetType.Weight, weight = target)
}
