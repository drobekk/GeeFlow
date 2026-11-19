package app.geeflow.domain.device.recipe

import app.geeflow.data.brew.model.ConditionOperator
import app.geeflow.data.brew.model.MeasurementScope
import app.geeflow.data.brew.model.RampStyle
import app.geeflow.data.brew.model.ThresholdComparison
import app.geeflow.data.device.model.ProfilingCapabilities
import app.geeflow.data.brew.model.PressureLocation
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable enum class RecipeUnit { Bar, MillilitresPerSecond, GramsPerSecond, Grams, Millilitres, Seconds }
@Serializable enum class RecipeMetric(val unit: RecipeUnit) {
    Pressure(RecipeUnit.Bar), MachineFlow(RecipeUnit.MillilitresPerSecond), CupFlow(RecipeUnit.GramsPerSecond),
    Weight(RecipeUnit.Grams), Volume(RecipeUnit.Millilitres), PhaseTime(RecipeUnit.Seconds),
}
@Serializable enum class RecipeControl(val unit: RecipeUnit) {
    Pressure(RecipeUnit.Bar), MachineFlow(RecipeUnit.MillilitresPerSecond), CupFlow(RecipeUnit.GramsPerSecond), Pause(RecipeUnit.Bar),
}
@Serializable enum class RecipeValueSource { Fixed, CurrentMeasurement, PreviousTarget, Capture }
@Serializable data class RecipeValue(
    val source: RecipeValueSource = RecipeValueSource.Fixed,
    val value: Float = 0f,
    val metric: RecipeMetric? = null,
    val captureId: String? = null,
)
@Serializable data class RecipeCapture(val id: String, val metric: RecipeMetric)
@Serializable data class RecipeFlowProbe(val samplingMillis: Long, val pressureCeilingBar: Float, val backoffMlPerSecond: Float = .1f)
@Serializable data class RecipeExit(
    val metric: RecipeMetric,
    val comparison: ThresholdComparison = ThresholdComparison.Above,
    val threshold: Float,
    val scope: MeasurementScope = MeasurementScope.Total,
)
@Serializable data class RecipePhase(
    val id: String,
    val name: String = id,
    val control: RecipeControl = RecipeControl.Pressure,
    val target: RecipeValue = RecipeValue(value = 3f),
    val maximumDurationMillis: Long = 5_000,
    val minimumDurationMillis: Long = 0,
    val rampMillis: Long = 0,
    val rampStyle: RampStyle = RampStyle.Linear,
    val rampStart: RecipeValue = RecipeValue(source = RecipeValueSource.PreviousTarget),
    val exits: List<RecipeExit> = emptyList(),
    val conditionOperator: ConditionOperator = ConditionOperator.Or,
    val captures: List<RecipeCapture> = emptyList(),
    val pressureLimitBar: Float? = null,
    val machineFlowLimitMlPerSecond: Float? = null,
    val sourceTemperatureC: Float? = null,
    val flowProbe: RecipeFlowProbe? = null,
)
@Serializable data class FlowControllerGains(val kp: Float = .5f, val ki: Float = .1f)
@Serializable data class RecipeShotSettings(
    val temperatureC: Int? = null,
    val targetWeightGrams: Float = 36f,
    val doseGrams: Float = 18f,
    val ratio: Float = 2f,
    val useRatio: Boolean = false,
    val deadlineMillis: Long = 40_000,
    val pressureCeilingBar: Float = 9f,
    val pressureSlewBarPerSecond: Float = 1f,
    val basePressureBar: Float = 3f,
    val commandIntervalMillis: Long = 100,
    val machineFlowGains: FlowControllerGains = FlowControllerGains(),
    val cupFlowGains: FlowControllerGains = FlowControllerGains(),
    val maximumVolumeMl: Float? = null,
) {
    val yieldGrams: Float get() = if (useRatio) doseGrams * ratio else targetWeightGrams
}
@Serializable data class LiveRecipeDocument(
    val version: Int = 1,
    val id: String,
    val name: String,
    val phases: List<RecipePhase>,
    val settings: RecipeShotSettings = RecipeShotSettings(),
    val sourceIdentity: String? = null,
    val originalSourceJson: String? = null,
    val mappings: List<String> = emptyList(),
    val unsupportedFeatures: List<String> = emptyList(),
    val mappingsAccepted: Boolean = false,
    val favorite: Boolean = false,
)

object LiveRecipeJson {
    private val json = Json { prettyPrint = true; encodeDefaults = true }
    fun encode(document: LiveRecipeDocument): String = json.encodeToString(document)
    fun decode(text: String): LiveRecipeDocument = json.decodeFromString(text)
}

class CompiledLiveRecipe internal constructor(val document: LiveRecipeDocument)

/** The runtime consumes a detached snapshot; no mutable editor collections cross this boundary. */
object LiveRecipeCompiler {
    fun issues(document: LiveRecipeDocument, capabilities: ProfilingCapabilities? = null): List<String> = buildList {
        fun check(ok: Boolean, message: String) { if (!ok) add(message) }
        val settings = document.settings
        check(document.version == 1, "Unsupported live recipe version ${document.version}")
        check(document.id.isNotBlank() && document.name.isNotBlank(), "Recipe ID and name are required")
        check(document.phases.isNotEmpty(), "Recipe needs a phase")
        check(document.phases.map { it.id }.distinct().size == document.phases.size, "Phase IDs must be unique")
        check(settings.yieldGrams.isFinite() && settings.yieldGrams > 0, "Positive finite yield is required")
        settings.maximumVolumeMl?.let { check(it.isFinite() && it > 0, "Global volume limit must be positive and finite") }
        check(settings.doseGrams.isFinite() && settings.doseGrams > 0 && settings.ratio.isFinite() && settings.ratio > 0, "Dose and ratio must be positive")
        check(settings.deadlineMillis in 1..86_400_000, "Deadline must be between 1 ms and 24 hours")
        check(settings.temperatureC == null || settings.temperatureC in 60..100, "Temperature must be 60–100°C")
        check(settings.pressureCeilingBar.isFinite() && settings.pressureCeilingBar > 0 && settings.pressureCeilingBar <= 12, "Pressure ceiling must be above zero and no more than 12 bar")
        check(settings.basePressureBar.isFinite() && settings.basePressureBar in 0f..settings.pressureCeilingBar, "Base pressure exceeds the ceiling")
        check(settings.pressureSlewBarPerSecond.isFinite() && settings.pressureSlewBarPerSecond in .1f..3f, "Pressure slew must be 0.1–3 bar/s")
        check(settings.commandIntervalMillis in listOf(100L, 250L, 500L), "Command interval must be 100, 250 or 500 ms")
        listOf(settings.machineFlowGains, settings.cupFlowGains).forEach { gains ->
            check(gains.kp.isFinite() && gains.kp in 0f..2f && gains.ki.isFinite() && gains.ki in 0f..1f, "Invalid flow controller gains")
        }
        addAll(document.unsupportedFeatures)
        if (capabilities != null) {
            val pressure = capabilities.livePressure[PressureLocation.Pump]
            check(pressure != null && settings.pressureCeilingBar <= pressure.maximum, "Machine cannot provide the configured pump pressure range")
            check(settings.commandIntervalMillis >= capabilities.minimumWriteIntervalMillis, "Machine requires a slower command interval")
            check(document.mappings.isEmpty() || document.mappingsAccepted, "Review and accept source mappings before live execution")
            val required = (if (settings.maximumVolumeMl != null) listOf(RecipeMetric.Volume) else emptyList()) + document.phases.flatMap { phase -> phase.exits.map { it.metric } + phase.captures.map { it.metric } +
                listOfNotNull(phase.target.metric, phase.rampStart.metric) +
                (if (phase.control == RecipeControl.MachineFlow || phase.machineFlowLimitMlPerSecond != null || phase.flowProbe != null) listOf(RecipeMetric.MachineFlow) else emptyList()) }
            required.distinct().forEach { metric ->
                val native = when (metric) {
                    RecipeMetric.Pressure -> app.geeflow.data.brew.model.BrewMetric.PumpPressure
                    RecipeMetric.MachineFlow -> app.geeflow.data.brew.model.BrewMetric.PumpFlow
                    RecipeMetric.Volume -> app.geeflow.data.brew.model.BrewMetric.PumpedVolume
                    else -> null // Weight and cup flow come from the independently connected scale.
                }
                check(native == null || native in capabilities.telemetry, "Machine does not expose required ${metric.name} telemetry")
            }
        }
        val captures = mutableMapOf<String, RecipeUnit>()
        var previousUnit: RecipeUnit? = null
        document.phases.forEach { phase ->
            val prefix = "${phase.name}: "
            check(phase.id.isNotBlank(), prefix + "phase ID is required")
            check(phase.maximumDurationMillis in 0..86_400_000 && phase.minimumDurationMillis in 0..phase.maximumDurationMillis, prefix + "invalid duration")
            check(phase.rampMillis in 0..phase.maximumDurationMillis, prefix + "invalid ramp duration")
            check(phase.control != RecipeControl.Pause || phase.rampMillis == 0L, prefix + "pause cannot have a ramp")
            fun value(v: RecipeValue) {
                when (v.source) {
                    RecipeValueSource.Fixed -> check(v.value.isFinite() && v.value >= 0, prefix + "invalid target")
                    RecipeValueSource.CurrentMeasurement -> check(v.metric?.unit == phase.control.unit && v.metric != RecipeMetric.PhaseTime, prefix + "measurement unit does not match control")
                    RecipeValueSource.PreviousTarget -> check(previousUnit == phase.control.unit, prefix + "previous target has a different unit or is missing")
                    RecipeValueSource.Capture -> check(captures[v.captureId] == phase.control.unit, prefix + "capture is missing, forward-referenced, or has a different unit")
                }
            }
            if (phase.maximumDurationMillis > 0 && phase.control != RecipeControl.Pause) {
                value(phase.target)
                if (phase.target.source == RecipeValueSource.Fixed) {
                    val upper = when (phase.control) { RecipeControl.Pressure -> settings.pressureCeilingBar; RecipeControl.MachineFlow -> 12f; else -> Float.MAX_VALUE }
                    check(phase.target.value <= upper, prefix + "target exceeds executable range")
                }
                if (phase.rampMillis > 0) value(phase.rampStart)
            }
            phase.pressureLimitBar?.let { check(it.isFinite() && it > 0 && it <= settings.pressureCeilingBar, prefix + "invalid pressure limit") }
            phase.machineFlowLimitMlPerSecond?.let { check(it.isFinite() && it > 0 && it <= 12f, prefix + "invalid machine-flow limit") }
            phase.flowProbe?.let { probe ->
                check(phase.control == RecipeControl.MachineFlow && probe.samplingMillis in 1..phase.maximumDurationMillis && phase.rampMillis >= probe.samplingMillis, prefix + "flow probe requires a machine-flow ramp covering its sampling interval")
                check(probe.pressureCeilingBar.isFinite() && probe.pressureCeilingBar > 0 && probe.pressureCeilingBar <= settings.pressureCeilingBar && probe.backoffMlPerSecond.isFinite() && probe.backoffMlPerSecond >= 0, prefix + "invalid flow probe bounds")
            }
            phase.exits.forEach { exit ->
                check(exit.threshold.isFinite() && exit.threshold >= 0, prefix + "invalid exit threshold")
                check(exit.scope == MeasurementScope.Total || exit.metric in listOf(RecipeMetric.Weight, RecipeMetric.Volume), prefix + "step-relative metric must be weight or volume")
            }
            phase.captures.forEach { capture ->
                check(capture.id.isNotBlank() && capture.id !in captures && capture.metric != RecipeMetric.PhaseTime, prefix + "capture IDs must be unique and use a sensor measurement")
                captures[capture.id] = capture.metric.unit
            }
            if (phase.maximumDurationMillis > 0) previousUnit = phase.control.unit
        }
    }

    fun compile(document: LiveRecipeDocument, capabilities: ProfilingCapabilities? = null): CompiledLiveRecipe {
        val detached = LiveRecipeJson.decode(LiveRecipeJson.encode(document))
        val issues = issues(detached, capabilities)
        require(issues.isEmpty()) { issues.joinToString("\n") }
        return CompiledLiveRecipe(detached)
    }
}
