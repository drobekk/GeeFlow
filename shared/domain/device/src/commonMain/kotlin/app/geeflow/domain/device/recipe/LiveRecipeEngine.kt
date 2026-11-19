package app.geeflow.domain.device.recipe

import app.geeflow.data.brew.model.ConditionOperator
import app.geeflow.data.brew.model.MeasurementScope
import app.geeflow.data.brew.model.PhaseRamp
import app.geeflow.data.brew.model.ThresholdComparison

data class TimedRecipeValue(val value: Float, val atMillis: Long)
data class RecipeMeasurements(val values: Map<RecipeMetric, TimedRecipeValue>) {
    fun fresh(metric: RecipeMetric, now: Long): TimedRecipeValue? = values[metric]?.takeIf {
        it.value.isFinite() && it.atMillis <= now && now - it.atMillis <=
            if (metric == RecipeMetric.CupFlow || metric == RecipeMetric.Weight) 1_000L else 2_000L
    }
    fun require(metric: RecipeMetric, now: Long): TimedRecipeValue =
        requireNotNull(fresh(metric, now)) { "Missing or stale ${metric.name} measurement" }
}
data class CapturedRecipeValue(val metric: RecipeMetric, val sample: TimedRecipeValue)
data class RecipeRuntimeEvent(val type: String, val fields: Map<String, String>)
data class RecipeEngineOutput(
    val phaseId: String? = null,
    val requestedTarget: Float? = null,
    val targetUnit: RecipeUnit? = null,
    val requestedPressureBar: Float? = null,
    val appliedPressureBar: Float? = null,
    val constraints: List<String> = emptyList(),
    val finish: String? = null,
    val events: List<RecipeRuntimeEvent> = emptyList(),
)

/** Pure recipe interpreter. Sensor timestamps and the caller's clock share a monotonic origin. */
class LiveRecipeEngine(compiled: CompiledLiveRecipe) {
    private val recipe = compiled.document
    private val settings = recipe.settings
    private var index = 0
    private var enteredAt: Long? = null
    private var target = 0f
    private var rampFrom = 0f
    private var previousTarget: Pair<RecipeUnit, Float>? = null
    private var baselineWeight: Float? = null
    private var baselineVolume: Float? = null
    private var probeMaximum: Float? = null
    private var probeHold: Float? = null
    private var probeStamp: Long? = null
    private val captured = mutableMapOf<String, CapturedRecipeValue>()
    val captures: Map<String, CapturedRecipeValue> get() = captured.toMap()
    private val machineFeedback = PressureFlowRegulator(settings.machineFlowGains, settings.basePressureBar)
    private val cupFeedback = PressureFlowRegulator(settings.cupFlowGains, settings.basePressureBar)
    private val limiterFeedback = PressureFlowRegulator(settings.machineFlowGains, settings.basePressureBar)
    private var pressure = 0f
    private var lastTick: Long? = null
    private var ended: String? = null

    fun stop(reason: String = "user_stop") {
        ended = reason
        resetFeedback(0f)
    }

    fun resetFeedback(currentPressure: Float) {
        machineFeedback.reset(currentPressure)
        cupFeedback.reset(currentPressure)
        limiterFeedback.reset(currentPressure)
    }

    fun tick(now: Long, measurements: RecipeMeasurements): RecipeEngineOutput {
        val events = mutableListOf<RecipeRuntimeEvent>()
        ended?.let { return RecipeEngineOutput(finish = it) }
        fun finish(reason: String): RecipeEngineOutput { stop(reason); return RecipeEngineOutput(finish = reason, events = events) }
        if (now >= settings.deadlineMillis) return finish("shot_deadline")
        if (measurements.require(RecipeMetric.Weight, now).value >= settings.yieldGrams) return finish("global_weight")
        settings.maximumVolumeMl?.let { if (measurements.require(RecipeMetric.Volume, now).value >= it) return finish("global_volume") }
        while (index < recipe.phases.size) {
            val phase = recipe.phases[index]
            if (enteredAt == null) {
                enteredAt = now
                baselineWeight = measurements.fresh(RecipeMetric.Weight, now)?.value
                baselineVolume = measurements.fresh(RecipeMetric.Volume, now)?.value
                probeMaximum = null; probeHold = null; probeStamp = null
                if (phase.maximumDurationMillis > 0 && phase.control != RecipeControl.Pause) {
                    target = resolve(phase.target, phase.control.unit, now, measurements)
                    rampFrom = if (phase.rampMillis > 0) resolve(phase.rampStart, phase.control.unit, now, measurements) else target
                }
                resetFeedback(pressure)
                events += RecipeRuntimeEvent("phase_entered", mapOf("phase_id" to phase.id, "target" to target.toString(), "ramp_start" to rampFrom.toString(), "unit" to phase.control.unit.name))
            }
            val elapsed = now - requireNotNull(enteredAt)
            fun matches(exit: RecipeExit): Boolean {
                var value = if (exit.metric == RecipeMetric.PhaseTime) elapsed / 1_000f else measurements.require(exit.metric, now).value
                if (exit.scope == MeasurementScope.Step) {
                    val baseline = if (exit.metric == RecipeMetric.Weight) baselineWeight else baselineVolume
                    value -= requireNotNull(baseline) { "Missing step baseline for ${exit.metric}" }
                }
                return when (exit.comparison) {
                    ThresholdComparison.Above -> value >= exit.threshold
                    ThresholdComparison.Below -> value <= exit.threshold
                    ThresholdComparison.StrictlyAbove -> value > exit.threshold
                    ThresholdComparison.StrictlyBelow -> value < exit.threshold
                }
            }
            val timeExits = phase.exits.filter { it.metric == RecipeMetric.PhaseTime }
            val sensorExits = phase.exits.filterNot { it.metric == RecipeMetric.PhaseTime }
            val maximumReached = elapsed >= phase.maximumDurationMillis
            val exits = !maximumReached && elapsed >= phase.minimumDurationMillis && (
                timeExits.any(::matches) || when (phase.conditionOperator) {
                    ConditionOperator.Or -> sensorExits.any(::matches)
                    ConditionOperator.And -> sensorExits.isNotEmpty() && sensorExits.all(::matches)
                })
            if (maximumReached || exits) {
                phase.captures.forEach { capture ->
                    val sample = measurements.require(capture.metric, now)
                    require(sample.value >= 0) { "Negative transition measurement ${capture.metric}" }
                    captured[capture.id] = CapturedRecipeValue(capture.metric, sample)
                    events += RecipeRuntimeEvent("measurement_captured", mapOf("phase_id" to phase.id, "capture_id" to capture.id, "metric" to capture.metric.name, "value" to sample.value.toString(), "sample_at_ms" to sample.atMillis.toString()))
                }
                events += RecipeRuntimeEvent("phase_exited", mapOf("phase_id" to phase.id, "reason" to if (exits) "condition" else "maximum_duration"))
                if (phase.maximumDurationMillis > 0) previousTarget = phase.control.unit to target
                index++
                enteredAt = null
                continue
            }
            val fraction = if (phase.rampMillis == 0L) 1f else PhaseRamp(phase.rampStyle, phase.rampMillis).fraction(elapsed)
            var requested = if (phase.control == RecipeControl.Pause) 0f else rampFrom + (target - rampFrom) * fraction
            phase.flowProbe?.let { probe ->
                val measuredPressure = measurements.require(RecipeMetric.Pressure, now).value
                val flow = measurements.require(RecipeMetric.MachineFlow, now)
                if (elapsed < probe.samplingMillis) {
                    if (measuredPressure < probe.pressureCeilingBar) probeMaximum = maxOf(probeMaximum ?: flow.value, flow.value)
                    else requested = minOf(requested, probeMaximum ?: rampFrom)
                } else {
                    if (probeHold == null) probeHold = probeMaximum ?: rampFrom
                    if (measuredPressure >= probe.pressureCeilingBar && flow.atMillis != probeStamp) probeHold = minOf(requireNotNull(probeHold), (flow.value - probe.backoffMlPerSecond).coerceAtLeast(0f))
                    requested = requireNotNull(probeHold)
                }
                probeStamp = flow.atMillis
            }
            require(requested.isFinite() && requested >= 0) { "Invalid dynamic target" }
            if (phase.control == RecipeControl.MachineFlow) require(requested <= 12f) { "Dynamic machine-flow target exceeds 12 mL/s" }
            val ceiling = minOf(settings.pressureCeilingBar, phase.pressureLimitBar ?: settings.pressureCeilingBar)
            val constraints = mutableListOf<String>()
            val primaryPressure = when (phase.control) {
                RecipeControl.Pressure -> requested
                RecipeControl.Pause -> 0f
                RecipeControl.MachineFlow -> machineFeedback.update(requested, measurements.require(RecipeMetric.MachineFlow, now), ceiling)
                RecipeControl.CupFlow -> cupFeedback.update(requested, measurements.require(RecipeMetric.CupFlow, now), ceiling)
            }
            var bounded = primaryPressure.coerceIn(0f, ceiling)
            if (primaryPressure > ceiling) constraints += "pressure_ceiling"
            phase.machineFlowLimitMlPerSecond?.let { limit ->
                val measurement = measurements.require(RecipeMetric.MachineFlow, now)
                // Track the current pressure while the limiter is inactive; limit only excess flow.
                if (measurement.value <= limit) limiterFeedback.reset(bounded)
                else {
                    val allowed = limiterFeedback.update(limit, measurement, ceiling)
                    if (allowed < bounded) { bounded = allowed; constraints += "machine_flow_limit" }
                }
            }
            val delta = settings.pressureSlewBarPerSecond * (now - (lastTick ?: 0L)).coerceAtLeast(0) / 1_000f
            val applied = if (phase.control == RecipeControl.Pause) 0f else
                (pressure + (bounded - pressure).coerceIn(-delta, delta)).coerceIn(0f, ceiling)
            if (applied != bounded) constraints += "pressure_slew"
            pressure = applied
            lastTick = now
            return RecipeEngineOutput(phase.id, requested, phase.control.unit, primaryPressure, applied, constraints, events = events)
        }
        return finish("program_complete")
    }

    private fun resolve(value: RecipeValue, unit: RecipeUnit, now: Long, measurements: RecipeMeasurements): Float = when (value.source) {
        RecipeValueSource.Fixed -> value.value
        RecipeValueSource.CurrentMeasurement -> measurements.require(requireNotNull(value.metric), now).value
        RecipeValueSource.PreviousTarget -> requireNotNull(previousTarget?.takeIf { it.first == unit }) { "Previous target unit mismatch" }.second
        RecipeValueSource.Capture -> requireNotNull(captured[value.captureId]?.takeIf { it.metric.unit == unit }) { "Missing captured target" }.sample.value
    }
}

/** Each instance controls one measurement basis; integration advances only on a new sensor stamp. */
private class PressureFlowRegulator(private val gains: FlowControllerGains, private val base: Float) {
    private var integral = 0f
    private var pressure = base
    private var stamp: Long? = null
    private var offset = 0f

    fun reset(current: Float) { integral = 0f; pressure = current; stamp = null; offset = 0f }
    fun update(target: Float, sample: TimedRecipeValue, ceiling: Float): Float {
        require(sample.value >= 0) { "Negative flow measurement" }
        val error = target - sample.value
        val old = stamp
        if (old == null) {
            // Offset the proportional term at handover for a bumpless first sample.
            offset = pressure - base - gains.kp * error
        } else if (sample.atMillis > old) {
            val candidate = integral + error * (sample.atMillis - old) / 1_000f
            val unconstrained = base + offset + gains.kp * error + gains.ki * candidate
            if (unconstrained in 0f..ceiling || unconstrained > ceiling && error < 0 || unconstrained < 0 && error > 0) integral = candidate
            pressure = (base + offset + gains.kp * error + gains.ki * integral).coerceIn(0f, ceiling)
        }
        if (old == null || sample.atMillis > old) stamp = sample.atMillis
        return pressure.coerceIn(0f, ceiling)
    }
}
