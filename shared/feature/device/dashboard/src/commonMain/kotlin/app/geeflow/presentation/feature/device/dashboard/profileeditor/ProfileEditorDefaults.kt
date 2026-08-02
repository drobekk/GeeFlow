package app.geeflow.presentation.feature.device.dashboard.profileeditor

/** Initial values and accepted ranges for the values entered in the profile editor dialogs. */
internal object ProfileEditorDefaults {
    const val StepTimeSec = 10
    const val WaitTimeSec = 5
    const val PressureBar = 9f
    const val FlowMlPerSec = 4f
    const val VolumeMl = 50f
    const val WeightG = 36f

    const val MaxStepTimeSec = 300
    const val MaxWaitTimeSec = 60
    const val MaxFinishTarget = 500f

    fun maxTimeSec(type: StepType) = if (type == StepType.Wait) MaxWaitTimeSec else MaxStepTimeSec

    fun defaultTimeSec(type: StepType) = if (type == StepType.Wait) WaitTimeSec else StepTimeSec

    fun defaultValue(type: StepType) = when (type) {
        StepType.Pressure -> PressureBar
        StepType.Flow -> FlowMlPerSec
        StepType.Wait -> 0f
    }
}
