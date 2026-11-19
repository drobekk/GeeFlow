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
import app.geeflow.data.device.model.ProfilingCapabilities
import app.geeflow.data.device.model.metric
import app.geeflow.data.device.model.requiredMetrics

/** A device may override native assessment to validate combinations and firmware constraints. */
fun DeviceController.assessProfile(
    profile: BrewProfile,
    checkAvailability: Boolean = false,
    weightFinishHandledByAppCutoff: Boolean = false,
): ProfileSupport {
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
    val programMetrics = profile.copy(finishCondition = null).requiredMetrics()
    val externallyHandledFinishMetrics = if (
        weightFinishHandledByAppCutoff && profile.finishCondition is Condition.Weight &&
        app.geeflow.data.brew.model.BrewMetric.CupWeight !in programMetrics
    ) {
        setOf(app.geeflow.data.brew.model.BrewMetric.CupWeight)
    } else {
        emptySet()
    }
    val native = profiles.assessNativeProfile(profile)
    val unavailable = if (checkAvailability) {
        profile.requiredMetrics().filter {
            it !in externallyHandledFinishMetrics && profiles.telemetry()[it] == null
        }
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
    val issues = liveIssues(profile, externallyHandledFinishMetrics)
    return ProfileSupport(
        if (issues.isEmpty()) ProfileExecution.AppControlled else ProfileExecution.Unsupported,
        native,
        issues + unavailable,
    )
}

private fun DeviceController.liveIssues(
    profile: BrewProfile,
    externallyHandledFinishMetrics: Set<app.geeflow.data.brew.model.BrewMetric> = emptySet(),
): List<ProfileIssue> {
    val caps = profilingCapabilities
    val controls = profile.extractPhaseControls()

    return buildList {
        (profile.program as? BrewProgram.Phases)?.recipe?.let { recipe ->
            if (recipe.unresolvedSemantics.isNotEmpty()) add(ProfileIssue(ProfileIssueCode.UnsupportedControl))
            if (profile.finishCondition == null) add(ProfileIssue(ProfileIssueCode.MissingGlobalGoal))
        }
        (profile.program as? BrewProgram.Phases)?.phases?.forEach { phase ->
            if (phase.secondaryLimit != null && !caps.verifiedSecondaryLimits) add(ProfileIssue(ProfileIssueCode.UnsupportedControl, phase.id))
            if (phase.temperatureC != null && !caps.perPhaseTemperature) add(ProfileIssue(ProfileIssueCode.UnsupportedControl, phase.id))
            if (phase.targetSource?.metric == app.geeflow.data.brew.model.BrewMetric.CupWeight ||
                phase.ramp.source?.metric == app.geeflow.data.brew.model.BrewMetric.CupWeight ||
                (phase.entryCaptures + phase.exitCaptures).any { it.metric == app.geeflow.data.brew.model.BrewMetric.CupWeight }) {
                if (!caps.timestampedCupWeight) add(ProfileIssue(ProfileIssueCode.UnsupportedMetric, phase.id))
            }
        }
        if (
            profile.program.version == 2 &&
            app.geeflow.data.brew.model.BrewMetric.CupWeight in profile.requiredMetrics() &&
            app.geeflow.data.brew.model.BrewMetric.CupWeight !in externallyHandledFinishMetrics &&
            !caps.timestampedCupWeight
        ) add(ProfileIssue(ProfileIssueCode.UnsupportedMetric))
        controls.forEach { (id, control) ->
            if (!caps.isControlSupported(control)) {
                add(ProfileIssue(ProfileIssueCode.UnsupportedControl, id))
            }
        }
        val metrics = controls.mapNotNull { it.second.metric() }.distinct()
        if (!caps.liveModeSwitch && metrics.size > 1) {
            add(ProfileIssue(ProfileIssueCode.ModeSwitch))
        }
        profile.requiredMetrics().filter {
            it !in caps.telemetry && it !in externallyHandledFinishMetrics
        }.forEach {
            add(ProfileIssue(ProfileIssueCode.UnsupportedMetric, metric = it))
        }
    }
}

private fun BrewProfile.extractPhaseControls(): List<Pair<String?, PhaseControl>> = when (val p = program) {
    is BrewProgram.Phases -> p.phases.map { it.id to it.control }
    is BrewProgram.Recording -> p.recording.samples.map { sample ->
        null to if (p.recording.controlMode == FreeHandControlMode.Flow) {
            PhaseControl.Flow(sample.data.flowRate)
        } else {
            PhaseControl.Pressure(sample.data.pressure)
        }
    }
}

private fun ProfilingCapabilities.isControlSupported(control: PhaseControl): Boolean = when (control) {
    is PhaseControl.Pressure -> livePressure[control.location]?.let { control.bar in it.minimum..it.maximum } == true
    is PhaseControl.Flow -> liveFlow?.let { control.millilitresPerSecond in it.minimum..it.maximum } == true
    is PhaseControl.PumpPause -> livePause
}
