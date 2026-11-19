package app.geeflow.presentation.feature.device.dashboard.livecontrol

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import app.geeflow.data.brew.model.ConditionOperator
import app.geeflow.data.brew.model.MeasurementScope
import app.geeflow.data.brew.model.RampStyle
import app.geeflow.data.brew.model.ThresholdComparison
import app.geeflow.domain.brew.usecase.GetBrewProfileUseCase
import app.geeflow.domain.device.recipe.*
import app.geeflow.navigation.NavEvent
import app.geeflow.navigation.Navigator
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import kotlin.time.Clock
import kotlin.random.Random

@Serializable
internal data class LiveRecipeEditorRoute(val deviceId: Long, val recipeId: String? = null, val legacyProfileId: Long? = null) : NavKey

internal fun newLiveRecipeId() = "local-${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt(1_000_000)}"

@Composable
internal fun LegacyLiveRecipeEditorScreen(route: LiveRecipeEditorRoute, navigator: Navigator) {
    val starter = rememberLiveBrewServiceStarter()
    val saved by starter.recipes.recipes.collectAsState()
    val getProfile = koinInject<GetBrewProfileUseCase>()
    var document by remember(route) { mutableStateOf(saved.find { it.id == route.recipeId } ?: LiveRecipeDocument(
        id = newLiveRecipeId(), name = "New live recipe", phases = listOf(RecipePhase("phase-1")),
        settings = RecipeShotSettings(temperatureC = 92),
    )) }
    var selected by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(starter.recipes.loadError) }
    var jsonDialog by remember { mutableStateOf(false) }
    var jsonText by remember { mutableStateOf("") }
    var cupTarget by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val files = rememberLiveRecipeFiles(onImported = { text ->
        runCatching { LiveRecipeImport.parse(text, newLiveRecipeId()) }.onSuccess {
            document = it; selected = 0; message = "Imported locally. Review mappings and compatibility before running."
        }.onFailure { message = it.message }
    }, onMessage = { message = it })
    LaunchedEffect(route.legacyProfileId) {
        route.legacyProfileId?.let { id ->
            runCatching { LiveRecipeImport.fromProfile(requireNotNull(getProfile(id)), newLiveRecipeId()) }
                .onSuccess { document = it; selected = 0 }.onFailure { message = it.message }
        }
    }
    fun save(): Boolean = runCatching { starter.recipes.save(document) }.fold({ message = "Saved locally"; true }, { message = it.message; false })
    val issues = remember(document) { LiveRecipeCompiler.issues(document) }
    key(document.id) { Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = { navigator.navigate(NavEvent.Back) }) { Text("Back") }
            Text("Live recipe editor", style = MaterialTheme.typography.headlineSmall)
            Text("Local experimental recipe. Machine flow uses mL/s; cup flow uses g/s. Temperature stays constant during the shot.")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { save() }) { Text("Save") }
                Button(enabled = issues.isEmpty(), onClick = {
                    if (save()) navigator.navigate(NavEvent.To(LiveBrewTestRoute(route.deviceId, document.id)))
                }) { Text("Capture / run") }
                OutlinedButton(onClick = { files.openImport() }) { Text("Import file") }
                OutlinedButton(onClick = { jsonText = ""; jsonDialog = true }) { Text("Import JSON") }
                OutlinedButton(onClick = {
                    runCatching { LiveRecipeJson.encode(document) }.onSuccess { text ->
                        clipboard.setText(AnnotatedString(text)); files.export("${document.id}.json", text)
                    }.onFailure { message = it.message }
                }) { Text("Export JSON") }
                OutlinedButton(onClick = { document = document.copy(id = newLiveRecipeId(), name = document.name + " copy"); message = "Editing a new local copy" }) { Text("Duplicate") }
                TextButton(onClick = { runCatching { starter.recipes.delete(document.id) }.onSuccess { navigator.navigate(NavEvent.Back) }.onFailure { message = it.message } }) { Text("Delete local recipe") }
            }
            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            issues.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
            RText("Recipe name", document.name) { document = document.copy(name = it) }
            val settings = document.settings
            RFloat("Dose (g)", settings.doseGrams) { document = document.copy(settings = settings.copy(doseGrams = it)) }
            RChoice("Yield mode", settings.useRatio, listOf(false, true), { if (it) "Dose × ratio" else "Manual yield" }) { document = document.copy(settings = settings.copy(useRatio = it)) }
            if (settings.useRatio) RFloat("Brew ratio", settings.ratio) { document = document.copy(settings = settings.copy(ratio = it)) }
            else RFloat("Target cup weight (g)", settings.targetWeightGrams) { document = document.copy(settings = settings.copy(targetWeightGrams = it)) }
            ROptionalFloat("Global pumped-volume limit (mL)", settings.maximumVolumeMl) { document = document.copy(settings = settings.copy(maximumVolumeMl = it)) }
            Text("Stop yield: ${settings.yieldGrams} g. Changing yield does not rescale phases.")
            RText("Constant boiler temperature (whole °C; blank keeps current)", settings.temperatureC?.toString().orEmpty()) { document = document.copy(settings = settings.copy(temperatureC = if (it.isBlank()) null else it.toIntOrNull() ?: -1)) }
            RLong("Overall deadline (ms)", settings.deadlineMillis) { document = document.copy(settings = settings.copy(deadlineMillis = it)) }
            RFloat("Pressure ceiling (bar)", settings.pressureCeilingBar) { document = document.copy(settings = settings.copy(pressureCeilingBar = it)) }
            RFloat("Pressure slew (bar/s)", settings.pressureSlewBarPerSecond) { document = document.copy(settings = settings.copy(pressureSlewBarPerSecond = it)) }
            RFloat("Flow-controller base pressure (bar)", settings.basePressureBar) { document = document.copy(settings = settings.copy(basePressureBar = it)) }
            RChoice("Write interval (ms)", settings.commandIntervalMillis, listOf(100L, 250L, 500L)) { document = document.copy(settings = settings.copy(commandIntervalMillis = it)) }
            RFloat("Machine flow Kp (bar per mL/s)", settings.machineFlowGains.kp) { document = document.copy(settings = settings.copy(machineFlowGains = settings.machineFlowGains.copy(kp = it))) }
            RFloat("Machine flow Ki (bar per mL)", settings.machineFlowGains.ki) { document = document.copy(settings = settings.copy(machineFlowGains = settings.machineFlowGains.copy(ki = it))) }
            RFloat("Cup flow Kp (bar per g/s)", settings.cupFlowGains.kp) { document = document.copy(settings = settings.copy(cupFlowGains = settings.cupFlowGains.copy(kp = it))) }
            RFloat("Cup flow Ki (bar per g)", settings.cupFlowGains.ki) { document = document.copy(settings = settings.copy(cupFlowGains = settings.cupFlowGains.copy(ki = it))) }
            if (document.mappings.isNotEmpty()) {
                Text("Source mappings", style = MaterialTheme.typography.titleMedium)
                Text(document.sourceIdentity.orEmpty())
                document.mappings.forEach { Text("• $it") }
                Row { Checkbox(document.mappingsAccepted, { document = document.copy(mappingsAccepted = it) }); Text("Accept these execution mappings") }
            }
            Text("Phases", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                document.phases.forEachIndexed { index, phase -> TextButton(onClick = { selected = index }) { Text("${index + 1}. ${phase.name}") } }
                OutlinedButton(onClick = { document = document.copy(phases = document.phases + RecipePhase(newLiveRecipeId(), "New phase")); selected = document.phases.lastIndex }) { Text("Add phase") }
            }
            document.phases.getOrNull(selected)?.let { phase ->
                key(document.id, selected, phase.id, phase.control) {
                    RecipePhaseEditor(phase, { updated -> document = document.copy(phases = document.phases.toMutableList().also { it[selected] = updated }) })
                }
                Row {
                    TextButton(enabled = selected > 0, onClick = {
                        val list = document.phases.toMutableList(); val item = list.removeAt(selected); list.add(selected - 1, item); selected--; document = document.copy(phases = list)
                    }) { Text("Move up") }
                    TextButton(enabled = selected < document.phases.lastIndex, onClick = {
                        val list = document.phases.toMutableList(); val item = list.removeAt(selected); list.add(selected + 1, item); selected++; document = document.copy(phases = list)
                    }) { Text("Move down") }
                    TextButton(onClick = { document = document.copy(phases = document.phases.filterIndexed { index, _ -> index != selected }); selected = 0 }) { Text("Remove phase") }
                }
                if (phase.control == RecipeControl.MachineFlow) {
                    RText("Explicit cup-flow end target (g/s)", cupTarget) { cupTarget = it }
                    OutlinedButton(onClick = {
                        runCatching { LiveRecipeImport.cupFlowCopy(document, phase.id, requireNotNull(cupTarget.toFloatOrNull()), newLiveRecipeId()) }
                            .onSuccess { document = it; message = "Editing a cup-flow copy. Review the new capture and target." }.onFailure { message = it.message }
                    }) { Text("Create cup-flow adaptation") }
                }
            }
            if (document.originalSourceJson != null) Text("Original source is retained in the exported document.")
        }
    }
    }
    if (jsonDialog) AlertDialog(onDismissRequest = { jsonDialog = false }, title = { Text("Import GeeFlow live or Decent JSON v2") }, text = {
        OutlinedTextField(jsonText, { jsonText = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 420.dp))
    }, confirmButton = { TextButton(onClick = {
        runCatching { LiveRecipeImport.parse(jsonText, newLiveRecipeId()) }.onSuccess {
            document = it; selected = 0; jsonDialog = false; message = "Imported locally. Review source mappings."
        }.onFailure { message = it.message }
    }) { Text("Import") } }, dismissButton = { TextButton(onClick = { jsonDialog = false }) { Text("Cancel") } })
}

@Composable
private fun RecipePhaseEditor(phase: RecipePhase, change: (RecipePhase) -> Unit) {
    RText("Phase name", phase.name) { change(phase.copy(name = it)) }
    Text("Phase ID: ${phase.id}")
    RChoice("Control", phase.control, RecipeControl.entries, { "${it.name} (${unitLabel(it.unit)})" }) { control ->
        change(phase.copy(control = control, target = if (control.unit != phase.control.unit) RecipeValue(value = Float.NaN) else phase.target))
    }
    RLong("Maximum duration (ms; zero = transition action)", phase.maximumDurationMillis) { change(phase.copy(maximumDurationMillis = it)) }
    RLong("Minimum duration (ms)", phase.minimumDurationMillis) { change(phase.copy(minimumDurationMillis = it)) }
    if (phase.maximumDurationMillis > 0 && phase.control != RecipeControl.Pause) {
        RecipeValueEditor("Target (${unitLabel(phase.control.unit)})", phase.target, phase.control.unit) { change(phase.copy(target = it)) }
        RLong("Ramp duration (ms; zero = fast)", phase.rampMillis) { change(phase.copy(rampMillis = it)) }
        if (phase.rampMillis > 0) {
            RChoice("Ramp shape", phase.rampStyle, RampStyle.entries) { change(phase.copy(rampStyle = it)) }
            RecipeValueEditor("Ramp start (${unitLabel(phase.control.unit)})", phase.rampStart, phase.control.unit) { change(phase.copy(rampStart = it)) }
        }
    }
    ROptionalFloat("Phase pressure limit (bar)", phase.pressureLimitBar) { change(phase.copy(pressureLimitBar = it)) }
    ROptionalFloat("Phase machine-flow limit (mL/s)", phase.machineFlowLimitMlPerSecond) { change(phase.copy(machineFlowLimitMlPerSecond = it)) }
    phase.sourceTemperatureC?.let { Text("Source stage temperature: $it°C; execution uses recipe constant.") }
    if (phase.control == RecipeControl.MachineFlow) {
        Row { Checkbox(phase.flowProbe != null, { enabled -> change(phase.copy(flowProbe = if (enabled) RecipeFlowProbe(1_000, 8f) else null)) }); Text("Probe and hold machine flow below a pressure ceiling") }
        phase.flowProbe?.let { probe ->
            RLong("Probe sampling duration (ms)", probe.samplingMillis) { change(phase.copy(flowProbe = probe.copy(samplingMillis = it))) }
            RFloat("Probe pressure ceiling (bar)", probe.pressureCeilingBar) { change(phase.copy(flowProbe = probe.copy(pressureCeilingBar = it))) }
            RFloat("Probe flow backoff (mL/s)", probe.backoffMlPerSecond) { change(phase.copy(flowProbe = probe.copy(backoffMlPerSecond = it))) }
        }
    }
    RChoice("Measurement exits", phase.conditionOperator, ConditionOperator.entries) { change(phase.copy(conditionOperator = it)) }
    Text("Time limits always act as deadlines.")
    phase.exits.forEachIndexed { index, exit -> key(index) {
        RChoice("Exit ${index + 1} metric", exit.metric, RecipeMetric.entries, { "${it.name} (${unitLabel(it.unit)})" }) { metric -> change(phase.copy(exits = phase.exits.toMutableList().also { it[index] = exit.copy(metric = metric) })) }
        RChoice("Comparison", exit.comparison, ThresholdComparison.entries) { value -> change(phase.copy(exits = phase.exits.toMutableList().also { it[index] = exit.copy(comparison = value) })) }
        RFloat("Threshold (${unitLabel(exit.metric.unit)})", exit.threshold) { value -> change(phase.copy(exits = phase.exits.toMutableList().also { it[index] = exit.copy(threshold = value) })) }
        RChoice("Measurement scope", exit.scope, MeasurementScope.entries) { value -> change(phase.copy(exits = phase.exits.toMutableList().also { it[index] = exit.copy(scope = value) })) }
        TextButton(onClick = { change(phase.copy(exits = phase.exits.filterIndexed { i, _ -> i != index })) }) { Text("Remove exit") }
    } }
    OutlinedButton(onClick = { change(phase.copy(exits = phase.exits + RecipeExit(RecipeMetric.Pressure, threshold = 3f))) }) { Text("Add sensor exit") }
    phase.captures.forEachIndexed { index, capture -> key(index) {
        RText("Capture ID at phase exit", capture.id) { value -> change(phase.copy(captures = phase.captures.toMutableList().also { it[index] = capture.copy(id = value) })) }
        RChoice("Capture measurement", capture.metric, RecipeMetric.entries.filterNot { it == RecipeMetric.PhaseTime }) { value -> change(phase.copy(captures = phase.captures.toMutableList().also { it[index] = capture.copy(metric = value) })) }
        TextButton(onClick = { change(phase.copy(captures = phase.captures.filterIndexed { i, _ -> i != index })) }) { Text("Remove capture") }
    } }
    OutlinedButton(onClick = { change(phase.copy(captures = phase.captures + RecipeCapture("${phase.id}-capture-${phase.captures.size}", RecipeMetric.MachineFlow))) }) { Text("Capture a measurement at exit") }
}

@Composable
private fun RecipeValueEditor(label: String, value: RecipeValue, unit: RecipeUnit, change: (RecipeValue) -> Unit) {
    RChoice(label, value.source, RecipeValueSource.entries) { change(value.copy(source = it, metric = value.metric ?: RecipeMetric.entries.first { metric -> metric.unit == unit })) }
    when (value.source) {
        RecipeValueSource.Fixed -> RFloat(label, value.value) { change(value.copy(value = it)) }
        RecipeValueSource.CurrentMeasurement -> RChoice("Measurement", value.metric ?: RecipeMetric.entries.first { it.unit == unit }, RecipeMetric.entries.filter { it.unit == unit }) { change(value.copy(metric = it)) }
        RecipeValueSource.Capture -> RText("Earlier capture ID", value.captureId.orEmpty()) { change(value.copy(captureId = it)) }
        RecipeValueSource.PreviousTarget -> Text("Uses the preceding timed phase's target; units must match.")
    }
}

@Composable internal fun RText(label: String, value: String, change: (String) -> Unit) {
    OutlinedTextField(value, change, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
}
@Composable private fun RFloat(label: String, value: Float, change: (Float) -> Unit) {
    var text by remember { mutableStateOf(value.toString()) }
    OutlinedTextField(text, { text = it; change(it.toFloatOrNull() ?: Float.NaN) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true, isError = !value.isFinite())
}
@Composable private fun RLong(label: String, value: Long, change: (Long) -> Unit) {
    var text by remember { mutableStateOf(value.toString()) }
    OutlinedTextField(text, { text = it; change(it.toLongOrNull() ?: -1) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true, isError = value < 0)
}
@Composable private fun ROptionalFloat(label: String, value: Float?, change: (Float?) -> Unit) {
    var text by remember { mutableStateOf(value?.toString().orEmpty()) }
    OutlinedTextField(text, { text = it; change(if (it.isBlank()) null else it.toFloatOrNull() ?: Float.NaN) }, label = { Text("$label; blank = none") }, modifier = Modifier.fillMaxWidth(), singleLine = true, isError = value?.isFinite() == false)
}
@Composable internal fun <T> RChoice(label: String, value: T, values: List<T>, title: (T) -> String = { it.toString() }, change: (T) -> Unit) {
    Text("$label · ${title(value)}", style = MaterialTheme.typography.labelLarge)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        values.forEach { item -> FilterChip(selected = item == value, onClick = { change(item) }, label = { Text(title(item)) }) }
    }
}
private fun unitLabel(unit: RecipeUnit) = when (unit) {
    RecipeUnit.Bar -> "bar"; RecipeUnit.MillilitresPerSecond -> "mL/s"; RecipeUnit.GramsPerSecond -> "g/s"
    RecipeUnit.Grams -> "g"; RecipeUnit.Millilitres -> "mL"; RecipeUnit.Seconds -> "s"
}
