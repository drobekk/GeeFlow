package app.geeflow.domain.device

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewPhase
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.PressureLocation
import app.geeflow.data.device.model.BrewTelemetry
import app.geeflow.data.device.model.BookooScaleFrame
import kotlin.math.max
import app.geeflow.domain.device.recipe.*

sealed interface LiveControlTarget {
    data class Pressure(val bar: Float) : LiveControlTarget
    data class CupFlow(val gramsPerSecond: Float, val basePressureBar: Float = 3f) : LiveControlTarget
    data object Pause : LiveControlTarget
}

/** Experimental recipe wrapper leaves saved/cloud profile serialization untouched. */
data class LiveTestRecipe(val profile: BrewProfile, val targets: Map<String, LiveControlTarget>, val document: LiveRecipeDocument? = null) {
    init {
        val phases = (profile.program as? BrewProgram.Phases)?.phases
            ?: error("Live test recipes require phases")
        require(phases.all { it.id in targets })
        require(document != null || targets.values.all { target ->
            when (target) {
                is LiveControlTarget.Pressure -> target.bar.isFinite() && target.bar in 0f..9f
                is LiveControlTarget.CupFlow -> target.gramsPerSecond.isFinite() && target.gramsPerSecond >= 0f &&
                    target.basePressureBar.isFinite() && target.basePressureBar in 0f..9f
                LiveControlTarget.Pause -> true
            }
        })
        require(profile.finishCondition is Condition.Weight) { "Live tests require a final cup-weight target" }
    }

    fun evaluationProfile(): BrewProfile {
        val phases = (profile.program as BrewProgram.Phases).phases.map { phase ->
            phase.copy(control = when (val target = targets.getValue(phase.id)) {
                is LiveControlTarget.Pressure -> PhaseControl.Pressure(target.bar, PressureLocation.Pump)
                is LiveControlTarget.CupFlow -> PhaseControl.Pressure(target.basePressureBar, PressureLocation.Pump)
                LiveControlTarget.Pause -> PhaseControl.PumpPause
            })
        }
        return profile.copy(program = BrewProgram.Phases(phases))
    }

    companion object {
        fun fromDocument(document: LiveRecipeDocument): LiveTestRecipe {
            val snapshot = LiveRecipeCompiler.compile(document).document
            val phases = snapshot.phases.map { phase -> BrewPhase(phase.id, phase.name, PhaseControl.Pressure(0f), phase.maximumDurationMillis) }
            return LiveTestRecipe(
                BrewProfile(userId = 0, name = snapshot.name, description = "Local live recipe", finishCondition = Condition.Weight(snapshot.settings.yieldGrams), program = BrewProgram.Phases(phases)),
                phases.associate { it.id to LiveControlTarget.Pressure(0f) }, snapshot,
            )
        }
        /** Simulation-only starting recipe. Its pressure and flow values are not physically tuned. */
        fun simulationDefaults(basePressureBar: Float = 3f): LiveTestRecipe {
            require(basePressureBar in 0f..9f)
            val phases = listOf(
                BrewPhase("preinfusion", "Preinfusion", PhaseControl.Pressure(basePressureBar), 5_000L),
                BrewPhase("cup-flow", "Cup flow", PhaseControl.Pressure(basePressureBar), 25_000L),
                BrewPhase("finish", "Finish", PhaseControl.Pressure(basePressureBar), 10_000L),
            )
            return LiveTestRecipe(
                profile = BrewProfile(
                    userId = 0,
                    name = "Experimental cup-flow test",
                    description = "Simulation defaults. Validate with passive capture before live control.",
                    finishCondition = Condition.Weight(36f),
                    program = BrewProgram.Phases(phases),
                ),
                targets = mapOf(
                    "preinfusion" to LiveControlTarget.Pressure(basePressureBar),
                    "cup-flow" to LiveControlTarget.CupFlow(1.5f, basePressureBar = basePressureBar),
                    "finish" to LiveControlTarget.Pressure(basePressureBar),
                ),
            )
        }
    }
}

data class LiveTargetOutput(val target: PhaseControl?, val phaseId: String?, val finish: String?)

/** Shares existing phase/exit timing while mapping cup-flow steps into pressure commands. */
class LiveTestProgramEngine(private val recipe: LiveTestRecipe, private val settings: LiveControlSettings) {
    private val timing = ProfileProgramEngine(recipe.evaluationProfile())
    private val feedback = LiveCupFlowController(settings)

    private var lastPhaseId: String? = null
    private var lastPressureBar = settings.basePressureBar

    fun tick(
        elapsedMillis: Long,
        telemetry: BrewTelemetry,
        cupFlowGramsPerSecond: Float?,
        nowMillis: Long,
        freshScaleMeasurement: Boolean = true,
    ): LiveTargetOutput {
        val step = timing.tick(elapsedMillis, telemetry)
        if (step.finish != null || step.phaseId == null) return LiveTargetOutput(null, step.phaseId, step.finish)
        if (step.phaseId != lastPhaseId) {
            val phaseBase = (step.target as? PhaseControl.Pressure)?.bar ?: settings.basePressureBar
            feedback.reset(basePressureBar = phaseBase, currentPressureBar = lastPressureBar)
            lastPhaseId = step.phaseId
        }
        return when (val target = recipe.targets.getValue(step.phaseId)) {
            is LiveControlTarget.Pressure -> {
                lastPressureBar = (step.target as? PhaseControl.Pressure)?.bar ?: target.bar
                LiveTargetOutput(PhaseControl.Pressure(lastPressureBar), step.phaseId, null)
            }
            is LiveControlTarget.CupFlow -> {
                val phaseBase = (step.target as? PhaseControl.Pressure)?.bar ?: target.basePressureBar
                if (freshScaleMeasurement && cupFlowGramsPerSecond != null) {
                    lastPressureBar = feedback.update(target.gramsPerSecond, cupFlowGramsPerSecond, nowMillis, phaseBase)
                }
                LiveTargetOutput(PhaseControl.Pressure(lastPressureBar), step.phaseId, null)
            }
            LiveControlTarget.Pause -> {
                lastPressureBar = 0f
                LiveTargetOutput(PhaseControl.PumpPause, step.phaseId, null)
            }
        }
    }

    fun resetFeedback(basePressureBar: Float) = feedback.reset(basePressureBar, lastPressureBar)
}

data class LiveControlSettings(
    val basePressureBar: Float = 3f,
    val maximumPressureBar: Float = 9f,
    val slewBarPerSecond: Float = 1f,
    val proportionalGainBarPerGramPerSecond: Float = .5f,
    val integralGainBarPerGram: Float = .1f,
    val commandIntervalMillis: Long = DEFAULT_COMMAND_INTERVAL_MILLIS,
    val shotDeadlineMillis: Long = 40_000L,
) {
    init {
        require(basePressureBar in 0f..maximumPressureBar && maximumPressureBar <= 12f)
        require(slewBarPerSecond in .1f..3f)
        require(proportionalGainBarPerGramPerSecond in 0f..2f && integralGainBarPerGram in 0f..1f)
        require(commandIntervalMillis in ALLOWED_COMMAND_INTERVALS_MILLIS)
        require(shotDeadlineMillis in 1L..86_400_000L)
    }

    companion object {
        const val DEFAULT_COMMAND_INTERVAL_MILLIS = 100L
        val ALLOWED_COMMAND_INTERVALS_MILLIS = listOf(100L, 250L, 500L)
    }
}

/** Estimates cup flow in g/s from arrival-timestamped scale weights. */
class CupFlowEstimator(
    private val windowMillis: Long = 500L,
    private val minimumWindowMillis: Long = minOf(200L, windowMillis),
) {
    private val samples = ArrayDeque<Pair<Long, Float>>()
    private var previousWeight: Float? = null

    fun add(arrivalMillis: Long, weightGrams: Float): Float? {
        if (!weightGrams.isFinite()) return null
        val previous = samples.lastOrNull()
        if (previous != null && arrivalMillis <= previous.first) return null
        if (previousWeight != null && weightGrams < previousWeight!! - MAX_WEIGHT_DROP_GRAMS) {
            samples.clear()
            previousWeight = weightGrams
            samples.addLast(arrivalMillis to weightGrams)
            return null
        }
        previousWeight = weightGrams
        samples.addLast(arrivalMillis to weightGrams)
        while (samples.size > 2 && arrivalMillis - samples.first().first > windowMillis) samples.removeFirst()
        if (samples.size < 3 || arrivalMillis - samples.first().first < minimumWindowMillis) return null

        val origin = samples.first().first
        val xs = samples.map { (it.first - origin).toDouble() / 1000.0 }
        val ys = samples.map { it.second.toDouble() }
        val xMean = xs.average()
        val yMean = ys.average()
        val denominator = xs.sumOf { (it - xMean) * (it - xMean) }
        if (denominator <= 0.0) return null
        return (xs.indices.sumOf { (xs[it] - xMean) * (ys[it] - yMean) } / denominator)
            .toFloat().takeIf { it.isFinite() && it >= 0f }
    }

    fun reset() { samples.clear(); previousWeight = null }

    private companion object {
        const val MAX_WEIGHT_DROP_GRAMS = 2f
    }
}

enum class ScaleInvalidReason { TimerReset, TareOrCupRemoval }

/** Invalidates the current shot on scale timer resets, tare changes, or cup removal. */
class LiveScaleSampleGuard(initial: BookooScaleFrame) {
    private var lastWeight = initial.weightGrams
    private var lastTimer = initial.elapsedMillis
    private var invalidReason: ScaleInvalidReason? = null

    fun accept(frame: BookooScaleFrame): ScaleInvalidReason? {
        if (invalidReason != null) return invalidReason
        invalidReason = when {
            frame.elapsedMillis + TIMER_RESET_TOLERANCE_MILLIS < lastTimer -> ScaleInvalidReason.TimerReset
            frame.weightGrams < lastWeight - MAX_WEIGHT_DROP_GRAMS -> ScaleInvalidReason.TareOrCupRemoval
            else -> null
        }
        if (invalidReason == null) {
            lastWeight = frame.weightGrams
            lastTimer = frame.elapsedMillis
        }
        return invalidReason
    }

    val isValid: Boolean get() = invalidReason == null

    private companion object {
        const val TIMER_RESET_TOLERANCE_MILLIS = 250L
        const val MAX_WEIGHT_DROP_GRAMS = 2f
    }
}

/** Bounded pressure PI controller. Call once for each fresh cup-flow estimate. */
class LiveCupFlowController(private val settings: LiveControlSettings) {
    private var integral = 0f
    private var pressure = settings.basePressureBar
    private var lastUpdateMillis: Long? = null
    private var activeBasePressure = settings.basePressureBar

    fun update(
        targetFlowGramsPerSecond: Float,
        measuredFlowGramsPerSecond: Float,
        atMillis: Long,
        basePressureBar: Float = activeBasePressure,
    ): Float {
        require(targetFlowGramsPerSecond.isFinite() && targetFlowGramsPerSecond >= 0f)
        require(measuredFlowGramsPerSecond.isFinite() && measuredFlowGramsPerSecond >= 0f)
        require(basePressureBar.isFinite() && basePressureBar in 0f..settings.maximumPressureBar)
        val error = targetFlowGramsPerSecond - measuredFlowGramsPerSecond
        val last = lastUpdateMillis
        if (last != null && atMillis > last) {
            val dt = (atMillis - last) / 1000f
            val candidateIntegral = integral + error * dt
            val unconstrained = basePressureBar +
                settings.proportionalGainBarPerGramPerSecond * error +
                settings.integralGainBarPerGram * candidateIntegral
            // Conditional integration prevents winding further into either pressure limit.
            if ((unconstrained in 0f..settings.maximumPressureBar) ||
                (unconstrained < 0f && error > 0f) || (unconstrained > settings.maximumPressureBar && error < 0f)
            ) integral = candidateIntegral
            val target = (basePressureBar + settings.proportionalGainBarPerGramPerSecond * error +
                settings.integralGainBarPerGram * integral).coerceIn(0f, settings.maximumPressureBar)
            val maxChange = settings.slewBarPerSecond * dt
            pressure += (target - pressure).coerceIn(-maxChange, maxChange)
        }
        activeBasePressure = basePressureBar
        lastUpdateMillis = atMillis
        return pressure.coerceIn(0f, settings.maximumPressureBar)
    }

    /** Clears integral history while carrying the current command across a phase boundary. */
    fun reset(basePressureBar: Float = settings.basePressureBar, currentPressureBar: Float = basePressureBar) {
        integral = 0f
        activeBasePressure = basePressureBar.coerceIn(0f, settings.maximumPressureBar)
        pressure = currentPressureBar.coerceIn(0f, settings.maximumPressureBar)
        lastUpdateMillis = null
    }
}

data class ScaleFallback(val pressureBar: Float, val stopAtMillis: Long)

fun scaleLossFallback(
    lastPressureBar: Float,
    nowMillis: Long,
    shotDeadlineMillis: Long,
    maximumPressureBar: Float = 9f,
): ScaleFallback = ScaleFallback(
    minOf(3f, max(0f, lastPressureBar), maximumPressureBar),
    minOf(nowMillis + 5_000L, shotDeadlineMillis),
)

/** Keeps a scale-loss fallback active for the remainder of the shot, even if samples return. */
fun scaleFallbackStartMillis(
    currentStartMillis: Long?,
    scaleDataValid: Boolean,
    scaleSampleAgeMillis: Long,
    nowMillis: Long,
    staleAfterMillis: Long = 1_000L,
): Long? = currentStartMillis ?: nowMillis.takeIf {
    !scaleDataValid || scaleSampleAgeMillis > staleAfterMillis
}

/** Coalesces targets while rate-limiting session dispatch and drops pending work on stop. */
class LiveTargetCommandGate(private val minimumIntervalMillis: Long = 100L) {
    private var pendingTarget: PhaseControl? = null
    private var lastDispatchMillis: Long? = null
    private var stopped = false

    init { require(minimumIntervalMillis > 0L) }

    fun offer(target: PhaseControl, nowMillis: Long): PhaseControl? {
        if (stopped) return null
        pendingTarget = target
        val last = lastDispatchMillis
        if (last != null && nowMillis - last < minimumIntervalMillis) return null
        return pendingTarget.also {
            pendingTarget = null
            lastDispatchMillis = nowMillis
        }
    }

    fun stop() {
        stopped = true
        pendingTarget = null
    }
}
