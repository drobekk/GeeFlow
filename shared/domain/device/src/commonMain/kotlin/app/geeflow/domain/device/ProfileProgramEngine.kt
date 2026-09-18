package app.geeflow.domain.device

import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.BrewPhase
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.ExitCondition
import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.brew.model.FreeHandRecording
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
class ProfileProgramEngine(private val profile: BrewProfile) {
    private var index = 0
    private var enteredAt = 0L
    private var entered = false
    private var rampFrom = 0f
    private var previousTarget: PhaseControl? = null
    val transitions = mutableListOf<PhaseTransition>()

    data class Output(val target: PhaseControl? = null, val phaseId: String? = null, val finish: String? = null)

    fun tick(elapsedMillis: Long, telemetry: BrewTelemetry): Output {
        val goal = when (val condition = profile.finishCondition) {
            is Condition.Volume -> telemetry[BrewMetric.PumpedVolume]?.let { it >= condition.target } == true
            is Condition.Weight -> telemetry[BrewMetric.CupWeight]?.let { it >= condition.target } == true
            null -> false
        }
        if (goal) return Output(finish = "global_goal")
        return when (val program = profile.program) {
            is BrewProgram.Recording -> recording(program.recording, elapsedMillis)
            is BrewProgram.Phases -> phases(program.phases, elapsedMillis, telemetry)
        }
    }

    private fun recording(recording: FreeHandRecording, elapsed: Long): Output {
        val duration = recording.samples.last().elapsedMillis
        if (elapsed >= maxOf(MINIMUM_RECORDING_TIMEOUT_MS, duration * 2)) return Output(finish = "recording_timeout")
        if (profile.finishCondition == null && elapsed >= duration) return Output(finish = "program_complete")
        val sample = recording.samples.lastOrNull { it.elapsedMillis <= elapsed } ?: recording.samples.first()
        return Output(
            target = if (recording.controlMode == FreeHandControlMode.Flow) {
                PhaseControl.Flow(sample.data.flowRate)
            } else {
                PhaseControl.Pressure(sample.data.pressure)
            }
        )
    }

    private fun phases(phases: List<BrewPhase>, elapsed: Long, telemetry: BrewTelemetry): Output {
        while (index < phases.size) {
            val phase = phases[index]
            if (!entered) {
                enteredAt = elapsed
                rampFrom = rampStartValue(phase, telemetry)
                entered = true
            }
            val phaseTime = elapsed - enteredAt
            var matchedCondition: ExitCondition? = null
            val conditionsMet = phaseTime >= phase.minimumDurationMillis && phase.conditionsMet { condition ->
                val measured = if (condition.metric == BrewMetric.PhaseTime) {
                    phaseTime / 1000f
                } else {
                    telemetry[condition.metric] ?: return@conditionsMet false
                }
                val matches = when (condition.comparison) {
                    ThresholdComparison.Above -> measured >= condition.threshold
                    ThresholdComparison.Below -> measured <= condition.threshold
                }
                if (matches) matchedCondition = condition
                matches
            }
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
                previousTarget = phase.control
                index++
                entered = false
                continue
            }
            val value = rampFrom + (phase.control.value() - rampFrom) * phase.ramp.fraction(phaseTime)
            return Output(phase.control.withValue(value), phase.id)
        }
        return Output(finish = "program_complete")
    }

    private fun rampStartValue(phase: BrewPhase, telemetry: BrewTelemetry): Float {
        if (phase.ramp.style == RampStyle.Instant) return phase.control.value()
        val metric = phase.control.metric()
        if (phase.ramp.start == RampStart.PreviousTarget && previousTarget?.metric() == metric) {
            return requireNotNull(previousTarget).value()
        }
        return requireNotNull(metric?.let { telemetry[it] }) { "Missing ramp start measurement" }
    }
}

private const val MINIMUM_RECORDING_TIMEOUT_MS = 30000L
