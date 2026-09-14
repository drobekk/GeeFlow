package app.geeflow.data.device

import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.PressureLocation
import app.geeflow.data.brew.model.validate
import app.geeflow.data.device.model.ProfileExecution
import app.geeflow.data.device.model.ProfileIssue
import app.geeflow.data.device.model.ProfileIssueCode
import app.geeflow.data.device.model.ProfileSupport
import app.geeflow.data.device.model.ProfilingCapabilities
import app.geeflow.data.device.model.metric
import app.geeflow.data.device.model.requiredMetrics

/** A device may override native assessment to validate combinations and firmware constraints. */
fun DeviceController.assessProfile(profile: BrewProfile, checkAvailability: Boolean = false): ProfileSupport {
    try {
        profile.program.validate()
        val goal = when (val finish = profile.finishCondition) {
            is Condition.Weight -> finish.target
            is Condition.Volume -> finish.target
            null -> null
        }
        require(goal == null || (goal.isFinite() && goal > 0f))
    } catch (_: IllegalArgumentException) {
        return ProfileSupport(
            ProfileExecution.Unsupported,
            emptyList(),
            listOf(ProfileIssue(ProfileIssueCode.InvalidProgram)),
        )
    }
    val native = assessNativeProfile(profile)
    val feedbackMetrics = if (native.isNotEmpty() && profilingCapabilities.liveFlowViaPressure && profile.hasFlowControl()) {
        setOf(BrewMetric.PumpFlow, BrewMetric.PumpPressure)
    } else {
        emptySet()
    }
    val unavailable = if (checkAvailability) {
        (profile.requiredMetrics() + feedbackMetrics).filter { telemetry()[it] == null }
            .map { ProfileIssue(ProfileIssueCode.NotAvailable, metric = it) }
    } else {
        emptyList()
    }
    if (native.isEmpty()) {
        return ProfileSupport(
            ProfileExecution.Native,
            native,
            unavailable,
            profilingCapabilities.binding,
        )
    }
    val issues = liveIssues(profile) + feedbackMetrics.filter { it !in profilingCapabilities.telemetry }.map {
        ProfileIssue(ProfileIssueCode.UnsupportedMetric, metric = it)
    }
    return ProfileSupport(
        if (issues.isEmpty()) ProfileExecution.AppControlled else ProfileExecution.Unsupported,
        native,
        issues + unavailable,
    )
}

private fun BrewProfile.hasFlowControl(): Boolean = when (val program = program) {
    is BrewProgram.Phases -> program.phases.any { it.control is PhaseControl.Flow }
    is BrewProgram.Recording -> program.recording.controlMode == FreeHandControlMode.Flow
}

private fun DeviceController.liveIssues(profile: BrewProfile): List<ProfileIssue> {
    val caps = profilingCapabilities
    val controls = profile.extractPhaseControls()

    return buildList {
        controls.forEach { (id, control) ->
            if (!caps.isControlSupported(control)) {
                add(ProfileIssue(ProfileIssueCode.UnsupportedControl, id))
            }
        }
        val metrics = controls.mapNotNull { it.second.metric() }.distinct()
        val pressureAdapter = caps.liveFlowViaPressure && metrics.all {
            it == BrewMetric.PumpPressure || it == BrewMetric.PumpFlow
        }
        if (!caps.liveModeSwitch && !pressureAdapter && metrics.size > 1) {
            add(ProfileIssue(ProfileIssueCode.ModeSwitch))
        }
        profile.requiredMetrics().filter { it !in caps.telemetry }.forEach {
            add(ProfileIssue(ProfileIssueCode.UnsupportedMetric, metric = it))
        }
    }
}

private fun BrewProfile.extractPhaseControls(): List<Pair<String?, PhaseControl>> = when (val p = program) {
    is BrewProgram.Phases -> p.phases.map { it.id to it.control }
    is BrewProgram.Recording -> listOf(
        null to if (p.recording.controlMode == FreeHandControlMode.Flow) {
            PhaseControl.Flow(0f)
        } else {
            PhaseControl.Pressure(0f)
        },
    )
}

private fun ProfilingCapabilities.isControlSupported(control: PhaseControl): Boolean = when (control) {
    is PhaseControl.Pressure -> livePressure.containsKey(control.location)
    is PhaseControl.Flow -> liveFlow != null && (!liveFlowViaPressure || PressureLocation.Pump in livePressure)
    is PhaseControl.PumpPause -> livePause
}

interface LiveBrewSession {
    /** Feedback controllers must run even when the profile's requested target stays constant. */
    val requiresContinuousUpdates: Boolean get() = false

    /** Returns the actual quantized target sent to the device. Calls must be serialized. */
    suspend fun applyTarget(target: PhaseControl): PhaseControl
    suspend fun stop()
}
