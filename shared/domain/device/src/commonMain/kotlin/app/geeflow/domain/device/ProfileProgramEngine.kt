package app.geeflow.domain.device

import app.geeflow.data.brew.model.*
import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.BrewPhase
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.ExitCondition
import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.brew.model.FreeHandRecording
import app.geeflow.data.brew.model.MeasurementScope
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.PhaseExitReason
import app.geeflow.data.brew.model.PhaseTransition
import app.geeflow.data.brew.model.RampStart
import app.geeflow.data.brew.model.RampStyle
import app.geeflow.data.brew.model.ThresholdComparison
import app.geeflow.data.brew.model.conditionsMet
import app.geeflow.data.device.model.BrewTelemetry
import app.geeflow.data.device.model.metric
import app.geeflow.data.device.model.value
import app.geeflow.data.device.model.withValue

/** Pure, monotonic-time evaluator. It never writes to a device or assumes missing measurements are zero. */
class ProfileProgramEngine(
    private val profile: BrewProfile,
    private val simulation: Boolean = false,
    private val finishWeightHandledExternally: Boolean = false,
) {
    private var index = 0
    private var enteredAt = 0L
    private var entered = false
    private var rampFrom = 0f
    private var previousTarget: PhaseControl? = null
    private var stepVolume: Float? = null
    private var stepWeight: Float? = null
    private var adaptiveFlowMaximum: Float? = null
    private var adaptiveFlowHold: Float? = null
    val transitions = mutableListOf<PhaseTransition>()
    val captureEvents = mutableListOf<CapturedMeasurement>()
    val decisions = mutableListOf<ControlDecision>()
    private val captured = mutableMapOf<String, Float>()
    private val samples = mutableListOf<Pair<Long, BrewTelemetry>>()
    private var applied: PhaseControl? = null
    private var lastController: PhaseControl? = null
    private var resolvedGoal = 0f
    private var previousResolvedGoal: PhaseControl? = null
    private var matchesSince: Long? = null
    private var limitedValue: Float? = null
    private var limitExceededAt: Long? = null
    private var lastEvaluation = 0L
    private var finished: String? = null
    fun acknowledgeApplied(target: PhaseControl) { if (finished == null) applied = target.activeControl(lastController) }
    private val deadline: Long get() = when (val p = profile.program) {
        is BrewProgram.Phases -> p.execution.maximumTotalDurationMillis
        is BrewProgram.Recording -> p.execution.maximumTotalDurationMillis
    }.coerceAtMost(40000)


    data class Output(val target: PhaseControl? = null, val phaseId: String? = null, val finish: String? = null) {
        init {
            require((finish == null) == (target != null))
            require(target != PhaseControl.PumpPause)
        }
        val controlTargets: ControlTargets? get() = target?.toControlTargets()
    }

    fun tick(elapsedMillis: Long, telemetry: BrewTelemetry): Output {
        finished?.let { return Output(finish = it) }
        require(elapsedMillis >= lastEvaluation) { "Execution time must be monotonic" }
        if (!simulation && elapsedMillis >= deadline) { finished = "profile_timeout"; return Output(finish = finished) }
        samples += elapsedMillis to telemetry
        samples.removeAll { it.first < elapsedMillis - 10000 }

        if (!finishWeightHandledExternally && profile.program.version == 2 && profile.finishCondition is Condition.Weight) {
            fresh(BrewMetric.CupWeight, elapsedMillis, telemetry, 500)
        }
        val goal = when (val condition = profile.finishCondition.takeUnless {
            finishWeightHandledExternally && it is Condition.Weight
        }) {
            is Condition.Volume -> telemetry[BrewMetric.PumpedVolume]?.let { it >= condition.target } == true
            is Condition.Weight -> telemetry[BrewMetric.CupWeight]?.let { it >= condition.target } == true
            null -> false
        }
        if (goal) { finished = "global_goal"; return Output(finish = finished) }
        val output = when (val program = profile.program) {
            is BrewProgram.Recording -> recording(program, elapsedMillis)
            is BrewProgram.Phases -> phases(program.phases, elapsedMillis, telemetry)
        }
        lastEvaluation = elapsedMillis
        output.target?.let { lastController = it }
        if (output.finish != null) finished = output.finish
        return output
    }

    private fun recording(program: BrewProgram.Recording, elapsed: Long): Output {
        val recording = program.recording
        val duration = recording.samples.last().elapsedMillis
        if (elapsed >= maxOf(MINIMUM_RECORDING_TIMEOUT_MS, duration * 2)) return Output(finish = "recording_timeout")
        if (profile.finishCondition == null && elapsed >= duration) return Output(finish = "program_complete")
        val sample = recording.samples.lastOrNull { it.elapsedMillis <= elapsed } ?: recording.samples.first()
        val next = recording.samples.firstOrNull { it.elapsedMillis > elapsed }
        val interpolated = if (program.version == 2 && program.replayPolicy == ReplayPolicy.Linear && next != null) {
            val f = (elapsed - sample.elapsedMillis).toFloat() / (next.elapsedMillis - sample.elapsedMillis)
            if (recording.controlMode == FreeHandControlMode.Flow) sample.data.flowRate + (next.data.flowRate - sample.data.flowRate) * f
            else sample.data.pressure + (next.data.pressure - sample.data.pressure) * f
        } else null
        return Output(
            target = if (recording.controlMode == FreeHandControlMode.Flow) {
                PhaseControl.Flow(interpolated ?: sample.data.flowRate)
            } else {
                PhaseControl.Pressure(interpolated ?: sample.data.pressure)
            }
        )
    }

    private fun phases(phases: List<BrewPhase>, elapsed: Long, telemetry: BrewTelemetry): Output {
        while (index < phases.size) {
            val phase = phases[index]
            if (!entered) {
                enteredAt = elapsed
                capture(phase.entryCaptures, elapsed, telemetry)
                resolvedGoal = phase.targetSource?.let { resolve(it, phase, elapsed, telemetry) } ?: phase.control.value()
                rampFrom = phase.ramp.source?.let { resolve(it, phase, elapsed, telemetry) } ?: rampStartValue(phase, elapsed, telemetry)
                matchesSince = null
                limitedValue = null
                limitExceededAt = null
                stepVolume = telemetry[BrewMetric.PumpedVolume]
                stepWeight = telemetry[BrewMetric.CupWeight]
                adaptiveFlowMaximum = null
                adaptiveFlowHold = null
                entered = true
            }
            val phaseTime = elapsed - enteredAt
            phase.adaptiveFlowProbe?.let { probe ->
                val pressure = telemetry[BrewMetric.PumpPressure]
                val flow = telemetry[BrewMetric.PumpFlow]
                if (
                    phaseTime < probe.samplingDurationMillis &&
                    pressure != null && pressure < probe.pressureCeilingBar &&
                    flow != null
                ) {
                    adaptiveFlowMaximum = maxOf(adaptiveFlowMaximum ?: flow, flow)
                }
            }
            var matchedCondition: ExitCondition? = null
            val rawConditionsMet = phaseTime >= phase.minimumDurationMillis && phase.conditionsMet { condition ->
                val measured = measurement(condition, phaseTime, elapsed, telemetry) ?: return@conditionsMet false
                val matches = when (condition.comparison) {
                    ThresholdComparison.Above -> measured >= condition.threshold
                    ThresholdComparison.Below -> measured <= condition.threshold
                    ThresholdComparison.StrictlyAbove -> measured > condition.threshold
                    ThresholdComparison.StrictlyBelow -> measured < condition.threshold
                }
                if (matches) matchedCondition = condition
                matches
            }
            if (rawConditionsMet) { if (matchesSince == null) matchesSince = elapsed } else matchesSince = null
            val timeDeadlineMet = phaseTime >= phase.minimumDurationMillis && phase.exitConditions.any { c ->
                if (c.metric != BrewMetric.PhaseTime) false else {
                    val measured = phaseTime / MILLISECONDS_PER_SECOND
                    when (c.comparison) {
                        ThresholdComparison.Above -> measured >= c.threshold
                        ThresholdComparison.Below -> measured <= c.threshold
                        ThresholdComparison.StrictlyAbove -> measured > c.threshold
                        ThresholdComparison.StrictlyBelow -> measured < c.threshold
                    }
                }
            }
            val conditionsMet = timeDeadlineMet || (rawConditionsMet && elapsed - requireNotNull(matchesSince) >= phase.stabilityDurationMillis)
            val reason = when {
                phaseTime >= phase.maximumDurationMillis -> PhaseExitReason.MaximumDuration
                conditionsMet -> PhaseExitReason.ConditionMatched
                else -> null
            }
            if (reason != null) {
                transitions += PhaseTransition(
                    elapsed,
                    phase.id,
                    reason,
                    matchedCondition.takeIf { reason == PhaseExitReason.ConditionMatched }
                )
                capture(phase.exitCaptures, elapsed, telemetry)
                previousResolvedGoal = phase.control.withValue(resolvedGoal)
                previousTarget = phase.control
                index++
                entered = false
                continue
            }
            var value = rampFrom + (resolvedGoal - rampFrom) * phase.ramp.fraction(phaseTime)
            phase.adaptiveFlowProbe?.let { probe ->
                val pressure = telemetry[BrewMetric.PumpPressure]
                val flow = telemetry[BrewMetric.PumpFlow]
                if (phaseTime < probe.samplingDurationMillis) {
                    if (pressure != null && pressure >= probe.pressureCeilingBar) {
                        value = minOf(value, adaptiveFlowMaximum ?: rampFrom)
                    }
                } else {
                    if (adaptiveFlowHold == null) adaptiveFlowHold = adaptiveFlowMaximum ?: rampFrom
                    if (pressure != null && pressure >= probe.pressureCeilingBar && flow != null) {
                        adaptiveFlowHold = minOf(
                            requireNotNull(adaptiveFlowHold),
                            (flow - probe.flowBackoffMillilitresPerSecond).coerceAtLeast(0f),
                        )
                    }
                    value = adaptiveFlowHold ?: rampFrom
                }
            }
            val requested = phase.control.withValue(value).activeControl(applied ?: lastController ?: previousTarget?.takeUnless { it == PhaseControl.PumpPause } ?: phases.firstOrNull { it.control != PhaseControl.PumpPause }?.control)
            phase.secondaryLimit?.let { limit ->
                val measured = fresh(limit.metric, elapsed, telemetry, 500)
                val dt = (elapsed - lastEvaluation).coerceIn(0, 500).toFloat() / 1000f
                val prior = limitedValue ?: minOf(value, applied?.takeIf { it.metric() == phase.control.metric() }?.value() ?: rampFrom)
                value = when {
                    (if (limit.minimum != null) measured > limit.ceiling else measured >= limit.ceiling) -> minOf(value, (prior - limit.reductionPerSecond * dt).coerceAtLeast(0f))
                    measured < (limit.minimum ?: (limit.ceiling - limit.hysteresis)) -> minOf(value, prior + limit.recoveryPerSecond * dt)
                    else -> minOf(value, prior)
                }.coerceIn(0f, 12f)
                limitedValue = value
                if (measured > limit.ceiling + limit.overshootAllowance) {
                    if (limitExceededAt == null) limitExceededAt = elapsed
                    if (elapsed - requireNotNull(limitExceededAt) >= limit.tripDurationMillis) return Output(finish = "secondary_limit_fault")
                } else limitExceededAt = null
            }
            val limited = phase.control.withValue(value).activeControl(applied ?: lastController ?: previousTarget?.takeUnless { it == PhaseControl.PumpPause } ?: phases.firstOrNull { it.control != PhaseControl.PumpPause }?.control)
            decisions += ControlDecision(elapsed, phase.id, requested, limited, applied)
            return Output(limited, phase.id)
        }
        return Output(finish = "program_complete")
    }

    private fun fresh(metric: BrewMetric, elapsed: Long, telemetry: BrewTelemetry, maximumAge: Long): Float {
        val sampledAt = requireNotNull(telemetry.sampledAtMillis[metric]) { "Missing measurement timestamp: $metric" }
        require(sampledAt <= elapsed && elapsed - sampledAt <= maximumAge) { "Stale measurement: $metric" }
        return requireNotNull(telemetry[metric]?.takeIf { it >= 0 }) { "Invalid measurement: $metric" }
    }

    private fun capture(actions: List<CaptureAction>, elapsed: Long, telemetry: BrewTelemetry) {
        actions.forEach { action ->
            val result = runCatching {
                fresh(action.metric, elapsed, telemetry, action.maximumAgeMillis)
                val window = samples.filter { elapsed - it.first <= action.windowMillis }
                    .mapNotNull { (_, data) -> data.sampledAtMillis[action.metric]?.let { stamp ->
                        if (stamp <= elapsed && elapsed - stamp <= action.maximumAgeMillis) data[action.metric]?.let { Triple(stamp, action.metric, it) } else null
                    } }.distinctBy { it.first }
                val value = if (action.filter == CaptureFilter.Mean) window.map { it.third }.average().toFloat()
                    else fresh(action.metric, elapsed, telemetry, action.maximumAgeMillis)
                require(value.isFinite() && value in action.minimum..action.maximum)
                value
            }
            val value = result.getOrNull() ?: requireNotNull(action.fallback) { "Invalid capture: ${action.name}" }
            captured[action.name] = value
            captureEvents += CapturedMeasurement(elapsed, action.name, action.metric, value, telemetry.sampledAtMillis[action.metric], result.isFailure)
        }
    }

    private fun resolve(source: TargetSource, phase: BrewPhase, elapsed: Long, telemetry: BrewTelemetry): Float {
        val value = when (source.kind) {
            TargetSourceKind.Constant -> requireNotNull(source.value)
            TargetSourceKind.MeasurementAtEntry -> fresh(requireNotNull(source.metric), elapsed, telemetry, 500)
            TargetSourceKind.CapturedValue -> requireNotNull(captured[source.capture]) { "Missing capture: ${source.capture}" }
            TargetSourceKind.PreviousGoal -> requireNotNull(previousResolvedGoal?.takeIf { it.metric() == phase.control.metric() }) { "Missing previous goal" }.value()
            TargetSourceKind.PreviousApplied -> requireNotNull(applied?.takeIf { it.metric() == phase.control.metric() }) { "Missing previous applied command" }.value()
        }
        require(value.isFinite() && value in 0f..12f) { "Target source outside machine range" }
        return value
    }

    private fun measurement(condition: ExitCondition, phaseTime: Long, elapsed: Long, telemetry: BrewTelemetry): Float? = when {
        condition.metric == BrewMetric.PhaseTime -> phaseTime / MILLISECONDS_PER_SECOND
        condition.scope == MeasurementScope.Total -> if (profile.program.version == 2) fresh(condition.metric, elapsed, telemetry, 500) else telemetry[condition.metric]
        else -> {
            val current = if (profile.program.version == 2) fresh(condition.metric, elapsed, telemetry, 500) else telemetry[condition.metric]
            val baseline = when (condition.metric) {
                BrewMetric.PumpedVolume -> stepVolume
                BrewMetric.CupWeight -> stepWeight
                else -> null
            }
            if (current != null && baseline != null) (current - baseline).takeIf { it >= 0f } else null
        }
    }

    private fun rampStartValue(phase: BrewPhase, elapsed: Long, telemetry: BrewTelemetry): Float {
        if (phase.ramp.style == RampStyle.Instant) return phase.control.value()
        val metric = phase.control.metric()
        if (phase.ramp.start == RampStart.PreviousTarget && previousTarget?.metric() == metric) {
            return requireNotNull(previousTarget).value()
        }
        return requireNotNull(metric?.let { if (profile.program.version == 2) fresh(it, elapsed, telemetry, 500) else telemetry[it] }) { "Missing ramp start measurement" }
    }
}

private const val MINIMUM_RECORDING_TIMEOUT_MS = 30000L
private const val MILLISECONDS_PER_SECOND = 1000f
