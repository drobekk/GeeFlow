package app.geeflow.domain.device.recipe

import app.geeflow.data.brew.model.*
import kotlinx.serialization.json.*
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object LiveRecipeImport {
    fun parse(text: String, id: String, sourceIdentity: String? = null): LiveRecipeDocument {
        require(text.length <= 2_000_000) { "Recipe file exceeds 2 MB" }
        val root = Json.parseToJsonElement(text).jsonObject
        return if ("steps" in root && "title" in root) decent(root, text, id, sourceIdentity)
        else LiveRecipeJson.decode(text).copy(id = id)
    }

    fun fromProfile(profile: BrewProfile, id: String): LiveRecipeDocument {
        val source = profile.program as? BrewProgram.Phases ?: error("Recordings retain their existing playback path; only phase recipes can be copied to live recipes")
        val unsupported = mutableListOf<String>()
        val phases = source.phases.map { phase ->
            val control = when (val value = phase.control) {
                is PhaseControl.Pressure -> {
                    if (value.location != PressureLocation.Pump) unsupported += "${phase.name}: ${value.location} pressure is not pump pressure"
                    RecipeControl.Pressure
                }
                is PhaseControl.Flow -> RecipeControl.MachineFlow
                PhaseControl.PumpPause -> RecipeControl.Pause
            }
            RecipePhase(
                id = phase.id, name = phase.name.ifBlank { phase.id }, control = control,
                target = RecipeValue(value = when (val value = phase.control) {
                    is PhaseControl.Pressure -> value.bar
                    is PhaseControl.Flow -> value.millilitresPerSecond
                    PhaseControl.PumpPause -> 0f
                }),
                maximumDurationMillis = phase.maximumDurationMillis, minimumDurationMillis = phase.minimumDurationMillis,
                rampMillis = phase.ramp.durationMillis, rampStyle = phase.ramp.style,
                rampStart = if (phase.ramp.start == RampStart.CurrentMeasurement) RecipeValue(RecipeValueSource.CurrentMeasurement, metric = metric(control))
                    else RecipeValue(RecipeValueSource.PreviousTarget),
                exits = phase.exitConditions.map { exit -> RecipeExit(metric(exit.metric), exit.comparison, exit.threshold, exit.scope) },
                conditionOperator = phase.conditionOperator,
                flowProbe = phase.adaptiveFlowProbe?.let { RecipeFlowProbe(it.samplingDurationMillis, it.pressureCeilingBar, it.flowBackoffMillilitresPerSecond) },
            )
        }
        val weight = (profile.finishCondition as? Condition.Weight)?.target ?: 36f
        val mappings = if (profile.finishCondition is Condition.Weight) emptyList() else listOf("The source has no cup-weight finish. Select a cup yield for live scale control; the original finish is retained as an exit where supported.")
        val volume = (profile.finishCondition as? Condition.Volume)?.target
        val ceiling = maxOf(9f, phases.filter { it.control == RecipeControl.Pressure }.maxOfOrNull { it.target.value } ?: 0f)
        return LiveRecipeDocument(id = id, name = profile.name, phases = phases, settings = RecipeShotSettings(targetWeightGrams = weight, maximumVolumeMl = volume, pressureCeilingBar = ceiling),
            sourceIdentity = "geeflow:${profile.id}", originalSourceJson = Json.encodeToString(profile), mappings = mappings, unsupportedFeatures = unsupported)
    }

    /** Explicit user conversion. It does not convert numbers between mass flow and machine flow. */
    fun cupFlowCopy(document: LiveRecipeDocument, phaseId: String, endTargetGramsPerSecond: Float, id: String): LiveRecipeDocument {
        require(endTargetGramsPerSecond.isFinite() && endTargetGramsPerSecond > 0)
        val index = document.phases.indexOfFirst { it.id == phaseId }
        require(index > 0 && document.phases[index].control == RecipeControl.MachineFlow) { "Select a machine-flow phase with a preceding measurement phase" }
        require(document.phases[index].flowProbe == null) { "A machine-flow probe cannot be converted into a cup-flow probe; remove or redesign it explicitly first" }
        val prior = (index - 1 downTo 0).firstOrNull { document.phases[it].maximumDurationMillis > 0 } ?: error("No capture phase")
        val captureId = "cup_${document.phases[index].id}"
        require(document.phases.none { phase -> phase.captures.any { it.id == captureId } }) { "Capture ID already exists" }
        val phases = document.phases.toMutableList()
        phases[prior] = phases[prior].copy(captures = phases[prior].captures + RecipeCapture(captureId, RecipeMetric.CupFlow))
        phases[index] = phases[index].copy(control = RecipeControl.CupFlow, target = RecipeValue(value = endTargetGramsPerSecond),
            rampStart = RecipeValue(RecipeValueSource.Capture, captureId = captureId))
        return document.copy(id = id, name = document.name + " · cup-flow adaptation", phases = phases,
            mappings = document.mappings + "Extraction uses cup flow in g/s with an explicitly chosen target; this is not a unit conversion from machine mL/s.", mappingsAccepted = false)
    }

    private fun decent(root: JsonObject, original: String, id: String, identity: String?): LiveRecipeDocument {
        val unsupported = mutableListOf<String>()
        val mappings = mutableListOf("Pressure targets and exits use LITA pump telemetry.",
            "One constant whole-degree boiler temperature is used instead of source stage coffee temperatures.",
            "Source fast transitions remain subject to pressure slew limits. Source limiter ranges are represented as hard bounds.")
        if (root["version"]?.jsonPrimitive?.content != "2") unsupported += "Only Decent JSON version 2 is supported"
        val knownRoot = setOf("title", "author", "notes", "beverage_type", "steps", "tank_temperature", "target_weight", "target_volume", "target_volume_count_start", "legacy_profile_type", "type", "lang", "hidden", "reference_file", "changes_since_last_espresso", "version")
        root.keys.filterNot { it in knownRoot }.forEach { unsupported += "Unsupported source field $it" }
        if ((root.optionalNumber("target_volume") ?: 0f) > 0) mappings += "Global stopping uses the selected cup yield and deadline; source global volume settings are retained as reference. Per-phase volume exits remain active."
        val steps = root.getValue("steps").jsonArray
        require(steps.isNotEmpty() && steps.size <= 512) { "Recipe must have 1–512 steps" }
        val known = setOf("name", "temperature", "sensor", "pump", "transition", "pressure", "flow", "seconds", "volume", "weight", "exit", "limiter")
        val phases = steps.mapIndexed { index, element ->
            val step = element.jsonObject
            step.keys.filterNot { it in known }.forEach { unsupported += "Step $index: unsupported field $it" }
            val pump = step.string("pump")
            val control = when (pump) { "pressure" -> RecipeControl.Pressure; "flow" -> RecipeControl.MachineFlow; else -> { unsupported += "Step $index: unsupported pump $pump"; RecipeControl.Pressure } }
            val transition = step.string("transition")
            if (transition !in listOf("fast", "smooth")) unsupported += "Step $index: unsupported transition $transition"
            if (step.string("sensor") !in setOf("coffee", "water")) unsupported += "Step $index: unsupported temperature sensor ${step.string("sensor")}"
            val duration = (step.number("seconds").toDouble() * 1_000).roundToLong()
            val exits = mutableListOf<RecipeExit>()
            step["exit"]?.jsonObject?.let { exit ->
                if (exit.keys.any { it !in setOf("type", "condition", "value") }) unsupported += "Step $index: unsupported exit fields"
                val metric = when (exit.string("type")) { "pressure" -> RecipeMetric.Pressure; "flow" -> RecipeMetric.MachineFlow; else -> { unsupported += "Step $index: unsupported exit metric"; RecipeMetric.Pressure } }
                val comparison = when (exit.string("condition")) { "over" -> ThresholdComparison.StrictlyAbove; "under" -> ThresholdComparison.StrictlyBelow; else -> { unsupported += "Step $index: unsupported exit comparison"; ThresholdComparison.Above } }
                exits += RecipeExit(metric, comparison, exit.number("value"))
            }
            step.optionalNumber("volume")?.takeIf { it > 0 }?.let { exits += RecipeExit(RecipeMetric.Volume, threshold = it, scope = MeasurementScope.Step) }
            step.optionalNumber("weight")?.takeIf { it > 0 }?.let { exits += RecipeExit(RecipeMetric.Weight, threshold = it, scope = MeasurementScope.Step) }
            val limiter = step["limiter"]?.jsonObject
            if (limiter?.keys?.any { it !in setOf("value", "range") } == true) unsupported += "Step $index: unsupported limiter fields"
            val limit = limiter?.optionalNumber("value")?.takeIf { it > 0 }
            RecipePhase(id = "step-$index", name = step.string("name"), control = control,
                target = RecipeValue(value = step.number(if (control == RecipeControl.Pressure) "pressure" else "flow")),
                maximumDurationMillis = duration, rampMillis = if (transition == "smooth") duration else 0,
                rampStart = RecipeValue(RecipeValueSource.PreviousTarget), exits = exits,
                pressureLimitBar = limit.takeIf { control == RecipeControl.MachineFlow },
                machineFlowLimitMlPerSecond = limit.takeIf { control == RecipeControl.Pressure },
                sourceTemperatureC = step.optionalNumber("temperature"))
        }.toMutableList()
        phases.indices.forEach { index ->
            val phase = phases[index]
            if (phase.rampMillis > 0) {
                val prior = (index - 1 downTo 0).firstOrNull { phases[it].maximumDurationMillis > 0 }
                if (prior == null) phases[index] = phase.copy(rampStart = RecipeValue(RecipeValueSource.CurrentMeasurement, metric = metric(phase.control)))
                else if (phases[prior].control != phase.control) {
                    val capture = RecipeCapture("transition-$index", metric(phase.control))
                    phases[prior] = phases[prior].copy(captures = phases[prior].captures + capture)
                    phases[index] = phase.copy(rampStart = RecipeValue(RecipeValueSource.Capture, captureId = capture.id))
                }
            }
        }
        val pressureMax = maxOf(3f, phases.mapNotNull { it.pressureLimitBar }.maxOrNull() ?: 0f,
            phases.filter { it.control == RecipeControl.Pressure }.maxOfOrNull { it.target.value } ?: 0f)
        val slew = phases.mapIndexedNotNull { index, phase ->
            if (phase.control != RecipeControl.Pressure || phase.rampMillis == 0L) null else {
                val prior = phases.take(index).lastOrNull { it.maximumDurationMillis > 0 && it.control == RecipeControl.Pressure }
                prior?.let { kotlin.math.abs(phase.target.value - it.target.value) * 1_000 / phase.rampMillis }
            }
        }.maxOrNull() ?: 1f
        val sourceWeight = root.optionalNumber("target_weight")?.takeIf { it > 0 } ?: 36f.also { mappings += "Source has no nonzero cup yield; select a yield before execution." }
        return LiveRecipeDocument(id = id, name = root.string("title"), phases = phases,
            settings = RecipeShotSettings(temperatureC = phases.lastOrNull { it.maximumDurationMillis > 0 }?.sourceTemperatureC?.roundToInt(), targetWeightGrams = sourceWeight,
                deadlineMillis = phases.sumOf { it.maximumDurationMillis }.coerceAtLeast(1), pressureCeilingBar = pressureMax,
                pressureSlewBarPerSecond = maxOf(1f, ceil(slew * 10) / 10)),
            sourceIdentity = identity ?: "decent-json-v2:${root.string("reference_file")}", originalSourceJson = original,
            mappings = mappings, unsupportedFeatures = unsupported)
    }

    private fun metric(control: RecipeControl) = when (control) {
        RecipeControl.MachineFlow -> RecipeMetric.MachineFlow
        RecipeControl.CupFlow -> RecipeMetric.CupFlow
        else -> RecipeMetric.Pressure
    }
    private fun metric(metric: BrewMetric): RecipeMetric = when (metric) {
        BrewMetric.PumpPressure -> RecipeMetric.Pressure
        BrewMetric.PumpFlow -> RecipeMetric.MachineFlow
        BrewMetric.CupWeight -> RecipeMetric.Weight
        BrewMetric.PumpedVolume -> RecipeMetric.Volume
        BrewMetric.PhaseTime -> RecipeMetric.PhaseTime
        else -> error("Unsupported source metric $metric")
    }
    private fun JsonObject.string(key: String) = get(key)?.jsonPrimitive?.content.orEmpty()
    private fun JsonObject.optionalNumber(key: String): Float? = get(key)?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }?.let {
        requireNotNull(it.toFloatOrNull()?.takeIf(Float::isFinite)) { "Invalid number for $key" }
    }
    private fun JsonObject.number(key: String) = requireNotNull(optionalNumber(key)) { "Missing numeric $key" }
}
