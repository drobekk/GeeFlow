package app.geeflow.presentation.feature.device.dashboard.profileeditor

import app.geeflow.data.brew.model.BrewPhase
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.PhaseControl
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

internal fun FinishTarget.toCondition(): Condition? = if (!enabled) {
    null
} else {
    when (type) {
        FinishTargetType.Volume -> Condition.Volume(volume)
        FinishTargetType.Weight -> Condition.Weight(weight)
    }
}

/** A stored profile only carries the target it finishes on; the other one falls back to its default. */
internal fun Condition?.toFinishTarget(): FinishTarget = when (this) {
    null -> FinishTarget(enabled = false)
    is Condition.Volume -> FinishTarget(type = FinishTargetType.Volume, volume = target)
    is Condition.Weight -> FinishTarget(type = FinishTargetType.Weight, weight = target)
}

internal fun Step.toPhase() = BrewPhase(
    id = phaseId,
    name = phaseName,
    control = when (type) {
        StepType.Pressure -> PhaseControl.Pressure(value)
        StepType.Flow -> PhaseControl.Flow(value)
        StepType.Wait -> PhaseControl.PumpPause
    },
    maximumDurationMillis = timeSec * 1000L,
    minimumDurationMillis = minimumDurationMillis,
    ramp = ramp,
    exitConditions = exitConditions,
    conditionOperator = conditionOperator,
)
