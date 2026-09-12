package app.geeflow.data.device.model

import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.PressureLocation
import app.geeflow.data.brew.model.RampStyle
import kotlin.math.round

data class TargetRange(val minimum: Float, val maximum: Float, val resolution: Float) {
    fun quantize(value: Float): Float = (round(value.coerceIn(minimum, maximum) / resolution) / (1f / resolution))
        .coerceIn(minimum, maximum)
}

/** Supported sensors are distinct from their current availability (e.g. a disconnected scale). */
data class ProfilingCapabilities(
    val native: NativeProfilingCapabilities? = null,
    val livePressure: Map<PressureLocation, TargetRange> = emptyMap(),
    val liveFlow: TargetRange? = null,
    val livePause: Boolean = false,
    val liveModeSwitch: Boolean = false,
    val minimumWriteIntervalMillis: Long = 200,
    val telemetry: Set<BrewMetric> = emptySet(),
    val binding: Boolean = false,
)

data class NativeProfilingCapabilities(
    val pressureLocations: Set<PressureLocation> = emptySet(),
    val flow: Boolean = false,
    val pause: Boolean = false,
    val ramps: Set<RampStyle> = setOf(RampStyle.Instant),
    val exitMetrics: Set<BrewMetric> = emptySet(),
    val requiresGlobalGoal: Boolean = true,
    val recording: NativeRecordingCapabilities? = null,
)

data class NativeRecordingCapabilities(val intervalMillis: Long, val maximumPoints: Int)

enum class ProfileIssueCode {
    InvalidProgram,
    NativeFeature,
    RecordingCapacity,
    MissingGlobalGoal,
    UnsupportedControl,
    UnsupportedMetric,
    ModeSwitch,
    NotAvailable,
    BindingUnsupported
}
data class ProfileIssue(val code: ProfileIssueCode, val phaseId: String? = null, val metric: BrewMetric? = null)
enum class ProfileExecution { Native, AppControlled, Unsupported }
data class ProfileSupport(
    val execution: ProfileExecution,
    val nativeIssues: List<ProfileIssue>,
    val issues: List<ProfileIssue> = emptyList(),
    val bindingAllowed: Boolean = false,
) {
    val experimental: Boolean get() = execution == ProfileExecution.AppControlled
}

data class BrewTelemetry(val values: Map<BrewMetric, Float>) {
    operator fun get(metric: BrewMetric): Float? = values[metric]?.takeIf { it.isFinite() }
}

fun DeviceState.pumpTelemetry(): BrewTelemetry = BrewTelemetry(
    buildMap {
        pressure?.let { put(BrewMetric.PumpPressure, it) }
        flowRate?.let { put(BrewMetric.PumpFlow, it) }
        volume?.let { put(BrewMetric.PumpedVolume, it) }
        if (smartScale?.isConnected == true) weight?.let { put(BrewMetric.CupWeight, it) }
    }
)

fun PhaseControl.metric(): BrewMetric? = when (this) {
    is PhaseControl.Pressure -> when (location) {
        PressureLocation.Pump -> BrewMetric.PumpPressure
        PressureLocation.Group -> BrewMetric.GroupPressure
        PressureLocation.Boiler -> BrewMetric.BoilerPressure
    }
    is PhaseControl.Flow -> BrewMetric.PumpFlow
    PhaseControl.PumpPause -> null
}

fun PhaseControl.value(): Float = when (this) {
    is PhaseControl.Pressure -> bar
    is PhaseControl.Flow -> millilitresPerSecond
    PhaseControl.PumpPause -> 0f
}

fun PhaseControl.withValue(value: Float): PhaseControl = when (this) {
    is PhaseControl.Pressure -> copy(bar = value)
    is PhaseControl.Flow -> copy(millilitresPerSecond = value)
    PhaseControl.PumpPause -> this
}

fun BrewProfile.requiredMetrics(): Set<BrewMetric> = buildSet {
    when (finishCondition) {
        is Condition.Weight -> add(BrewMetric.CupWeight)
        is Condition.Volume -> add(BrewMetric.PumpedVolume)
        null -> Unit
    }
    (program as? BrewProgram.Phases)?.phases?.forEach { phase ->
        addAll(phase.exitConditions.map { it.metric })
        if (phase.ramp.style != RampStyle.Instant) phase.control.metric()?.let { add(it) }
    }
}
