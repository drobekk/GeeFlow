package app.geeflow.data.brew.model

/** Exactly one selected controller, even when its target is zero. Sensor readings are separate. */
data class ControlTargets(
    val controller: FreeHandControlMode,
    val pressureBar: Float,
    val flowMillilitresPerSecond: Float,
) {
    init {
        require(pressureBar.isFinite() && pressureBar >= 0f)
        require(flowMillilitresPerSecond.isFinite() && flowMillilitresPerSecond >= 0f)
        require(if (controller == FreeHandControlMode.Pressure) flowMillilitresPerSecond == 0f else pressureBar == 0f)
    }
}

/** Compatibility for saved waits: keep a controller selected and command zero through that mode. */
fun PhaseControl.activeControl(previous: PhaseControl? = null): PhaseControl = when (this) {
    PhaseControl.PumpPause -> when (previous) {
        is PhaseControl.Flow -> previous.copy(millilitresPerSecond = 0f)
        is PhaseControl.Pressure -> previous.copy(bar = 0f)
        else -> PhaseControl.Pressure(0f)
    }
    else -> this
}

fun PhaseControl.toControlTargets(previous: PhaseControl? = null): ControlTargets = when (val active = activeControl(previous)) {
    is PhaseControl.Pressure -> ControlTargets(FreeHandControlMode.Pressure, active.bar, 0f)
    is PhaseControl.Flow -> ControlTargets(FreeHandControlMode.Flow, 0f, active.millilitresPerSecond)
    PhaseControl.PumpPause -> error("A running program must select pressure or flow")
}
