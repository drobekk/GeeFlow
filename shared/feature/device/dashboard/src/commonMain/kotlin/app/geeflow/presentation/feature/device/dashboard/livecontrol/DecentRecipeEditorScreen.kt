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
import app.geeflow.domain.device.recipe.DecentRecipe
import app.geeflow.domain.device.recipe.LiveRecipeCompiler
import app.geeflow.navigation.NavEvent
import app.geeflow.navigation.Navigator
import kotlinx.serialization.json.*

@Composable
internal fun LiveRecipeEditorScreen(route: LiveRecipeEditorRoute, navigator: Navigator) {
    val starter = rememberLiveBrewServiceStarter()
    val saved by starter.recipes.recipes.collectAsState()
    val existing = saved.find { it.id == route.recipeId }
    val source = remember(existing) { existing?.let(DecentRecipe::authoredSource) }
    if (route.legacyProfileId != null || (existing != null && source == null)) {
        LegacyLiveRecipeEditorScreen(route, navigator)
        return
    }
    var id by remember(route) { mutableStateOf(route.recipeId ?: newLiveRecipeId()) }
    var profile by remember(route, source) { mutableStateOf(source ?: DecentRecipe.newProfile()) }
    var selected by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var paste by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current
    fun importProfile(text: String) {
        runCatching { DecentRecipe.parse(text) }.onSuccess {
            profile = it
            id = newLiveRecipeId()
            selected = 0
            paste = null
            message = "Imported Decent JSON v2"
        }.onFailure { message = it.message }
    }
    val files = rememberLiveRecipeFiles(onImported = ::importProfile, onMessage = { message = it })
    val steps = profile.getValue("steps").jsonArray
    val compiled = remember(profile, id, existing?.favorite) {
        runCatching { DecentRecipe.compile(profile, id).copy(favorite = existing?.favorite == true) }
    }
    val issues = compiled.getOrNull()?.let { LiveRecipeCompiler.issues(it) }.orEmpty()
    fun save(): Boolean = runCatching { starter.recipes.save(compiled.getOrThrow()) }
        .fold({ message = "Saved locally"; true }, { message = it.message; false })
    fun updateSteps(values: List<JsonElement>) { profile = JsonObject(profile + ("steps" to JsonArray(values))) }

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = { navigator.navigate(NavEvent.Back) }) { Text("Back") }
            Text("Decent recipe editor", style = MaterialTheme.typography.headlineSmall)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { save() }, enabled = compiled.isSuccess) { Text("Save") }
                Button(onClick = {
                    if (save()) navigator.navigate(NavEvent.To(LiveBrewTestRoute(route.deviceId, id)))
                }, enabled = compiled.isSuccess && issues.isEmpty()) { Text("Capture / run") }
                OutlinedButton(onClick = { files.openImport() }) { Text("Import file") }
                OutlinedButton(onClick = { paste = "" }) { Text("Import JSON") }
                OutlinedButton(onClick = {
                    val json = DecentRecipe.encode(profile)
                    clipboard.setText(AnnotatedString(json))
                    files.export("decent-profile.json", json)
                }) { Text("Export JSON") }
            }
            message?.let { Text(it) }
            compiled.exceptionOrNull()?.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            issues.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
            JsonFields(profile, listOf(
                "title" to "Profile name", "author" to "Author", "notes" to "Notes",
                "target_weight" to "Target weight (g)", "target_volume" to "Target volume (mL)",
                "target_volume_count_start" to "Volume counting starts at step",
                "tank_temperature" to "Tank temperature (°C)",
            )) { profile = it }
            Text("Stages", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                steps.forEachIndexed { index, step ->
                    FilterChip(selected == index, { selected = index }, { Text(step.jsonObject.text("name")) })
                }
                TextButton(onClick = { updateSteps(steps + DecentRecipe.newStep()); selected = steps.size }) { Text("Add stage") }
            }
            steps.getOrNull(selected)?.jsonObject?.let { step ->
                key(selected) {
                    DecentStage(step) { updated ->
                        updateSteps(steps.toMutableList().also { it[selected] = updated })
                    }
                }
                Row {
                    TextButton(enabled = selected > 0, onClick = {
                        val reordered = steps.toMutableList()
                        reordered.add(selected - 1, reordered.removeAt(selected))
                        updateSteps(reordered)
                        selected--
                    }) { Text("Move up") }
                    TextButton(enabled = selected < steps.lastIndex, onClick = {
                        val reordered = steps.toMutableList()
                        reordered.add(selected + 1, reordered.removeAt(selected))
                        updateSteps(reordered)
                        selected++
                    }) { Text("Move down") }
                    TextButton(enabled = steps.size > 1, onClick = {
                        updateSteps(steps.filterIndexed { index, _ -> index != selected })
                        selected = selected.coerceAtMost(steps.lastIndex - 1)
                    }) { Text("Remove stage") }
                }
            }
        }
    }
    paste?.let { input ->
        AlertDialog(onDismissRequest = { paste = null }, title = { Text("Import Decent JSON v2") },
            text = { OutlinedTextField(input, { paste = it }, Modifier.heightIn(min = 160.dp, max = 380.dp)) },
            confirmButton = { TextButton(onClick = { importProfile(input) }) { Text("Import") } },
            dismissButton = { TextButton(onClick = { paste = null }) { Text("Cancel") } })
    }
}

@Composable
private fun DecentStage(step: JsonObject, change: (JsonObject) -> Unit) {
    fun set(key: String, value: JsonElement) = change(JsonObject(step + (key to value)))
    JsonFields(step, listOf("name" to "Stage name"), change)
    Text("Temperature", style = MaterialTheme.typography.titleMedium)
    JsonFields(step, listOf("temperature" to "Target (°C)"), change)
    JsonChoice(step, "sensor", "Temperature sensor", listOf("coffee", "water"), change)
    Text("Pump", style = MaterialTheme.typography.titleMedium)
    JsonChoice(step, "pump", "Pump control", listOf("pressure", "flow"), change)
    val pressure = step.text("pump") == "pressure"
    val targets = listOf("pressure" to "Pressure (bar)", "flow" to "Flow (mL/s)")
    JsonFields(step, (if (pressure) targets else targets.reversed()).map { (key, label) ->
        key to (if ((key == "pressure") == pressure) "Active target · $label" else label)
    }, change)
    JsonChoice(step, "transition", "Transition", listOf("fast", "smooth"), change)
    Text("Maximum", style = MaterialTheme.typography.titleMedium)
    JsonFields(step, listOf("seconds" to "Time (s)", "volume" to "Volume (mL)", "weight" to "Weight (g)"), change)
    Text("Move on if", style = MaterialTheme.typography.titleMedium)
    val exit = step["exit"] as? JsonObject
    if (exit == null) {
        OutlinedButton(onClick = {
            set("exit", JsonObject(mapOf("type" to JsonPrimitive("pressure"), "condition" to JsonPrimitive("over"), "value" to JsonPrimitive("3"))))
        }) { Text("Add exit") }
    } else {
        JsonChoice(exit, "type", "Measurement", listOf("pressure", "flow")) { set("exit", it) }
        JsonChoice(exit, "condition", "Comparison", listOf("over", "under")) { set("exit", it) }
        JsonFields(exit, listOf("value" to if (exit.text("type") == "pressure") "Threshold (bar)" else "Threshold (mL/s)")) { set("exit", it) }
        TextButton(onClick = { change(JsonObject(step - "exit")) }) { Text("Remove exit") }
    }
    Text("Limiter", style = MaterialTheme.typography.titleMedium)
    val unit = if (pressure) "mL/s" else "bar"
    JsonFields(step["limiter"] as? JsonObject ?: JsonObject(emptyMap()),
        listOf("value" to "Limit ($unit)", "range" to "Range ($unit)")) { set("limiter", it) }
}

private fun JsonObject.text(key: String) = (get(key) as? JsonPrimitive)?.content.orEmpty()

@Composable
private fun JsonFields(source: JsonObject, fields: List<Pair<String, String>>, change: (JsonObject) -> Unit) {
    fields.forEach { (key, label) ->
        RText(label, source.text(key)) { change(JsonObject(source + (key to JsonPrimitive(it)))) }
    }
}

@Composable
private fun JsonChoice(source: JsonObject, key: String, label: String, options: List<String>, change: (JsonObject) -> Unit) {
    RChoice(label, source.text(key), options) { change(JsonObject(source + (key to JsonPrimitive(it)))) }
}
