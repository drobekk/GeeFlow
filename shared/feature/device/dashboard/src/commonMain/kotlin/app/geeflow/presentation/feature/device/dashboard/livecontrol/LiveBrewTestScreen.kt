package app.geeflow.presentation.feature.device.dashboard.livecontrol

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.data.device.BookooScaleConnection
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.LiveTargetDiagnosticWriter
import app.geeflow.data.brew.model.PressureLocation
import app.geeflow.domain.device.LiveBrewTestCoordinator
import app.geeflow.domain.device.LiveControlSettings
import app.geeflow.domain.device.LiveModeProbePath
import app.geeflow.domain.device.LiveModeProbeSequence
import app.geeflow.domain.device.LiveModeSwitchProbeScript
import app.geeflow.domain.device.LiveTestMode
import app.geeflow.domain.device.LiveTestRecipe
import app.geeflow.domain.device.recipe.*
import app.geeflow.navigation.NavEvent
import app.geeflow.navigation.Navigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@kotlinx.serialization.Serializable
internal data class LiveBrewTestRoute(val deviceId: Long, val recipeId: String? = null) : androidx.navigation3.runtime.NavKey

@Composable
internal fun LiveBrewTestScreen(deviceId: Long, navigator: Navigator, recipeId: String? = null) {
    val coordinator = koinInject<LiveBrewTestCoordinator>()
    val scale = koinInject<BookooScaleConnection>()
    val provider = koinInject<DeviceControllerProvider>()
    val controller = remember(provider, deviceId) { provider.getController(deviceId) }
    val machine by controller.deviceState.collectAsStateWithLifecycle()
    val scaleState by scale.state.collectAsStateWithLifecycle()
    val run by coordinator.state.collectAsStateWithLifecycle()
    val starter = rememberLiveBrewServiceStarter()
    var mode by remember { mutableStateOf(LiveTestMode.PassiveCapture) }
    var probePath by remember { mutableStateOf(LiveModeProbePath.LiveModeRegister) }
    var probeSequence by remember { mutableStateOf(LiveModeProbeSequence.FlowPressureFlowPressure) }
    val localRecipes by starter.recipes.recipes.collectAsStateWithLifecycle()
    val stored = localRecipes.find { it.id == recipeId }
    val source = remember(stored) { stored?.let(DecentRecipe::authoredSource) }
    var temperatureOverride by remember(recipeId) { mutableStateOf("") }
    var mappingsAccepted by remember(recipeId) { mutableStateOf(false) }
    val document = remember(stored, source, temperatureOverride, mappingsAccepted) {
        if (source == null || stored == null) stored else DecentRecipe.compile(source, stored.id).let { compiled ->
            compiled.copy(
                settings = compiled.settings.copy(temperatureC = if (temperatureOverride.isBlank()) compiled.settings.temperatureC else temperatureOverride.toIntOrNull() ?: -1),
                mappingsAccepted = mappingsAccepted,
            )
        }
    }
    val compatibility = document?.let { LiveRecipeCompiler.issues(it, controller.profilingCapabilities) }.orEmpty()
    val commandProbeSupported = controller.brewing is LiveTargetDiagnosticWriter &&
        controller.profilingCapabilities.liveFlow != null && controller.profilingCapabilities.liveModeSwitch
    val nativePriorityProbeSupported = controller.profilingCapabilities.livePressure.containsKey(PressureLocation.Pump) &&
        controller.profilingCapabilities.liveFlow != null &&
        controller.profiles.assessNativeProfile(LiveModeSwitchProbeScript.nativeProfile(probeSequence)).isEmpty()
    val scaleProbeReady = scaleState.connected && scaleState.reading != null
    val scope = rememberCoroutineScope()
    var disconnectControlPath by remember { mutableStateOf("native_profile") }
    var errorText by remember { mutableStateOf<String?>(null) }
    val disconnectObservations = setOf(
        "autonomous_pump_stop_observed",
        "operator_intervention",
        "physical_stop_observed",
    )
    val lastDisconnectObservation = run.captureEvents.lastOrNull { it.type in disconnectObservations }?.type
    val disconnectStopUnconfirmed = run.stopPending && (
        run.mode == LiveTestMode.MachineDisconnectSimulation ||
            run.mode == LiveTestMode.LiveControl && run.message?.contains("Machine disconnected", ignoreCase = true) == true
        )
    var settings by remember(starter) { mutableStateOf(document?.controlSettings() ?: starter.loadSettings() ?: LiveControlSettings()) }
    var commandIntervalMillis by remember(settings) { mutableStateOf(settings.commandIntervalMillis) }
    var baseText by remember(settings) { mutableStateOf(settings.basePressureBar.toString()) }
    var ceilingText by remember(settings) { mutableStateOf(settings.maximumPressureBar.toString()) }
    var slewText by remember(settings) { mutableStateOf(settings.slewBarPerSecond.toString()) }
    var kpText by remember(settings) { mutableStateOf(settings.proportionalGainBarPerGramPerSecond.toString()) }
    var kiText by remember(settings) { mutableStateOf(settings.integralGainBarPerGram.toString()) }
    var deadlineText by remember(settings) { mutableStateOf((settings.shotDeadlineMillis / 1_000L).toString()) }
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { navigator.navigate(NavEvent.Back) }) { Text("Back") }
                Text("Experimental live brew test", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                if (mode == LiveTestMode.CommandEncodingProbe) {
                    "This probe starts a short physical shot and sends test commands. Keep the machine attended."
                } else {
                    "Unvalidated simulation defaults. Start with passive capture. BOOKOO sign-byte polarity is provisional; verify it from raw packets before live control."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Machine", style = MaterialTheme.typography.titleMedium)
                    Text("Connection: ${machine.connectionStatus}")
                    Text("Brew state: ${machine.brewStatus}")
                    Text("Pressure: ${machine.pressure?.let { "%.1f bar".format(it) } ?: "unavailable"}")
                    Text("Machine flow: ${machine.flowRate?.let { "%.1f mL/s".format(it) } ?: "unavailable"}")
                    Text("Telemetry age: ${machine.telemetryTime?.let { "${(kotlin.time.Clock.System.now() - it).inWholeMilliseconds.coerceAtLeast(0)} ms" } ?: "unavailable"}")
                }
            }

            if (mode != LiveTestMode.CommandEncodingProbe || probePath != LiveModeProbePath.EncodingMatrix) Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Electronic scale", style = MaterialTheme.typography.titleMedium)
                    Text(when {
                        scaleState.connecting -> "Connecting to BOOKOO…"
                        scaleState.connected -> "Connected"
                        else -> "Not connected"
                    })
                    scaleState.reading?.let { reading ->
                        Text("Weight: ${reading.frame.weightGrams} g")
                        Text("Scale flow: ${reading.frame.reportedFlowGramsPerSecond} g/s")
                        Text("Scale sample age: ${(kotlin.time.Clock.System.now() - reading.receivedAt).inWholeMilliseconds.coerceAtLeast(0)} ms")
                    }
                    scaleState.lastMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (scaleState.connected && scaleState.reading == null) {
                        Text(
                            "No valid weight frame received yet. Passive capture can still record raw packets; Shadow and Live require valid weight data.",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (!scaleState.connected && !scaleState.connecting) {
                        Text("Connect with 电子秤 in the dashboard toolbar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Capture mode", style = MaterialTheme.typography.titleMedium)
                    ModeOption("Passive capture", "Record sensors only", mode == LiveTestMode.PassiveCapture) {
                        mode = LiveTestMode.PassiveCapture
                    }
                    ModeOption("Shadow control", "Calculate proposed pressure targets without sending them", mode == LiveTestMode.ShadowControl) {
                        mode = LiveTestMode.ShadowControl
                    }
                    ModeOption("Live control", "Starts a pressure-only test with a foreground Stop notification", mode == LiveTestMode.LiveControl) {
                        mode = LiveTestMode.LiveControl
                    }
                    if (commandProbeSupported) {
                        ModeOption("Command encoding probe", "One short shot testing pressure/flow writes and mode changes", mode == LiveTestMode.CommandEncodingProbe) {
                            mode = LiveTestMode.CommandEncodingProbe
                        }
                    }
                    ModeOption("Machine disconnect", "Simulate link loss and record stop verification", mode == LiveTestMode.MachineDisconnectSimulation) {
                        mode = LiveTestMode.MachineDisconnectSimulation
                    }
                    if (mode == LiveTestMode.MachineDisconnectSimulation) {
                        Text("This simulation sends no machine commands.")
                        ModeOption("Native profile", "Record the firmware profile path", disconnectControlPath == "native_profile") {
                            disconnectControlPath = "native_profile"
                        }
                        ModeOption("App controlled", "Record the live app control path", disconnectControlPath == "app_controlled") {
                            disconnectControlPath = "app_controlled"
                        }
                    }
                    if (mode == LiveTestMode.CommandEncodingProbe) {
                        Text("Switching test", style = MaterialTheme.typography.titleSmall)
                        ModeOption(
                            "Live mode-register writes",
                            "Four 6 s stages; GeeFlow writes mode 1→0→1→0 and updates target 0x058B each stage.",
                            probePath == LiveModeProbePath.LiveModeRegister,
                        ) { probePath = LiveModeProbePath.LiveModeRegister }
                        ModeOption(
                            "Native per-stage priority",
                            "Uploads one temporary 4-stage profile; firmware changes controller using each stage’s priority bit.",
                            probePath == LiveModeProbePath.NativeStagePriority,
                        ) { probePath = LiveModeProbePath.NativeStagePriority }
                        ModeOption(
                            "Legacy encoding matrix",
                            "Tests all 16 target-word and mode-value combinations, holding each for 4 s (about 64 s plus switching).",
                            probePath == LiveModeProbePath.EncodingMatrix,
                        ) { probePath = LiveModeProbePath.EncodingMatrix }
                        if (probePath != LiveModeProbePath.EncodingMatrix) {
                            Text("Stage order", style = MaterialTheme.typography.titleSmall)
                            LiveModeProbeSequence.entries.forEach { sequence ->
                                ModeOption(
                                    sequence.label,
                                    if (sequence == LiveModeProbeSequence.FlowPressureFlowPressure) "Previously captured order."
                                    else "Complementary order for checking whether the first controller changes the result.",
                                    probeSequence == sequence,
                                ) { probeSequence = sequence }
                            }
                        }
                        Text(
                            when (probePath) {
                                LiveModeProbePath.EncodingMatrix -> "Runs all 16 encoding combinations for 4 s each, with per-combination pressure, flow, volume, and scale summaries. Total time is about 64 s plus command switching."
                                LiveModeProbePath.LiveModeRegister -> "Starts a 24 s physical test: ${probeSequence.label}, using 2 mL/s flow and 3 bar pressure for 6 s per stage. GeeFlow logs every mode write, target write, response, and sensor sample."
                                LiveModeProbePath.NativeStagePriority -> "Starts a matched 24 s physical test using one temporary native profile in ${probeSequence.label} order. The profile is not saved to your recipe library."
                            },
                            color = MaterialTheme.colorScheme.error,
                        )
                        if (probePath != LiveModeProbePath.EncodingMatrix) {
                            Text("Use a prepared puck and empty cup on a freshly tared BOOKOO. Keep the machine attended; Stop remains in the notification. Both four-stage tests stop at the scheduled 24 s; 32 s remains a hard safety limit.")
                            Text("Evidence saved: raw Modbus requests/replies, BOOKOO packets and readings, machine pressure/flow/volume samples, command timing, and per-stage sensor min/max/end and volume/weight change.")
                            if (probePath == LiveModeProbePath.NativeStagePriority) {
                                Text("Firmware stage index is not exposed in telemetry, so stage labels are assigned from elapsed time; sensor response is measured directly.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (!scaleProbeReady) Text("Connect BOOKOO and wait for a fresh sample before starting.", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (document == null) Text("Test recipe: 3 bar preinfusion, 1.5 g/s cup-flow target, 36 g final weight.")
                    else {
                        Text("${document.name}: ${document.phases.size} phases, ${document.settings.yieldGrams} g yield, ${document.settings.deadlineMillis / 1_000f} s deadline")
                        Text("Constant boiler temperature: ${document.settings.temperatureC?.toString() ?: "current"}°C")
                        if (source != null) {
                            OutlinedTextField(temperatureOverride, { temperatureOverride = it }, label = { Text("LITA temperature override (whole °C)") })
                            document.mappings.forEach { Text(it) }
                            Row {
                                Checkbox(mappingsAccepted, { mappingsAccepted = it })
                                Text("Accept LITA execution mappings")
                            }
                        }
                        compatibility.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
                        TextButton(onClick = { navigator.navigate(NavEvent.To(LiveRecipeEditorRoute(deviceId, document.id))) }) { Text("Edit recipe") }
                    }
                }
            }

            if (recipeId == null && mode != LiveTestMode.CommandEncodingProbe) Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Experimental controller settings", style = MaterialTheme.typography.titleMedium)
                    SettingField("Base pressure (bar)", baseText) { baseText = it }
                    SettingField("Pressure ceiling (bar)", ceilingText) { ceilingText = it }
                    SettingField("Pressure slew (bar/s)", slewText) { slewText = it }
                    SettingField("Kp (bar per g/s)", kpText) { kpText = it }
                    SettingField("Ki (bar per gram)", kiText) { kiText = it }
                    Text("Pressure write interval")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        LiveControlSettings.ALLOWED_COMMAND_INTERVALS_MILLIS.forEach { interval ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = commandIntervalMillis == interval,
                                    onClick = { commandIntervalMillis = interval },
                                )
                                Text("${interval} ms")
                            }
                        }
                    }
                    Text("100 ms is the default. 250 and 500 ms are available for attended latency comparisons.")
                    SettingField("Shot deadline (seconds)", deadlineText) { deadlineText = it }
                }
            }

            run.lastSample?.let { sample ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Live readings", style = MaterialTheme.typography.titleMedium)
                        Text("Phase: ${run.phaseId ?: "not evaluated"}")
                        Text("Measured pressure: ${sample.pressureBar?.let { "$it bar" } ?: "unavailable"}")
                        Text("Machine flow: ${sample.machineFlowMlPerSecond?.let { "$it mL/s" } ?: "unavailable"}")
                        Text("Weight: ${sample.weightGrams?.let { "$it g" } ?: "unavailable"}")
                        Text("Derived cup flow: ${sample.derivedCupFlowGramsPerSecond?.let { "$it g/s" } ?: "waiting for 3 scale samples"}")
                        Text("BOOKOO flow: ${sample.reportedCupFlowGramsPerSecond?.let { "$it g/s" } ?: "unavailable"}")
                        Text("Target pressure: ${sample.targetPressureBar?.let { "$it bar" } ?: "none"}")
                        Text("Machine sample age: ${sample.machineSampleAgeMillis?.let { "$it ms" } ?: "unavailable"}")
                        Text("Scale sample age: ${sample.scaleSampleAgeMillis?.let { "$it ms" } ?: "unavailable"}")
                        Text("Fallback: ${run.fallbackStatus ?: "none"}")
                    }
                }
            }

            run.message?.let { Text("Last run: $it") }
            if (disconnectStopUnconfirmed) {
                Text("Machine disconnected · stop unconfirmed", color = MaterialTheme.colorScheme.error)
                when (lastDisconnectObservation) {
                    null -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            coordinator.recordDisconnectTrialStep("autonomous_pump_stop_observed")
                        }) { Text("Record autonomous stop") }
                        Button(onClick = {
                            coordinator.recordDisconnectTrialStep("operator_intervention")
                        }) { Text("Record operator intervention") }
                    }
                    "operator_intervention" -> Button(onClick = {
                        coordinator.recordDisconnectTrialStep("physical_stop_observed")
                    }) { Text("Record pump or flow stopped") }
                    "autonomous_pump_stop_observed", "physical_stop_observed" -> {
                        if (run.mode == LiveTestMode.MachineDisconnectSimulation) {
                            Button(onClick = {
                                coordinator.recordDisconnectTrialStep("simulated_reconnect_and_verify")
                            }) { Text("Simulate reconnect and verify idle") }
                        } else {
                            Text("Waiting for reconnect. The app will resume stop verification only.")
                        }
                    }
                }
            }
            errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (disconnectStopUnconfirmed) {
                    Text(
                        if (run.mode == LiveTestMode.MachineDisconnectSimulation) {
                            "Complete the simulated observations above."
                        } else {
                            "Record what you observe. Stop verification resumes automatically after reconnect."
                        },
                    )
                } else if (run.active || run.stopPending) {
                    Button(onClick = { scope.launch { coordinator.stop(deviceId) } }) { Text("Stop") }
                } else {
                    Button(enabled = mode == LiveTestMode.MachineDisconnectSimulation ||
                        mode == LiveTestMode.CommandEncodingProbe && when (probePath) {
                            LiveModeProbePath.EncodingMatrix -> commandProbeSupported
                            LiveModeProbePath.LiveModeRegister -> commandProbeSupported && scaleProbeReady
                            LiveModeProbePath.NativeStagePriority -> nativePriorityProbeSupported && scaleProbeReady
                        } ||
                        (recipeId == null || document != null) &&
                        (mode == LiveTestMode.PassiveCapture || document == null || LiveRecipeCompiler.issues(document).isEmpty()) &&
                        (mode != LiveTestMode.LiveControl || compatibility.isEmpty()), onClick = {
                        errorText = null
                        val parsed = if (mode == LiveTestMode.CommandEncodingProbe) LiveControlSettings() else document?.controlSettings() ?: parseSettings(
                            baseText, ceilingText, slewText, kpText, kiText, commandIntervalMillis, deadlineText,
                        )
                        if (parsed == null) {
                            errorText = "Check the controller values and ranges."
                        } else {
                            if (mode != LiveTestMode.CommandEncodingProbe) {
                                settings = parsed
                                starter.saveSettings(parsed)
                            }
                            scope.launch {
                                runCatching {
                                    if (mode == LiveTestMode.MachineDisconnectSimulation) {
                                        coordinator.startDisconnectSimulation(deviceId, disconnectControlPath)
                                    } else if (mode == LiveTestMode.CommandEncodingProbe) {
                                        val startupError = starter.startCommandEncodingProbe(deviceId, probePath, probeSequence)
                                        startupError?.let { error(it) }
                                    } else if (mode == LiveTestMode.LiveControl) {
                                        val scaleId = scaleState.device?.id
                                            ?: error("Connect the scale before starting live control.")
                                        val startupError = if (document != null) starter.startRecipe(deviceId, scaleId, document) else starter.start(deviceId, scaleId, parsed)
                                        startupError?.let { error(it) }
                                    } else {
                                        coordinator.startMonitoring(
                                            deviceId,
                                            mode,
                                            document?.let(LiveTestRecipe::fromDocument) ?: LiveTestRecipe.simulationDefaults(parsed.basePressureBar),
                                            parsed,
                                        )
                                    }
                                }.onFailure { errorText = it.message ?: "Could not start the test." }
                            }
                        }
                    }) {
                        Text(when (mode) {
                            LiveTestMode.LiveControl -> "Start live test"
                            LiveTestMode.CommandEncodingProbe -> when (probePath) {
                                LiveModeProbePath.EncodingMatrix -> "Run encoding matrix"
                                LiveModeProbePath.LiveModeRegister -> "Run mode-register test"
                                LiveModeProbePath.NativeStagePriority -> "Run native-priority test"
                            }
                            LiveTestMode.MachineDisconnectSimulation -> "Run disconnect simulation"
                            else -> "Start capture"
                        })
                    }
                }
                if (!run.active && !run.stopPending && run.samples.isNotEmpty()) {
                    OutlinedButton(onClick = { coordinator.clearCompleted() }) { Text("Clear capture") }
                }
            }
            if (mode == LiveTestMode.LiveControl || mode == LiveTestMode.CommandEncodingProbe) {
                OutlinedButton(onClick = starter::requestNotificationPermission) { Text("Allow Stop notification") }
            }
            if (!run.active && !run.stopPending && run.captureEvents.isNotEmpty()) {
                OutlinedButton(onClick = {
                    val location = starter.exportCapture(run)
                    if (location == null) errorText = "Capture export is available on Android."
                    else coordinator.reportExportLocation(location)
                }) { Text("Export JSONL, CSV, and report") }
            }
        }
    }
}

@Composable
private fun ModeOption(title: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, onClick)
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SettingField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

private fun parseSettings(
    base: String,
    ceiling: String,
    slew: String,
    kp: String,
    ki: String,
    commandIntervalMillis: Long,
    deadlineSeconds: String,
): LiveControlSettings? = runCatching {
    LiveControlSettings(
        basePressureBar = base.toFloat(),
        maximumPressureBar = ceiling.toFloat(),
        slewBarPerSecond = slew.toFloat(),
        proportionalGainBarPerGramPerSecond = kp.toFloat(),
        integralGainBarPerGram = ki.toFloat(),
        commandIntervalMillis = commandIntervalMillis,
        shotDeadlineMillis = (deadlineSeconds.toFloat() * 1_000).toLong(),
    )
}.getOrNull()
