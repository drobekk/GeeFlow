package app.geeflow.data.device

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.validate
import app.geeflow.data.device.model.ProfileExecution
import app.geeflow.data.device.model.ProfileIssue
import app.geeflow.data.device.model.ProfileIssueCode
import app.geeflow.data.device.model.ProfileSupport
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
        require(goal == null || goal.isFinite() && goal > 0f)
    } catch (_: IllegalArgumentException) {
        return ProfileSupport(
            ProfileExecution.Unsupported,
            emptyList(),
            listOf(ProfileIssue(ProfileIssueCode.InvalidProgram))
        )
    }
    val native = assessNativeProfile(profile)
    val unavailable = if (checkAvailability) {
        profile.requiredMetrics().filter { telemetry()[it] == null }
            .map { ProfileIssue(ProfileIssueCode.NotAvailable, metric = it) }
    } else {
        emptyList()
    }
    if (native.isEmpty()) {
        return ProfileSupport(
        ProfileExecution.Native,
        native,
        unavailable,
        profilingCapabilities.binding
    )
    }
    val issues = liveIssues(profile)
    return ProfileSupport(
        if (issues.isEmpty()) ProfileExecution.AppControlled else ProfileExecution.Unsupported,
        native,
        issues + unavailable,
    )
}

private fun DeviceController.liveIssues(profile: BrewProfile): List<ProfileIssue> {
    val caps = profilingCapabilities
    val controls = when (val program = profile.program) {
        is BrewProgram.Phases -> program.phases.map { it.id to it.control }
        is BrewProgram.Recording -> listOf(
            null to if (program.recording.controlMode == FreeHandControlMode.Flow) {
                PhaseControl.Flow(0f)
            } else {
                PhaseControl.Pressure(0f)
            }
        )
    }
    return buildList {
        controls.forEach { (id, control) ->
            val supported = when (control) {
                is PhaseControl.Pressure -> caps.livePressure.containsKey(control.location)
                is PhaseControl.Flow -> caps.liveFlow != null
                PhaseControl.PumpPause -> caps.livePause
            }
            if (!supported) add(ProfileIssue(ProfileIssueCode.UnsupportedControl, id))
        }
        if (!caps.liveModeSwitch && controls.mapNotNull { it.second.metric() }.distinct().size > 1) {
            add(ProfileIssue(ProfileIssueCode.ModeSwitch))
        }
        profile.requiredMetrics().filter { it !in caps.telemetry }.forEach {
            add(ProfileIssue(ProfileIssueCode.UnsupportedMetric, metric = it))
        }
    }
}

interface LiveBrewSession {
    /** Returns the actual quantized target sent to the device. Calls must be serialized. */
    suspend fun applyTarget(target: PhaseControl): PhaseControl
    suspend fun stop()
}
