@file:Suppress("TooManyFunctions", "LongParameterList")

package app.geeflow.domain.device

import app.geeflow.domain.device.recipe.*
import app.geeflow.data.device.BookooScaleReading
import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.PressureLocation
import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.LiveBrewSession
import app.geeflow.data.device.LiveTargetDiagnosticWriter
import app.geeflow.data.device.MachineDiagnosticEventSource
import app.geeflow.data.device.TransportTimedLiveBrewSession
import app.geeflow.data.device.ScaleReadingSource
import app.geeflow.data.device.model.BrewTelemetry
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.withValue
import app.geeflow.domain.device.usecase.requireReady
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlin.time.TimeSource

enum class LiveTestMode { PassiveCapture, ShadowControl, LiveControl, CommandEncodingProbe, MachineDisconnectSimulation }

data class LiveTestSample(
    val elapsedMillis: Long,
    val machineTelemetryAt: Instant?,
    val scaleReceivedAt: Instant?,
    val machineSampleAgeMillis: Long?,
    val scaleSampleAgeMillis: Long?,
    val phaseId: String?,
    val pressureBar: Float?,
    val machineFlowMlPerSecond: Float?,
    val weightGrams: Float?,
    val derivedCupFlowGramsPerSecond: Float?,
    val reportedCupFlowGramsPerSecond: Float?,
    val targetPressureBar: Float?,
    val requestedControlValue: Float? = null,
    val requestedControlUnit: String? = null,
    val requestedPressureBar: Float? = null,
    val commandedPressureBar: Float? = null,
    val constraints: String? = null,
)

data class LiveTestCaptureEvent(
    val type: String,
    val recordedAt: Instant,
    val elapsedMillis: Long?,
    val fields: Map<String, String>,
)

data class LiveTestRunState(
    val runId: Long? = null,
    val deviceId: Long? = null,
    val mode: LiveTestMode? = null,
    val active: Boolean = false,
    val stopPending: Boolean = false,
    val phaseId: String? = null,
    val fallbackStatus: String? = null,
    val settings: LiveControlSettings? = null,
    val recipeSnapshot: String? = null,
    val message: String? = null,
    val lastSample: LiveTestSample? = null,
    val samples: List<LiveTestSample> = emptyList(),
    val captureEvents: List<LiveTestCaptureEvent> = emptyList(),
)

/** Owns experimental live pressure shots independently of the test screen lifecycle. */
@Single
class LiveBrewTestCoordinator(
    private val provider: DeviceControllerProvider,
    private val scale: ScaleReadingSource,
    private val execution: ProfileExecutionCoordinator,
    private val scope: CoroutineScope,
) {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(LiveTestRunState())
    private var runJob: Job? = null
    private var settledWeightJob: Job? = null
    private var nextRunId = 0L
    val state: StateFlow<LiveTestRunState> = mutableState.asStateFlow()

    suspend fun startLive(
        deviceId: Long,
        recipe: LiveTestRecipe,
        settings: LiveControlSettings,
        executionScope: CoroutineScope = scope,
    ) = mutex.withLock {
        check(runJob?.isActive != true && !state.value.stopPending) { "A live test is already active" }
        cancelSettledWeightJob()
        val controller = provider.getController(deviceId)
        controller.requireReady(deviceId)
        execution.beginExternalControl(deviceId)
        try {
            recipe.document?.let { LiveRecipeCompiler.compile(it, controller.profilingCapabilities) }
            val machine = controller.deviceState.value
            check(machine.connectionStatus == DeviceState.ConnectionStatus.Connected) { "Connect to the machine first" }
            check(machine.brewStatus == DeviceState.BrewStatus.Idle) { "Machine must be idle" }
            check(machine.pressure != null && machine.flowRate != null) { "Machine pressure and flow readings are required" }
            check(machine.telemetryTime?.let { (Clock.System.now() - it).inWholeMilliseconds <= TELEMETRY_STALE_MILLIS } == true) {
                "Machine telemetry is stale. Wait for fresh pressure and flow readings."
            }
            val reading = scale.state.value.reading
            check(scale.state.value.connected && reading != null) { "Connect the BOOKOO scale first" }
            check((Clock.System.now() - reading.receivedAt).inWholeMilliseconds <= SCALE_STALE_MILLIS) {
                "Scale data is stale. Wait for a fresh reading."
            }
            check(kotlin.math.abs(reading.frame.weightGrams) <= START_WEIGHT_TOLERANCE_GRAMS) {
                "Tare the scale and place an empty cup before starting."
            }
            check(controller.profilingCapabilities.livePressure.containsKey(PressureLocation.Pump)) {
                "Pump pressure control is unavailable on this model"
            }
            mutableState.value = LiveTestRunState(
                runId = ++nextRunId,
                deviceId = deviceId,
                mode = LiveTestMode.LiveControl,
                active = true,
                settings = settings,
                recipeSnapshot = recipe.document?.let(LiveRecipeJson::encode),
                captureEvents = initialCaptureEvents(deviceId, LiveTestMode.LiveControl, recipe, settings),
            )
            runJob = executionScope.launch { run(controller, recipe, settings, LiveTestMode.LiveControl) }
        } catch (error: Exception) {
            execution.endExternalControl(deviceId)
            throw error
        }
    }

    suspend fun startCommandEncodingProbe(
        deviceId: Long,
        path: LiveModeProbePath = LiveModeProbePath.EncodingMatrix,
        sequence: LiveModeProbeSequence = LiveModeProbeSequence.FlowPressureFlowPressure,
        executionScope: CoroutineScope = scope,
    ) = mutex.withLock {
        check(runJob?.isActive != true && !state.value.stopPending) { "A live test is already active" }
        val controller = provider.getController(deviceId)
        controller.requireReady(deviceId)
        if (path != LiveModeProbePath.NativeStagePriority) {
            check(controller.brewing is LiveTargetDiagnosticWriter) { "Command probe is available only on supported Wendougee machines" }
        }
        check(controller.profilingCapabilities.livePressure[PressureLocation.Pump] != null) { "Pump pressure control is unavailable on this model" }
        check(controller.profilingCapabilities.liveFlow != null) { "Live flow control is unavailable on this model" }
        if (path == LiveModeProbePath.LiveModeRegister) {
            check(controller.profilingCapabilities.liveModeSwitch) {
                "This model does not support live flow and pressure switching"
            }
        }
        if (path == LiveModeProbePath.NativeStagePriority) {
            check(controller.profiles.assessNativeProfile(LiveModeSwitchProbeScript.nativeProfile(sequence)).isEmpty()) {
                "This model does not support the native pressure/flow stage-priority profile"
            }
        }
        val machine = controller.deviceState.value
        check(machine.connectionStatus == DeviceState.ConnectionStatus.Connected) { "Connect to the machine first" }
        check(machine.brewStatus == DeviceState.BrewStatus.Idle) { "Machine must be idle" }
        check(machine.pressure != null && machine.flowRate != null) { "Machine pressure and flow readings are required" }
        check(machine.telemetryTime?.let { (Clock.System.now() - it).inWholeMilliseconds <= TELEMETRY_STALE_MILLIS } == true) {
            "Machine telemetry is stale. Wait for fresh pressure and flow readings."
        }
        if (path != LiveModeProbePath.EncodingMatrix) {
            val scaleState = scale.state.value
            val reading = scaleState.reading
            check(scaleState.connected && reading != null) { "Connect BOOKOO and wait for a valid reading before this comparison." }
            check((Clock.System.now() - reading.receivedAt).inWholeMilliseconds <= SCALE_STALE_MILLIS) {
                "BOOKOO reading is stale. Wait for a fresh reading."
            }
            check(kotlin.math.abs(reading.frame.weightGrams) <= START_WEIGHT_TOLERANCE_GRAMS) {
                "Tare BOOKOO with the cup in place before starting."
            }
        }
        execution.beginExternalControl(deviceId)
        val startedAt = Clock.System.now()
        val selectedStages = LiveModeSwitchProbeScript.stages(sequence)
        val selectedNativeProfile = LiveModeSwitchProbeScript.nativeProfile(sequence)
        val expectedStages = selectedStages.joinToString(";") { stage ->
            "${stage.index}:${stage.id}:${stage.control}:${stage.target}:${stage.targetUnit}:${stage.durationMillis}"
        }
        val captureType = when (path) {
            LiveModeProbePath.EncodingMatrix -> "command_probe_configuration"
            LiveModeProbePath.LiveModeRegister -> "mode_switch_probe_configuration"
            LiveModeProbePath.NativeStagePriority -> "native_priority_probe_configuration"
        }
        mutableState.value = LiveTestRunState(
            runId = ++nextRunId,
            deviceId = deviceId,
            mode = LiveTestMode.CommandEncodingProbe,
            active = true,
            message = "Starting ${path.label}",
            captureEvents = listOf(
                LiveTestCaptureEvent(
                    captureType,
                    startedAt,
                    0,
                    mapOf(
                        "schema_version" to "1",
                        "device_id" to deviceId.toString(),
                        "test_path" to path.id,
                        "sequence_id" to sequence.id,
                        "sequence" to sequence.label,
                        "target_register" to "0x058B",
                        "target_function" to "0x10",
                        "mode_register" to "0x05B3",
                        "pressure_mode_value" to "0",
                        "flow_mode_value" to "1",
                        "pressure_target_bar" to LiveModeSwitchProbeScript.PRESSURE_BAR.toString(),
                        "flow_target_ml_per_s" to LiveModeSwitchProbeScript.FLOW_ML_PER_SECOND.toString(),
                        "hold_ms_per_stage" to LiveModeSwitchProbeScript.STAGE_DURATION_MILLIS.toString(),
                        "expected_stages" to expectedStages,
                        "native_profile_name" to selectedNativeProfile.name,
                        "native_profile_stage_words" to "time,pressure_x10,flow_x10,wait,end,flow_priority",
                        "encoding_matrix_steps" to LiveCommandEncodingProbeScript.steps.joinToString(";") {
                            "${it.index}:${it.modeEncoding.id}:${it.variant.id}:${it.mode}:${it.modeEncoding.valueFor(it.mode)}:${it.words.joinToString(",")}"
                        },
                    ),
                ),
            ),
        )
        runJob = executionScope.launch {
            when (path) {
                LiveModeProbePath.EncodingMatrix -> runCommandEncodingProbe(controller)
                LiveModeProbePath.LiveModeRegister -> runLiveModeSwitchProbe(controller, sequence)
                LiveModeProbePath.NativeStagePriority -> runNativeStagePriorityProbe(controller, sequence)
            }
        }
    }

    suspend fun startMonitoring(
        deviceId: Long,
        mode: LiveTestMode,
        recipe: LiveTestRecipe,
        settings: LiveControlSettings,
    ) = mutex.withLock {
        require(mode != LiveTestMode.LiveControl) { "Live control must start from the foreground service" }
        check(runJob?.isActive != true && !state.value.stopPending) { "A live test is already active" }
        cancelSettledWeightJob()
        val controller = provider.getController(deviceId)
        controller.requireReady(deviceId)
        val machine = controller.deviceState.value
        check(machine.connectionStatus == DeviceState.ConnectionStatus.Connected) { "Connect to the machine first" }
        check(machine.pressure != null && machine.flowRate != null) { "Machine pressure and flow readings are required" }
        check(machine.telemetryTime?.let { (Clock.System.now() - it).inWholeMilliseconds <= TELEMETRY_STALE_MILLIS } == true) {
            "Machine telemetry is stale. Wait for fresh pressure and flow readings."
        }
        val scaleState = scale.state.value
        check(scaleState.connected) { "Connect the BOOKOO scale first" }
        if (mode != LiveTestMode.PassiveCapture) {
            val reading = scaleState.reading
            check(reading != null) { "No valid BOOKOO weight packet yet. Passive capture can record unsupported packets." }
            check((Clock.System.now() - reading.receivedAt).inWholeMilliseconds <= SCALE_STALE_MILLIS) {
                "Scale data is stale. Wait for a fresh reading."
            }
            check(kotlin.math.abs(reading.frame.weightGrams) <= START_WEIGHT_TOLERANCE_GRAMS) {
                "Tare the scale and place an empty cup before starting."
            }
        }
        mutableState.value = LiveTestRunState(
            runId = ++nextRunId,
            deviceId = deviceId,
            mode = mode,
            active = true,
            settings = settings,
            recipeSnapshot = recipe.document?.let(LiveRecipeJson::encode),
            captureEvents = initialCaptureEvents(deviceId, mode, recipe, settings),
        )
        runJob = scope.launch { run(controller, recipe, settings, mode) }
    }

    suspend fun startDisconnectSimulation(deviceId: Long, controlPath: String) = mutex.withLock {
        check(runJob?.isActive != true && !state.value.stopPending) { "A live test is already active" }
        cancelSettledWeightJob()
        require(controlPath in setOf("native_profile", "app_controlled"))
        val controller = provider.getController(deviceId)
        val machine = controller.deviceState.value
        check(machine.connectionStatus == DeviceState.ConnectionStatus.Connected) { "Connect to the machine first" }
        check(machine.brewStatus == DeviceState.BrewStatus.Idle) { "Machine must be idle for the disconnect simulation" }
        val startedAt = Clock.System.now()
        val recipe = LiveTestRecipe.simulationDefaults(LiveControlSettings().basePressureBar)
        mutableState.value = LiveTestRunState(
            runId = ++nextRunId,
            deviceId = deviceId,
            mode = LiveTestMode.MachineDisconnectSimulation,
            stopPending = true,
            settings = LiveControlSettings(),
            recipeSnapshot = null,
            message = "Machine disconnected · stop unconfirmed",
            captureEvents = listOf(
                LiveTestCaptureEvent(
                    "scenario_started", startedAt, 0,
                    mapOf("scenario" to "machine_disconnect", "simulated" to "true", "control_path" to controlPath),
                ),
                LiveTestCaptureEvent("machine_link_lost", startedAt, 0, mapOf("simulated" to "true")),
                LiveTestCaptureEvent("stop_unconfirmed", startedAt, 0, mapOf("simulated" to "true")),
            ),
        )
    }

    fun recordDisconnectTrialStep(step: String) {
        val current = state.value
        check(
            (current.mode == LiveTestMode.MachineDisconnectSimulation || current.mode == LiveTestMode.LiveControl) &&
                current.stopPending
        ) {
            "No machine disconnect trial is waiting for input."
        }
        val observations = setOf("autonomous_pump_stop_observed", "operator_intervention", "physical_stop_observed")
        val previous = current.captureEvents.lastOrNull { it.type in observations }?.type
        val simulated = current.mode == LiveTestMode.MachineDisconnectSimulation
        val nextEvents = when (step) {
            "autonomous_pump_stop_observed" -> {
                check(previous == null)
                listOf(
                    "autonomous_pump_stop_observed" to mapOf(
                        "source" to "operator_observation",
                        "before_intervention" to "true",
                        "before_reconnect_stop_command" to "true",
                    ),
                )
            }
            "operator_intervention" -> {
                check(previous == null)
                listOf("operator_intervention" to mapOf("source" to "test_operator"))
            }
            "physical_stop_observed" -> {
                check(previous == "operator_intervention")
                listOf("physical_stop_observed" to mapOf("source" to "operator_observation"))
            }
            "simulated_reconnect_and_verify" -> {
                check(simulated && (previous == "autonomous_pump_stop_observed" || previous == "physical_stop_observed"))
                listOf(
                    "machine_reconnected" to mapOf("source" to "simulated"),
                    "stop_request" to mapOf("source" to "simulated_after_reconnect"),
                    "fresh_idle_confirmation" to mapOf("source" to "simulated"),
                )
            }
            else -> error("Unknown machine disconnect simulation step.")
        }
        nextEvents.forEach { (type, fields) ->
            val at = Clock.System.now()
            captureEvent(type, null, fields + ("simulated" to simulated.toString()), at)
        }
        if (step == "simulated_reconnect_and_verify") {
            mutableState.update { it.copy(stopPending = false, message = "Disconnect simulation complete") }
        }
    }

    suspend fun stop(deviceId: Long): Boolean {
        val job = mutex.withLock {
            if (state.value.deviceId != deviceId || !state.value.active && !state.value.stopPending) return false
            runJob
        }
        job?.cancelAndJoin()
        return true
    }

    fun owns(deviceId: Long) =
        (state.value.active || state.value.stopPending) && state.value.deviceId == deviceId

    fun clearCompleted() {
        if (!state.value.active && !state.value.stopPending) {
            settledWeightJob?.cancel()
            settledWeightJob = null
            mutableState.value = LiveTestRunState()
        }
    }

    fun reportExportLocation(location: String) {
        mutableState.update { it.copy(message = "Capture exported to $location") }
    }

    private suspend fun runCommandEncodingProbe(controller: DeviceController) {
        val runId = requireNotNull(state.value.runId)
        val startedAt = Clock.System.now()
        val clock = TimeSource.Monotonic.markNow()
        val diagnosticSource = controller as? MachineDiagnosticEventSource
        var activeStep = "starting"
        var activeMode = FreeHandControlMode.Pressure
        var activeModeValue = 0
        var lastTelemetryTimestamp: Instant? = null
        val diagnosticJob = diagnosticSource?.let { source ->
            scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                source.diagnosticEvents.collect { event ->
                    captureEvent(
                        event.name,
                        clock.elapsedNow().inWholeMilliseconds,
                        event.fields + ("probe_step" to activeStep),
                        event.receivedAt,
                        runId,
                    )
                }
            }
        }
        val scalePacketJob = scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            scale.packets.collect { packet ->
                if (packet.receivedAt >= startedAt) {
                    captureEvent(
                        "scale_packet", clock.elapsedNow().inWholeMilliseconds,
                        mapOf(
                            "packet_hex" to packet.packetHex,
                            "decoded" to packet.decoded.toString(),
                            "weight_g" to packet.frame?.weightGrams?.toString().orEmpty(),
                            "probe_step" to activeStep,
                        ), packet.receivedAt, runId,
                    )
                }
            }
        }
        val scaleStatusJob = scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            var previous: Triple<Boolean, Boolean, Boolean>? = null
            scale.state.collect { status ->
                val key = Triple(status.connected, status.connecting, status.receiving)
                if (key != previous) {
                    previous = key
                    captureEvent(
                        "scale_status", clock.elapsedNow().inWholeMilliseconds,
                        mapOf(
                            "connected" to status.connected.toString(),
                            "connecting" to status.connecting.toString(),
                            "receiving" to status.receiving.toString(),
                            "device_id" to status.device?.id.orEmpty(),
                            "device_name" to status.device?.name.orEmpty(),
                            "message" to status.lastMessage.orEmpty(),
                            "probe_step" to activeStep,
                        ), runId = runId,
                    )
                }
            }
        }
        var reason = "complete"
        try {
            captureEvent("command_probe_started", 0, mapOf("initial_mode" to activeMode.name), startedAt, runId)
            controller.brewing.startFreeVariableBrewing(activeMode)
            withTimeout(TELEMETRY_STALE_MILLIS.milliseconds) {
                controller.deviceState.first { it.brewStatus == DeviceState.BrewStatus.FreeVariable }
            }
            controller.deviceState.value.let { baseline ->
                lastTelemetryTimestamp = baseline.telemetryTime
                captureEvent(
                    "command_probe_sensor_baseline",
                    clock.elapsedNow().inWholeMilliseconds,
                    mapOf(
                        "pressure_bar" to baseline.pressure?.toString().orEmpty(),
                        "machine_flow_ml_per_s" to baseline.flowRate?.toString().orEmpty(),
                        "brew_status" to baseline.brewStatus.name,
                        "telemetry_at" to baseline.telemetryTime.toString(),
                        "telemetry_age_ms" to baseline.telemetryTime?.let {
                            (Clock.System.now() - it).inWholeMilliseconds.coerceAtLeast(0).toString()
                        }.orEmpty(),
                    ),
                    baseline.telemetryTime ?: Clock.System.now(),
                    runId,
                )
            }
            val writer = controller.brewing as LiveTargetDiagnosticWriter
            for (step in LiveCommandEncodingProbeScript.steps) {
                activeStep = "${step.index}:${step.modeEncoding.id}:${step.variant.id}:${step.mode.name.lowercase()}"
                val modeValue = step.modeEncoding.valueFor(step.mode)
                if (step.mode != activeMode || modeValue != activeModeValue) {
                    captureEvent(
                        "probe_mode_switch_requested",
                        clock.elapsedNow().inWholeMilliseconds,
                        mapOf(
                            "step" to activeStep,
                            "from" to activeMode.name,
                            "from_register_value" to activeModeValue.toString(),
                            "to" to step.mode.name,
                            "to_register_value" to modeValue.toString(),
                            "mode_encoding" to step.modeEncoding.id,
                            "register" to "0x05B3",
                        ),
                        runId = runId,
                    )
                    writer.writeLiveModeValue(modeValue, step.mode)
                    activeMode = step.mode
                    activeModeValue = modeValue
                    captureEvent(
                        "probe_mode_switch_completed",
                        clock.elapsedNow().inWholeMilliseconds,
                        mapOf("step" to activeStep, "mode" to activeMode.name, "register_value" to activeModeValue.toString()),
                        runId = runId,
                    )
                }
                val fields = mapOf(
                    "step" to activeStep,
                    "variant" to step.variant.id,
                    "variant_label" to step.variant.label,
                    "mode_encoding" to step.modeEncoding.id,
                    "mode_encoding_label" to step.modeEncoding.label,
                    "mode" to step.mode.name,
                    "mode_register_value" to modeValue.toString(),
                    "logical_target" to step.targetValue.toString(),
                    "unit" to step.targetUnit,
                    "register" to "0x058B",
                    "function" to "0x10",
                    "word_0" to step.words[0].toString(),
                    "word_1" to step.words[1].toString(),
                    "hold_ms" to step.holdMillis.toString(),
                )
                captureEvent("probe_target_write_requested", clock.elapsedNow().inWholeMilliseconds, fields, runId = runId)
                writer.writeLiveTargetWords(step.words)
                captureEvent("probe_target_write_completed", clock.elapsedNow().inWholeMilliseconds, fields, runId = runId)
                val holdStarted = TimeSource.Monotonic.markNow()
                val observations = mutableListOf<ProbeSensorObservation>()
                while (holdStarted.elapsedNow().inWholeMilliseconds < step.holdMillis) {
                    val machine = controller.deviceState.value
                    val telemetryAt = machine.telemetryTime
                    if (telemetryAt != null && telemetryAt != lastTelemetryTimestamp) {
                        lastTelemetryTimestamp = telemetryAt
                        val elapsed = clock.elapsedNow().inWholeMilliseconds
                        captureEvent(
                            "command_probe_sensor",
                            elapsed,
                            mapOf(
                                "step" to activeStep,
                                "variant" to step.variant.id,
                                "mode_encoding" to step.modeEncoding.id,
                                "mode" to step.mode.name,
                                "mode_register_value" to modeValue.toString(),
                                "command_value" to step.targetValue.toString(),
                                "command_unit" to step.targetUnit,
                                "pressure_bar" to machine.pressure?.toString().orEmpty(),
                                "machine_flow_ml_per_s" to machine.flowRate?.toString().orEmpty(),
                                "brew_status" to machine.brewStatus.name,
                                "telemetry_at" to telemetryAt.toString(),
                                "telemetry_age_ms" to (Clock.System.now() - telemetryAt).inWholeMilliseconds.coerceAtLeast(0).toString(),
                            ),
                            telemetryAt,
                            runId,
                        )
                        val scaleReading = scale.state.value.reading?.takeIf { scale.state.value.connected }
                        observations += ProbeSensorObservation(
                            elapsed,
                            machine.pressure,
                            machine.flowRate,
                            machine.volume,
                            scaleReading?.frame?.weightGrams,
                            null,
                        )
                        recordSample(
                            elapsed,
                            machine,
                            scaleReading?.frame?.weightGrams,
                            null,
                            scaleReading?.frame?.reportedFlowGramsPerSecond,
                            step.targetValue.takeIf { step.mode == FreeHandControlMode.Pressure },
                            activeStep,
                            scaleReading?.receivedAt,
                            scaleReading?.monotonicMark?.elapsedNow()?.inWholeMilliseconds,
                            step.targetValue,
                            step.targetUnit,
                            step.targetValue.takeIf { step.mode == FreeHandControlMode.Pressure },
                            step.targetValue.takeIf { step.mode == FreeHandControlMode.Pressure },
                        )
                    }
                    delay(50.milliseconds)
                }
                captureProbeStageSummary(
                    runId,
                    clock.elapsedNow().inWholeMilliseconds,
                    LiveModeSwitchProbeStage(
                        step.index,
                        activeStep,
                        if (step.mode == FreeHandControlMode.Pressure) LiveControl.Pressure else LiveControl.Flow,
                        step.targetValue,
                        step.holdMillis,
                    ),
                    "encoding_matrix",
                    observations,
                )
                captureEvent("probe_step_completed", clock.elapsedNow().inWholeMilliseconds, fields, runId = runId)
            }
        } catch (error: CancellationException) {
            reason = "operator_stop"
        } catch (error: Exception) {
            reason = error.message ?: "command_probe_failed"
            captureEvent(
                "command_probe_error",
                clock.elapsedNow().inWholeMilliseconds,
                mapOf("error" to reason),
                runId = runId,
            )
            Logger.e(error) { "Live command encoding probe stopped" }
        } finally {
            activeStep = "stop"
            val stopped = withContext(NonCancellable) {
                confirmStop(controller, null, startedAt) {
                    captureEvent("stop_request", clock.elapsedNow().inWholeMilliseconds, mapOf("reason" to reason), runId = runId)
                }
            }
            withContext(NonCancellable) {
                diagnosticJob?.cancelAndJoin()
                scalePacketJob.cancelAndJoin()
                scaleStatusJob.cancelAndJoin()
            }
            captureEvent(
                "stop_confirmation",
                clock.elapsedNow().inWholeMilliseconds,
                mapOf("confirmed" to stopped.toString(), "reason" to reason),
                runId = runId,
            )
            captureEvent("command_probe_ended", clock.elapsedNow().inWholeMilliseconds, mapOf("stop_confirmed" to stopped.toString()), runId = runId)
            mutableState.update { it.copy(active = false, stopPending = !stopped, message = if (stopped) "Command probe $reason" else "Stop unconfirmed") }
            if (stopped) {
                state.value.deviceId?.let { execution.endExternalControl(it) }
            } else {
                retryCommandProbeStop(controller, startedAt, runId)
            }
        }
    }

    private suspend fun runLiveModeSwitchProbe(controller: DeviceController, sequence: LiveModeProbeSequence) {
        val runId = requireNotNull(state.value.runId)
        val startedAt = Clock.System.now()
        val clock = TimeSource.Monotonic.markNow()
        val diagnosticSource = controller as? MachineDiagnosticEventSource
        var activeStage = "starting"
        var lastTelemetryAt: Instant? = null
        val diagnosticJob = diagnosticSource?.let { source ->
            scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                source.diagnosticEvents.collect { event ->
                    captureEvent(event.name, clock.elapsedNow().inWholeMilliseconds,
                        event.fields + ("probe_stage" to activeStage), event.receivedAt, runId)
                }
            }
        }
        val scalePacketJob = scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            scale.packets.collect { packet ->
                if (packet.receivedAt >= startedAt) {
                    captureEvent("scale_packet", clock.elapsedNow().inWholeMilliseconds,
                        mapOf(
                            "packet_hex" to packet.packetHex,
                            "decoded" to packet.decoded.toString(),
                            "weight_g" to packet.frame?.weightGrams?.toString().orEmpty(),
                            "reported_flow_g_per_s" to packet.frame?.reportedFlowGramsPerSecond?.toString().orEmpty(),
                            "probe_stage" to activeStage,
                        ), packet.receivedAt, runId)
                }
            }
        }
        val scaleStatusJob = launchProbeScaleStatus(clock, runId) { activeStage }
        var reason = "complete"
        val probeStages = LiveModeSwitchProbeScript.stages(sequence)
        var activeMode = if (probeStages.first().control == LiveControl.Pressure) FreeHandControlMode.Pressure else FreeHandControlMode.Flow
        var previousScaleMark: Long? = null
        val flowEstimator = CupFlowEstimator(windowMillis = 1_000L, minimumWindowMillis = 500L)
        val stageObservations = mutableListOf<ProbeSensorObservation>()
        try {
            captureEvent("live_mode_probe_started", 0,
                mapOf("initial_mode" to activeMode.name, "sequence_id" to sequence.id, "sequence" to sequence.label,
                    "target_encoding" to "canonical_x10", "stage_count" to probeStages.size.toString()), startedAt, runId)
            controller.brewing.startFreeVariableBrewing(activeMode)
            withTimeout(TELEMETRY_STALE_MILLIS.milliseconds) {
                controller.deviceState.first { it.brewStatus == DeviceState.BrewStatus.FreeVariable }
            }
            controller.deviceState.value.let { baseline ->
                lastTelemetryAt = baseline.telemetryTime
                captureEvent("probe_sensor_baseline", clock.elapsedNow().inWholeMilliseconds,
                    sensorFields(baseline, scale.state.value.reading?.frame?.weightGrams, "baseline"), baseline.telemetryTime ?: startedAt, runId)
            }

            for (stage in probeStages) {
                activeStage = "${stage.index}:${stage.id}"
                val nextMode = when (stage.control) {
                    LiveControl.Pressure -> FreeHandControlMode.Pressure
                    LiveControl.Flow -> FreeHandControlMode.Flow
                }
                stageObservations.clear()
                captureEvent("probe_stage_started", clock.elapsedNow().inWholeMilliseconds,
                    stageFields(stage, "live_mode_register", if (nextMode == FreeHandControlMode.Pressure) 0 else 1), runId = runId)
                if (nextMode != activeMode) {
                    val modeValue = if (nextMode == FreeHandControlMode.Pressure) 0 else 1
                    captureEvent("probe_mode_switch_requested", clock.elapsedNow().inWholeMilliseconds,
                        mapOf("stage" to activeStage, "from" to activeMode.name, "to" to nextMode.name,
                            "register" to "0x05B3", "register_value" to modeValue.toString(),
                            "switch_method" to "clear_targets_then_fc06_mode_write"), runId = runId)
                    controller.brewing.setFreeVariableBrewingMode(nextMode)
                    activeMode = nextMode
                    captureEvent("probe_mode_switch_completed", clock.elapsedNow().inWholeMilliseconds,
                        mapOf("stage" to activeStage, "mode" to activeMode.name, "register_value" to modeValue.toString()), runId = runId)
                }
                val words = when (stage.control) {
                    LiveControl.Pressure -> listOf((stage.target * 10).toInt(), 0)
                    LiveControl.Flow -> listOf(0, (stage.target * 10).toInt())
                }
                val requestFields = stageFields(stage, "live_mode_register", if (nextMode == FreeHandControlMode.Pressure) 0 else 1) + mapOf(
                    "target_register" to "0x058B", "function" to "0x10", "word_0" to words[0].toString(), "word_1" to words[1].toString(),
                )
                captureEvent("probe_target_write_requested", clock.elapsedNow().inWholeMilliseconds, requestFields, runId = runId)
                when (stage.control) {
                    LiveControl.Pressure -> controller.brewing.setFreeBrewPressureTarget(stage.target)
                    LiveControl.Flow -> controller.brewing.setFreeBrewFlowTarget(stage.target)
                }
                captureEvent("probe_target_write_completed", clock.elapsedNow().inWholeMilliseconds, requestFields, runId = runId)
                val stageClock = TimeSource.Monotonic.markNow()
                while (stageClock.elapsedNow().inWholeMilliseconds < stage.durationMillis) {
                    val machine = controller.deviceState.value
                    val telemetryAt = machine.telemetryTime
                    if (telemetryAt != null && telemetryAt != lastTelemetryAt) {
                        lastTelemetryAt = telemetryAt
                        val elapsed = clock.elapsedNow().inWholeMilliseconds
                        val reading = scale.state.value.reading?.takeIf { scale.state.value.connected }
                        val scaleAge = reading?.monotonicMark?.elapsedNow()?.inWholeMilliseconds
                        val cupFlow = reading?.takeIf { it.monotonicMark.elapsedNow().inWholeMilliseconds < SCALE_STALE_MILLIS }
                            ?.let { current ->
                                val at = elapsed - current.monotonicMark.elapsedNow().inWholeMilliseconds
                                if (previousScaleMark != current.monotonicMark.elapsedNow().inWholeMilliseconds) {
                                    previousScaleMark = current.monotonicMark.elapsedNow().inWholeMilliseconds
                                    flowEstimator.add(at, current.frame.weightGrams)
                                } else null
                            }
                        val obs = ProbeSensorObservation(elapsed, machine.pressure, machine.flowRate, machine.volume,
                            reading?.frame?.weightGrams, cupFlow)
                        stageObservations += obs
                        captureEvent("probe_sensor_sample", elapsed,
                            sensorFields(machine, reading?.frame?.weightGrams, activeStage) + mapOf(
                                "target_control" to stage.control.name,
                                "target_value" to stage.target.toString(),
                                "target_unit" to stage.targetUnit,
                                "derived_cup_flow_g_per_s" to cupFlow?.toString().orEmpty(),
                                "scale_age_ms" to scaleAge?.toString().orEmpty(),
                            ), telemetryAt, runId)
                        recordSample(elapsed, machine, reading?.frame?.weightGrams, cupFlow,
                            reading?.frame?.reportedFlowGramsPerSecond,
                            stage.target.takeIf { stage.control == LiveControl.Pressure }, activeStage,
                            reading?.receivedAt, scaleAge, stage.target, stage.targetUnit,
                            stage.target.takeIf { stage.control == LiveControl.Pressure },
                            stage.target.takeIf { stage.control == LiveControl.Pressure })
                    }
                    delay(50.milliseconds)
                }
                captureProbeStageSummary(runId, clock.elapsedNow().inWholeMilliseconds, stage, "live_mode_register", stageObservations)
            }
        } catch (error: CancellationException) {
            reason = "operator_stop"
        } catch (error: Exception) {
            reason = error.message ?: "live_mode_probe_failed"
            captureEvent("mode_switch_probe_error", clock.elapsedNow().inWholeMilliseconds, mapOf("path" to "live_mode_register", "error" to reason), runId = runId)
            Logger.e(error) { "Live mode-register probe stopped" }
        } finally {
            activeStage = "stop"
            val stopped = withContext(NonCancellable) {
                confirmStop(controller, null, startedAt) {
                    captureEvent("stop_request", clock.elapsedNow().inWholeMilliseconds, mapOf("reason" to reason, "path" to "live_mode_register"), runId = runId)
                }
            }
            withContext(NonCancellable) { diagnosticJob?.cancelAndJoin(); scalePacketJob.cancelAndJoin(); scaleStatusJob.cancelAndJoin() }
            captureEvent("stop_confirmation", clock.elapsedNow().inWholeMilliseconds, mapOf("confirmed" to stopped.toString(), "reason" to reason), runId = runId)
            captureEvent("mode_switch_probe_ended", clock.elapsedNow().inWholeMilliseconds, mapOf("path" to "live_mode_register", "stop_confirmed" to stopped.toString()), runId = runId)
            mutableState.update { it.copy(active = false, stopPending = !stopped, message = if (stopped) "Live mode-register test $reason" else "Stop unconfirmed") }
            if (stopped) state.value.deviceId?.let { execution.endExternalControl(it) }
            else retryCommandProbeStop(controller, startedAt, runId)
        }
    }

    private suspend fun runNativeStagePriorityProbe(controller: DeviceController, sequence: LiveModeProbeSequence) {
        val runId = requireNotNull(state.value.runId)
        val startedAt = Clock.System.now()
        val clock = TimeSource.Monotonic.markNow()
        val diagnosticSource = controller as? MachineDiagnosticEventSource
        var activeStage = "profile_upload"
        var nativeBrewClock = clock
        var lastTelemetryAt: Instant? = null
        val diagnosticJob = diagnosticSource?.let { source ->
            scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                source.diagnosticEvents.collect { event ->
                    captureEvent(event.name, clock.elapsedNow().inWholeMilliseconds,
                        event.fields + ("expected_stage" to activeStage), event.receivedAt, runId)
                }
            }
        }
        val scalePacketJob = scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            scale.packets.collect { packet ->
                if (packet.receivedAt >= startedAt) captureEvent("scale_packet", clock.elapsedNow().inWholeMilliseconds,
                    mapOf("packet_hex" to packet.packetHex, "decoded" to packet.decoded.toString(),
                        "weight_g" to packet.frame?.weightGrams?.toString().orEmpty(),
                        "reported_flow_g_per_s" to packet.frame?.reportedFlowGramsPerSecond?.toString().orEmpty(),
                        "expected_stage" to activeStage), packet.receivedAt, runId)
            }
        }
        val scaleStatusJob = launchProbeScaleStatus(clock, runId) { activeStage }
        var reason = "complete"
        var profileStarted = false
        var previousScaleMark: Long? = null
        var observedStageIndex = -1
        var stageObservations = mutableListOf<ProbeSensorObservation>()
        val flowEstimator = CupFlowEstimator(windowMillis = 1_000L, minimumWindowMillis = 500L)
        val probeStages = LiveModeSwitchProbeScript.stages(sequence)
        val nativeProfile = LiveModeSwitchProbeScript.nativeProfile(sequence)
        try {
            captureEvent("native_profile_upload_requested", 0,
                mapOf("profile_name" to nativeProfile.name, "sequence_id" to sequence.id, "sequence" to sequence.label,
                    "profile_mode" to "2", "priority_bit_0" to "pressure", "priority_bit_1" to "flow",
                    "stages" to probeStages.joinToString(";") {
                        "${it.index}:${it.control}:${it.target}:${it.targetUnit}:${it.durationMillis}:priority_${if (it.control == LiveControl.Flow) 1 else 0}"
                    }), startedAt, runId)
            controller.profiles.startProfileBrewing(nativeProfile)
            profileStarted = true
            captureEvent("native_profile_upload_completed_and_start_requested", clock.elapsedNow().inWholeMilliseconds,
                mapOf("profile_name" to nativeProfile.name, "sequence_id" to sequence.id,
                    "expected_duration_ms" to LiveModeSwitchProbeScript.TOTAL_DURATION_MILLIS.toString()), runId = runId)
            withTimeout(TELEMETRY_STALE_MILLIS.milliseconds) {
                controller.deviceState.first { it.brewStatus == DeviceState.BrewStatus.Profile }
            }
            nativeBrewClock = TimeSource.Monotonic.markNow()
            captureEvent("native_profile_started", clock.elapsedNow().inWholeMilliseconds,
                mapOf("brew_status" to controller.deviceState.value.brewStatus.name, "control_path" to "firmware_native_priority"), runId = runId)
            lastTelemetryAt = controller.deviceState.value.telemetryTime
            val initialScale = scale.state.value.reading
            if (initialScale != null) flowEstimator.add(0L, initialScale.frame.weightGrams)
            while (currentCoroutineContext().isActive) {
                val machine = controller.deviceState.value
                val elapsed = clock.elapsedNow().inWholeMilliseconds
                if (machine.connectionStatus != DeviceState.ConnectionStatus.Connected) error("Machine disconnected during native profile")
                if (machine.brewStatus == DeviceState.BrewStatus.Idle && machine.statusTime?.let { it >= startedAt } == true) {
                    reason = "native_profile_finished"
                    break
                }
                val brewElapsed = nativeBrewClock.elapsedNow().inWholeMilliseconds
                if (brewElapsed >= LiveModeSwitchProbeScript.TOTAL_DURATION_MILLIS) {
                    reason = "native_profile_schedule_complete"
                    break
                }
                if (brewElapsed >= LiveModeSwitchProbeScript.MAX_RUN_MILLIS) {
                    reason = "native_profile_probe_timeout"
                    break
                }
                val index = (brewElapsed / LiveModeSwitchProbeScript.STAGE_DURATION_MILLIS).toInt()
                    .coerceIn(0, probeStages.lastIndex)
                if (index != observedStageIndex) {
                    if (observedStageIndex >= 0) {
                        captureProbeStageSummary(runId, elapsed, probeStages[observedStageIndex], "native_stage_priority", stageObservations)
                    }
                    observedStageIndex = index
                    stageObservations = mutableListOf()
                    val stage = probeStages[index]
                    activeStage = "${stage.index}:${stage.id}"
                    captureEvent("native_stage_window_expected", elapsed,
                        stageFields(stage, "native_stage_priority", if (stage.control == LiveControl.Pressure) 0 else 1) + mapOf(
                            "priority_bit" to if (stage.control == LiveControl.Flow) "1" else "0",
                            "actual_stage_index_readback" to "unavailable",
                        ), runId = runId)
                }
                val telemetryAt = machine.telemetryTime
                if (telemetryAt != null && telemetryAt != lastTelemetryAt) {
                    lastTelemetryAt = telemetryAt
                    val stage = probeStages[index]
                    val reading = scale.state.value.reading?.takeIf { scale.state.value.connected }
                    val scaleAge = reading?.monotonicMark?.elapsedNow()?.inWholeMilliseconds
                    val cupFlow = reading?.takeIf { scaleAge != null && scaleAge < SCALE_STALE_MILLIS }?.let { current ->
                        val at = elapsed - (scaleAge ?: 0)
                        if (previousScaleMark != current.monotonicMark.elapsedNow().inWholeMilliseconds) {
                            previousScaleMark = current.monotonicMark.elapsedNow().inWholeMilliseconds
                            flowEstimator.add(at, current.frame.weightGrams)
                        } else null
                    }
                    stageObservations += ProbeSensorObservation(elapsed, machine.pressure, machine.flowRate, machine.volume,
                        reading?.frame?.weightGrams, cupFlow)
                    captureEvent("probe_sensor_sample", elapsed,
                        sensorFields(machine, reading?.frame?.weightGrams, activeStage) + mapOf(
                            "target_control" to stage.control.name, "target_value" to stage.target.toString(),
                            "target_unit" to stage.targetUnit,
                            "priority_bit" to if (stage.control == LiveControl.Flow) "1" else "0",
                            "derived_cup_flow_g_per_s" to cupFlow?.toString().orEmpty(),
                            "scale_age_ms" to scaleAge?.toString().orEmpty(),
                        ), telemetryAt, runId)
                    recordSample(elapsed, machine, reading?.frame?.weightGrams, cupFlow,
                        reading?.frame?.reportedFlowGramsPerSecond,
                        stage.target.takeIf { stage.control == LiveControl.Pressure }, activeStage,
                        reading?.receivedAt, scaleAge, stage.target, stage.targetUnit,
                        stage.target.takeIf { stage.control == LiveControl.Pressure },
                        stage.target.takeIf { stage.control == LiveControl.Pressure })
                }
                delay(50.milliseconds)
            }
            if (observedStageIndex >= 0) {
                captureProbeStageSummary(runId, clock.elapsedNow().inWholeMilliseconds,
                    probeStages[observedStageIndex], "native_stage_priority", stageObservations)
            }
        } catch (error: CancellationException) {
            reason = "operator_stop"
        } catch (error: Exception) {
            reason = error.message ?: "native_priority_probe_failed"
            captureEvent("mode_switch_probe_error", clock.elapsedNow().inWholeMilliseconds,
                mapOf("path" to "native_stage_priority", "error" to reason), runId = runId)
            Logger.e(error) { "Native stage-priority probe stopped" }
        } finally {
            activeStage = "stop"
            val stopped = withContext(NonCancellable) {
                confirmNativeProfileStop(controller, startedAt, profileStarted) {
                    captureEvent("stop_request", clock.elapsedNow().inWholeMilliseconds,
                        mapOf("reason" to reason, "path" to "native_stage_priority"), runId = runId)
                }
            }
            withContext(NonCancellable) { diagnosticJob?.cancelAndJoin(); scalePacketJob.cancelAndJoin(); scaleStatusJob.cancelAndJoin() }
            captureEvent("stop_confirmation", clock.elapsedNow().inWholeMilliseconds,
                mapOf("confirmed" to stopped.toString(), "reason" to reason), runId = runId)
            captureEvent("mode_switch_probe_ended", clock.elapsedNow().inWholeMilliseconds,
                mapOf("path" to "native_stage_priority", "stop_confirmed" to stopped.toString()), runId = runId)
            mutableState.update { it.copy(active = false, stopPending = !stopped, message = if (stopped) "Native priority test $reason" else "Stop unconfirmed") }
            if (stopped) state.value.deviceId?.let { execution.endExternalControl(it) }
            else retryNativePriorityStop(controller, startedAt, runId)
        }
    }

    private fun launchProbeScaleStatus(
        clock: TimeSource.Monotonic.ValueTimeMark,
        runId: Long,
        stage: () -> String,
    ): Job = scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
        var previous: Triple<Boolean, Boolean, Boolean>? = null
        scale.state.collect { status ->
            val key = Triple(status.connected, status.connecting, status.receiving)
            if (key != previous) {
                previous = key
                captureEvent("scale_status", clock.elapsedNow().inWholeMilliseconds,
                    mapOf("connected" to status.connected.toString(), "connecting" to status.connecting.toString(),
                        "receiving" to status.receiving.toString(), "device_id" to status.device?.id.orEmpty(),
                        "device_name" to status.device?.name.orEmpty(), "message" to status.lastMessage.orEmpty(),
                        "probe_stage" to stage()), runId = runId)
            }
        }
    }

    private fun stageFields(stage: LiveModeSwitchProbeStage, path: String, modeValue: Int) = mapOf(
        "path" to path, "stage_index" to stage.index.toString(), "stage_id" to stage.id,
        "control" to stage.control.name, "target" to stage.target.toString(), "unit" to stage.targetUnit,
        "hold_ms" to stage.durationMillis.toString(), "mode_register_value" to modeValue.toString(),
    )

    private fun sensorFields(machine: DeviceState, weight: Float?, stage: String) = mapOf(
        "probe_stage" to stage, "machine_telemetry_at" to machine.telemetryTime.toString(),
        "machine_sample_age_ms" to machine.telemetryTime?.let { (Clock.System.now() - it).inWholeMilliseconds.coerceAtLeast(0).toString() }.orEmpty(),
        "pressure_bar" to machine.pressure?.toString().orEmpty(),
        "machine_flow_ml_per_s" to machine.flowRate?.toString().orEmpty(),
        "machine_volume_ml" to machine.volume?.toString().orEmpty(),
        "machine_time_s" to machine.time?.toString().orEmpty(),
        "weight_g" to weight?.toString().orEmpty(),
        "brew_status" to machine.brewStatus.name,
    )

    private fun captureProbeStageSummary(
        runId: Long,
        elapsed: Long,
        stage: LiveModeSwitchProbeStage,
        path: String,
        observations: List<ProbeSensorObservation>,
    ) {
        val pressure = observations.mapNotNull { it.pressure }
        val flow = observations.mapNotNull { it.flow }
        val volume = observations.mapNotNull { it.volume }
        val weights = observations.mapNotNull { it.weight }
        val first = observations.firstOrNull()
        val last = observations.lastOrNull()
        captureEvent("probe_stage_summary", elapsed,
            mapOf("path" to path, "stage_index" to stage.index.toString(), "stage_id" to stage.id,
                "control" to stage.control.name, "target" to stage.target.toString(), "unit" to stage.targetUnit,
                "sensor_samples" to observations.size.toString(),
                "pressure_min_bar" to pressure.minOrNull()?.toString().orEmpty(),
                "pressure_max_bar" to pressure.maxOrNull()?.toString().orEmpty(),
                "pressure_end_bar" to last?.pressure?.toString().orEmpty(),
                "machine_flow_min_ml_s" to flow.minOrNull()?.toString().orEmpty(),
                "machine_flow_max_ml_s" to flow.maxOrNull()?.toString().orEmpty(),
                "machine_flow_end_ml_s" to last?.flow?.toString().orEmpty(),
                "machine_volume_delta_ml" to ((last?.volume ?: 0f) - (first?.volume ?: 0f)).toString(),
                "scale_weight_start_g" to first?.weight?.toString().orEmpty(),
                "scale_weight_end_g" to last?.weight?.toString().orEmpty(),
                "scale_weight_delta_g" to ((last?.weight ?: 0f) - (first?.weight ?: 0f)).toString(),
                "derived_cup_flow_max_g_s" to observations.mapNotNull { it.cupFlow }.maxOrNull()?.toString().orEmpty()),
            runId = runId)
    }

    private suspend fun confirmNativeProfileStop(
        controller: DeviceController,
        startedAt: Instant,
        profileStarted: Boolean,
        onStopRequest: () -> Unit = {},
    ): Boolean = try {
        if (!profileStarted && controller.deviceState.value.brewStatus == DeviceState.BrewStatus.Idle) return true
        withTimeout(STOP_TIMEOUT_MILLIS.milliseconds) {
            val machine = controller.deviceState.value
            if (machine.brewStatus != DeviceState.BrewStatus.Idle) {
                check(machine.connectionStatus == DeviceState.ConnectionStatus.Connected)
                onStopRequest()
                controller.profiles.stopProfileBrewing()
            }
            controller.deviceState.first {
                it.connectionStatus == DeviceState.ConnectionStatus.Connected &&
                    it.brewStatus == DeviceState.BrewStatus.Idle &&
                    (!profileStarted || it.statusTime?.let { stamp -> stamp >= startedAt } == true)
            }
        }
        true
    } catch (_: Exception) {
        false
    }

    private data class ProbeSensorObservation(
        val elapsedMillis: Long,
        val pressure: Float?,
        val flow: Float?,
        val volume: Float?,
        val weight: Float?,
        val cupFlow: Float?,
    )

    private fun retryCommandProbeStop(controller: DeviceController, startedAt: Instant, runId: Long) {
        scope.launch {
            while (currentCoroutineContext().isActive && state.value.stopPending) {
                delay(STOP_RETRY_MILLIS.milliseconds)
                val stopped = confirmStop(controller, null, startedAt) {
                    captureEvent("stop_verification_retry", null, mapOf("mode" to "command_probe"), runId = runId)
                }
                if (stopped) {
                    captureEvent("fresh_idle_confirmation", null, mapOf("source" to "machine_status"), runId = runId)
                    mutableState.update { it.copy(stopPending = false, message = "Command probe stopped") }
                    state.value.deviceId?.let { execution.endExternalControl(it) }
                    break
                }
            }
        }
    }

    private fun retryNativePriorityStop(controller: DeviceController, startedAt: Instant, runId: Long) {
        scope.launch {
            while (currentCoroutineContext().isActive && state.value.stopPending) {
                delay(STOP_RETRY_MILLIS.milliseconds)
                val stopped = confirmNativeProfileStop(controller, startedAt, profileStarted = true) {
                    captureEvent("stop_verification_retry", null, mapOf("path" to "native_stage_priority"), runId = runId)
                }
                if (stopped) {
                    captureEvent("fresh_idle_confirmation", null, mapOf("source" to "machine_status", "path" to "native_stage_priority"), runId = runId)
                    mutableState.update { it.copy(stopPending = false, message = "Native profile test stopped") }
                    state.value.deviceId?.let { execution.endExternalControl(it) }
                    break
                }
            }
        }
    }

    private suspend fun run(
        controller: DeviceController,
        recipe: LiveTestRecipe,
        settings: LiveControlSettings,
        mode: LiveTestMode,
    ) {
        val runId = requireNotNull(state.value.runId)
        var session: LiveBrewSession? = null
        var scaleCaptureJob: Job? = null
        var machineCaptureJob: Job? = null
        var scaleSamplesJob: Job? = null
        var machineSamplesJob: Job? = null
        var reason = "user_stop"
        var machineLinkLossCaptured = false
        var lastPressureBeforeCommand: Float? = null
        var lastPressureCommandSentAt: Instant? = null
        var pressureResponseCaptured = false
        val startedAt = Clock.System.now()
        val clock = TimeSource.Monotonic.markNow()
        val genericEngine = recipe.document?.let { LiveRecipeEngine(LiveRecipeCompiler.compile(it)) }
        val commandGate = LiveTargetCommandGate(settings.commandIntervalMillis)
        try {
            // Capture collectors outlive the control loop when a link drops, so the scale can
            // keep recording while stop verification waits for reconnection.
            val captureScope = scope
            scaleCaptureJob = captureScope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                scale.packets.collect { packet ->
                    if (packet.receivedAt < startedAt) return@collect
                    captureEvent(
                        "scale_packet",
                        null,
                        mapOf(
                            "received_at" to packet.receivedAt.toString(),
                            "packet_hex" to packet.packetHex,
                            "decoded" to packet.decoded.toString(),
                        ),
                        packet.receivedAt,
                        runId,
                    )
                }
            }
            machineCaptureJob = (controller as? MachineDiagnosticEventSource)?.let { source ->
                captureScope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                    source.diagnosticEvents.collect { frame ->
                        captureEvent(
                            "machine_frame_${frame.name}",
                            null,
                            frame.fields + ("received_at" to frame.receivedAt.toString()),
                            frame.receivedAt,
                        )
                    }
                }
            }
            val initial = controller.deviceState.value
            val initialReading = scale.state.value.reading
            val scaleSamples = MutableStateFlow(
                initialReading?.let {
                    DerivedScaleSample(it, atMillis = -it.monotonicMark.elapsedNow().inWholeMilliseconds)
                },
            )
            val machineSamples = MutableStateFlow(initial to -(initial.telemetryTime?.let { (Clock.System.now() - it).inWholeMilliseconds } ?: 0L))
            scaleSamplesJob = captureScope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                val estimator = CupFlowEstimator(windowMillis = 1_000L, minimumWindowMillis = 500L)
                var baseline = initialReading
                var guard = initialReading?.let { LiveScaleSampleGuard(it.frame) }
                var sequence = 0L
                scale.readings.collect { reading ->
                    if (baseline?.let { reading.monotonicMark < it.monotonicMark } == true) return@collect
                    if (baseline == null) {
                        baseline = reading
                        guard = LiveScaleSampleGuard(reading.frame)
                    }
                    val at = clock.elapsedNow().inWholeMilliseconds - reading.monotonicMark.elapsedNow().inWholeMilliseconds
                    val invalid = guard?.accept(reading.frame)
                    val flow = if (invalid == null) estimator.add(at, reading.frame.weightGrams) else { estimator.reset(); null }
                    scaleSamples.value = DerivedScaleSample(reading, flow, at, invalid, ++sequence)
                    captureEvent("scale_measurement", at, mapOf("weight_g" to reading.frame.weightGrams.toString(), "cup_flow_g_per_s" to flow.toString(), "sample_at_ms" to at.toString(), "invalid" to invalid?.name.orEmpty()), reading.receivedAt, runId)
                }
            }
            machineSamplesJob = captureScope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                var previous = initial.telemetryTime
                controller.deviceState.collect { sample ->
                    if (sample.telemetryTime != null && sample.telemetryTime != previous) {
                        previous = sample.telemetryTime
                        val at = clock.elapsedNow().inWholeMilliseconds
                        machineSamples.value = sample to at
                        captureEvent("machine_measurement", at, mapOf("pressure_bar" to sample.pressure.toString(), "flow_ml_per_s" to sample.flowRate.toString(), "sample_at_ms" to at.toString()))
                    }
                }
            }
            val oldTelemetry = initial.telemetryTime
            if (mode == LiveTestMode.LiveControl) {
                recipe.document?.settings?.temperatureC?.let { temperature ->
                    check(controller.deviceState.value.brewStatus == DeviceState.BrewStatus.Idle)
                    withTimeout(TELEMETRY_STALE_MILLIS.milliseconds) { controller.settings.setBrewTemperature(temperature) }
                    captureEvent("constant_temperature_set", 0, mapOf("boiler_setpoint_c" to temperature.toString()))
                }
                session = controller.profiles.openLiveSession(initialPressure(recipe))
            }
            val engine = LiveTestProgramEngine(recipe, settings)
            recipe.profile.finishCondition.let { condition ->
                require(condition is app.geeflow.data.brew.model.Condition.Weight)
            }
            var lastScaleSequence = -1L
            var scaleDataValid = true
            var freshScaleMeasurement = false
            var cupFlow: Float? = null
            var fallbackStartedAt: Long? = null
            var lastCapturedPhaseId: String? = null
            var lastSent: PhaseControl? = null
            var lastPressure = 0f
            var lastCommandElapsed = 0L
            var lastTelemetryTimestamp = oldTelemetry
            var telemetryFreshAt = TimeSource.Monotonic.markNow()

            if (mode == LiveTestMode.LiveControl) {
                withTimeout(TELEMETRY_STALE_MILLIS.milliseconds) {
                    controller.deviceState.first { it.telemetryTime != oldTelemetry && controller.profiles.isLiveSessionActive(it) }
                }
            }

            while (currentCoroutineContext().isActive) {
                val machine = controller.deviceState.value
                val elapsed = clock.elapsedNow().inWholeMilliseconds
                if (machine.connectionStatus != DeviceState.ConnectionStatus.Connected) {
                    if (!machineLinkLossCaptured) {
                        machineLinkLossCaptured = true
                        captureEvent("machine_link_lost", elapsed, mapOf("simulated" to "false"))
                    }
                    error("Machine disconnected")
                }
                controller.requireReady(requireNotNull(state.value.deviceId))
                if (mode == LiveTestMode.LiveControl && !controller.profiles.isLiveSessionActive(machine)) {
                    error("Machine control mode changed")
                }
                if (mode == LiveTestMode.LiveControl && machine.brewStatus == DeviceState.BrewStatus.Idle) {
                    reason = "machine_stop"
                    break
                }
                val telemetryAt = machine.telemetryTime
                if (telemetryAt != null && telemetryAt != lastTelemetryTimestamp) {
                    lastTelemetryTimestamp = telemetryAt
                    telemetryFreshAt = TimeSource.Monotonic.markNow()
                    val baselinePressure = lastPressureBeforeCommand
                    val pressure = machine.pressure
                    if (!pressureResponseCaptured && baselinePressure != null && pressure != null &&
                        kotlin.math.abs(pressure - baselinePressure) >= MIN_PRESSURE_RESPONSE_BAR
                    ) {
                        pressureResponseCaptured = true
                        val sentAt = lastPressureCommandSentAt
                        captureEvent(
                            "first_pressure_response_observed",
                            elapsed,
                            mapOf(
                                "pressure_before_command_bar" to baselinePressure.toString(),
                                "pressure_after_command_bar" to pressure.toString(),
                                "latency_ms" to sentAt?.let { (telemetryAt - it).inWholeMilliseconds.toString() }.orEmpty(),
                            ),
                            telemetryAt,
                        )
                    }
                }
                if (telemetryFreshAt.elapsedNow().inWholeMilliseconds > TELEMETRY_STALE_MILLIS) error("Machine telemetry became stale")

                val derived = scaleSamples.value
                freshScaleMeasurement = derived != null && derived.sequence != lastScaleSequence && derived.flow != null
                if (derived != null) lastScaleSequence = derived.sequence
                val scaleReading = derived?.reading
                val scaleAge = scaleReading?.monotonicMark?.elapsedNow()?.inWholeMilliseconds
                cupFlow = derived?.flow
                if (derived?.invalid != null && scaleDataValid) {
                    scaleDataValid = false
                    mutableState.update { it.copy(fallbackStatus = "scale_invalid:${derived.invalid}") }
                    captureEvent("scale_invalid", elapsed, mapOf("reason" to derived.invalid.name))
                }
                if (scaleReading == null) {
                    if (mode != LiveTestMode.PassiveCapture) {
                        captureEvent("scale_link_lost", elapsed, mapOf("simulated" to "false"))
                        error("No valid BOOKOO weight packet. Passive capture can record unsupported packets.")
                    }
                    recordSample(elapsed, machine, null, null, null, null, null, null, null)
                    if (elapsed >= settings.shotDeadlineMillis) {
                        reason = "shot_deadline"
                        break
                    }
                    delay(EVALUATION_INTERVAL_MILLIS.milliseconds)
                    continue
                }
                val fallbackStart = scaleFallbackStartMillis(
                    fallbackStartedAt,
                    scaleDataValid,
                    scaleAge ?: Long.MAX_VALUE,
                    elapsed,
                    SCALE_STALE_MILLIS,
                ).takeUnless { mode == LiveTestMode.PassiveCapture }
                if (fallbackStart != null) {
                    if (fallbackStartedAt == null) {
                        fallbackStartedAt = fallbackStart
                        engine.resetFeedback(lastPressure)
                        genericEngine?.resetFeedback(lastPressure)
                        if (state.value.fallbackStatus == null) {
                            mutableState.update { current -> current.copy(fallbackStatus = "scale_stale") }
                        }
                        captureEvent(
                            "fallback_started",
                            elapsed,
                            mapOf("reason" to (state.value.fallbackStatus ?: "scale_stale")),
                        )
                    }
                    val fallback = scaleLossFallback(lastPressure, fallbackStart, settings.shotDeadlineMillis, settings.maximumPressureBar)
                    val fallbackUntil = minOf(fallbackStart + SCALE_FALLBACK_MILLIS, fallback.stopAtMillis)
                    if (elapsed >= fallbackUntil) {
                        reason = "scale_stale_fallback_timeout"
                        break
                    }
                    lastPressure = fallback.pressureBar
                    val fallbackTarget = controller.quantizePressureTarget(PhaseControl.Pressure(fallback.pressureBar))
                    val dispatchTarget = commandGate.offer(fallbackTarget, elapsed)
                    if (mode == LiveTestMode.LiveControl && dispatchTarget != null) {
                        val activeSession = requireNotNull(session)
                        val sent = transmitTarget(activeSession, dispatchTarget, elapsed)
                        lastSent = sent.target
                        if (sent.target is PhaseControl.Pressure) {
                            lastPressure = sent.target.bar
                            lastPressureBeforeCommand = machine.pressure
                            lastPressureCommandSentAt = sent.sentAt
                            pressureResponseCaptured = false
                        }
                        lastCommandElapsed = elapsed
                    }
                    recordSample(
                        elapsed, machine, scaleReading.frame.weightGrams, cupFlow,
                        scaleReading.frame.reportedFlowGramsPerSecond, lastPressure, state.value.phaseId,
                        scaleReading.receivedAt, scaleAge,
                    )
                    delay(EVALUATION_INTERVAL_MILLIS.milliseconds)
                    continue
                }

                val telemetry = BrewTelemetry(
                    buildMap {
                        machine.pressure?.let { put(BrewMetric.PumpPressure, it) }
                        machine.flowRate?.let { put(BrewMetric.PumpFlow, it) }
                        machine.volume?.let { put(BrewMetric.PumpedVolume, it) }
                        put(BrewMetric.CupWeight, scaleReading.frame.weightGrams)
                    },
                )
                val genericOutput = genericEngine?.let { runtime ->
                    val (sample, at) = machineSamples.value
                    runtime.tick(elapsed, RecipeMeasurements(buildMap {
                        sample.pressure?.let { put(RecipeMetric.Pressure, TimedRecipeValue(it, at)) }
                        sample.flowRate?.let { put(RecipeMetric.MachineFlow, TimedRecipeValue(it, at)) }
                        sample.volume?.let { put(RecipeMetric.Volume, TimedRecipeValue(it, at)) }
                        put(RecipeMetric.Weight, TimedRecipeValue(scaleReading.frame.weightGrams, derived.atMillis))
                        derived.flow?.let { put(RecipeMetric.CupFlow, TimedRecipeValue(it, derived.atMillis)) }
                    }))
                }
                genericOutput?.events?.forEach { captureEvent(it.type, elapsed, it.fields) }
                genericOutput?.let { output ->
                    captureEvent("recipe_control", elapsed, mapOf(
                        "phase_id" to output.phaseId.orEmpty(), "requested_target" to output.requestedTarget.toString(),
                        "target_unit" to output.targetUnit.toString(), "requested_pressure_bar" to output.requestedPressureBar.toString(),
                        "bounded_pressure_bar" to output.appliedPressureBar.toString(), "constraints" to output.constraints.joinToString(","),
                    ))
                }
                val evaluated = if (genericOutput != null) LiveTargetOutput(
                    genericOutput.appliedPressureBar?.let { PhaseControl.Pressure(it) }, genericOutput.phaseId, genericOutput.finish,
                ) else engine.tick(elapsed, telemetry, cupFlow, elapsed, freshScaleMeasurement)
                val output = if (mode == LiveTestMode.PassiveCapture) evaluated.copy(target = null) else evaluated
                if (output.phaseId != null && output.phaseId != lastCapturedPhaseId) {
                    lastCapturedPhaseId = output.phaseId
                    captureEvent("phase_transition", elapsed, mapOf("phase_id" to output.phaseId))
                }
                output.finish?.let {
                    reason = it
                    if (it == "global_weight") {
                        val weightTarget = recipe.document?.settings?.yieldGrams
                            ?: (recipe.profile.finishCondition as? app.geeflow.data.brew.model.Condition.Weight)?.target
                        captureEvent(
                            "weight_threshold_crossed",
                            elapsed,
                            mapOf(
                                "weight_g" to scaleReading.frame.weightGrams.toString(),
                                "target_g" to weightTarget?.toString().orEmpty(),
                            ),
                            scaleReading.receivedAt,
                        )
                    }
                    break
                }
                val target = output.target?.let { control ->
                    val capped = if (control is PhaseControl.Pressure) {
                        control.copy(bar = control.bar.coerceAtMost(settings.maximumPressureBar))
                    } else control
                    controller.quantizePressureTarget(capped)
                }
                if (mode != LiveTestMode.PassiveCapture && target is PhaseControl.Pressure) {
                    captureEvent(
                        "controller_calculation",
                        elapsed,
                        mapOf(
                            "phase_id" to output.phaseId.orEmpty(),
                            "measured_cup_flow_g_per_s" to cupFlow?.toString().orEmpty(),
                            "proposed_pressure_bar" to target.bar.toString(),
                            "control_mode" to mode.name,
                        ),
                    )
                }
                if (mode == LiveTestMode.LiveControl && target != null) {
                    val activeSession = requireNotNull(session)
                    val writeNeeded = target != lastSent || activeSession.requiresContinuousUpdates
                    val dispatchTarget = if (writeNeeded) commandGate.offer(target, elapsed) else null
                    if (dispatchTarget != null) {
                        val limited = dispatchTarget.withSlewLimit(lastPressure, elapsed - lastCommandElapsed, settings)
                        val sent = transmitTarget(activeSession, limited, elapsed)
                        when (val actual = sent.target) {
                            is PhaseControl.Pressure -> {
                                lastPressure = actual.bar
                                lastPressureBeforeCommand = machine.pressure
                                lastPressureCommandSentAt = sent.sentAt
                                pressureResponseCaptured = false
                            }
                            PhaseControl.PumpPause -> lastPressure = 0f
                            is PhaseControl.Flow -> error("Live test recipe cannot switch to machine flow mode")
                        }
                        lastSent = sent.target
                        lastCommandElapsed = elapsed
                    }
                }
                recordSample(
                    elapsed,
                    machine,
                    scaleReading.frame.weightGrams,
                    cupFlow,
                    scaleReading.frame.reportedFlowGramsPerSecond,
                    (target as? PhaseControl.Pressure)?.bar,
                    output.phaseId,
                    requireNotNull(scaleReading).receivedAt,
                    scaleAge,
                    genericOutput?.requestedTarget,
                    genericOutput?.targetUnit?.name,
                    genericOutput?.requestedPressureBar,
                    if (mode == LiveTestMode.LiveControl) lastPressure else null,
                    genericOutput?.constraints?.joinToString(","),
                )
                if (elapsed >= settings.shotDeadlineMillis) {
                    reason = "shot_deadline"
                    break
                }
                delay(EVALUATION_INTERVAL_MILLIS.milliseconds)
            }
        } catch (error: CancellationException) {
            reason = error.message ?: "user_stop"
        } catch (error: Exception) {
            reason = error.message ?: "control_failure"
            Logger.e(error) { "Live brew test stopped" }
        } finally {
            commandGate.stop()
            genericEngine?.stop(reason)
            val stopped = if (mode == LiveTestMode.LiveControl) {
                withContext(NonCancellable) {
                    confirmStop(controller, session, startedAt) {
                        captureEvent("stop_request", null, mapOf("reason" to reason))
                    }
                }
            } else true
            if (!stopped) captureEvent("stop_unconfirmed", null, mapOf("reason" to reason))
            val confirmedAt = Clock.System.now()
            val confirmationReading = scale.state.value.reading?.takeIf { scale.state.value.connected }
            captureEvent(
                "stop_confirmation",
                null,
                mapOf(
                    "confirmed" to stopped.toString(),
                    "final_weight_g" to confirmationReading?.frame?.weightGrams?.toString().orEmpty(),
                ),
                confirmedAt,
            )
            if (stopped && mode == LiveTestMode.LiveControl) {
                captureEvent("fresh_idle_confirmation", null, mapOf("source" to "machine_status"), confirmedAt)
            }
            withContext(NonCancellable) {
                machineCaptureJob?.cancelAndJoin()
                machineSamplesJob?.cancelAndJoin()
                if (stopped && mode != LiveTestMode.LiveControl) {
                    scaleCaptureJob?.cancelAndJoin()
                    scaleSamplesJob?.cancelAndJoin()
                }
            }
            val finalScaleWeight = scale.state.value.reading?.takeIf { scale.state.value.connected }?.frame?.weightGrams
            captureEvent("run_ended", null, mapOf("reason" to reason, "stop_confirmed" to stopped.toString()))
            mutableState.update { it.copy(active = false, stopPending = !stopped, message = reason) }
            if (stopped) {
                state.value.deviceId?.let { execution.endExternalControl(it) }
                if (mode == LiveTestMode.LiveControl) {
                    scheduleSettledWeight(
                        runId,
                        recipe.document?.settings?.yieldGrams
                            ?: (recipe.profile.finishCondition as? app.geeflow.data.brew.model.Condition.Weight)?.target,
                        scaleCaptureJob,
                        scaleSamplesJob,
                    )
                }
            } else {
                retryStop(controller, session, startedAt, scaleCaptureJob, scaleSamplesJob, recipe, runId)
            }
        }
    }

    private var commandSequence = 0L

    private fun initialCaptureEvents(
        deviceId: Long,
        mode: LiveTestMode,
        recipe: LiveTestRecipe,
        settings: LiveControlSettings,
    ): List<LiveTestCaptureEvent> = listOf(
        LiveTestCaptureEvent(
            type = "configuration",
            recordedAt = Clock.System.now(),
            elapsedMillis = 0,
            fields = mapOf(
                "schema_version" to "1",
                "device_id" to deviceId.toString(),
                "mode" to mode.name,
                "recipe_name" to recipe.profile.name,
                "recipe_snapshot" to (recipe.document?.let(LiveRecipeJson::encode).orEmpty()),
                "finish_weight_g" to (recipe.profile.finishCondition as app.geeflow.data.brew.model.Condition.Weight).target.toString(),
                "targets" to recipe.targets.entries.joinToString(";") { (id, target) -> "$id=$target" },
                "base_pressure_bar" to settings.basePressureBar.toString(),
                "pressure_ceiling_bar" to settings.maximumPressureBar.toString(),
                "slew_bar_per_second" to settings.slewBarPerSecond.toString(),
                "kp_bar_per_g_per_s" to settings.proportionalGainBarPerGramPerSecond.toString(),
                "ki_bar_per_g" to settings.integralGainBarPerGram.toString(),
                "command_interval_ms" to settings.commandIntervalMillis.toString(),
                "shot_deadline_ms" to settings.shotDeadlineMillis.toString(),
                "settings_validated_for_physical_control" to "false",
            ),
        ),
    )

    private fun captureEvent(
        type: String,
        elapsedMillis: Long?,
        fields: Map<String, String>,
        at: Instant = Clock.System.now(),
        runId: Long? = null,
    ) {
        val event = LiveTestCaptureEvent(type, at, elapsedMillis, fields)
        mutableState.update { current ->
            if (runId != null && current.runId != runId) return@update current
            current.copy(captureEvents = (current.captureEvents + event).takeLast(MAX_CAPTURE_EVENTS))
        }
    }

    private suspend fun transmitTarget(session: LiveBrewSession, target: PhaseControl, elapsedMillis: Long): DispatchedTarget {
        val id = ++commandSequence
        val enqueuedAt = Clock.System.now()
        captureEvent(
            "command_enqueued",
            elapsedMillis,
            mapOf("command_id" to id.toString(), "target" to target.toCaptureString()),
            enqueuedAt,
        )
        var dispatchedAt: Instant? = null
        try {
            val sent = withTimeout(TELEMETRY_STALE_MILLIS.milliseconds) {
                val onTransportSend = {
                    val stamp = Clock.System.now()
                    dispatchedAt = stamp
                    captureEvent(
                        "command_transport_send",
                        elapsedMillis,
                        mapOf("command_id" to id.toString(), "target" to target.toCaptureString()),
                        stamp,
                    )
                }
                if (session is TransportTimedLiveBrewSession) {
                    session.applyTarget(target, onTransportSend)
                } else {
                    onTransportSend()
                    session.applyTarget(target)
                }
            }
            val sentAt = requireNotNull(dispatchedAt) { "Transport did not report the target send." }
            captureEvent(
                "command_acknowledged",
                elapsedMillis,
                mapOf(
                    "command_id" to id.toString(),
                    "actual_target" to sent.toCaptureString(),
                    "acknowledgement_latency_ms" to (Clock.System.now() - sentAt).inWholeMilliseconds.toString(),
                ),
            )
            return DispatchedTarget(sent, sentAt)
        } catch (error: Exception) {
            captureEvent(
                "command_failed",
                elapsedMillis,
                mapOf("command_id" to id.toString(), "error" to (error.message ?: error::class.simpleName.orEmpty())),
            )
            throw error
        }
    }

    private data class DispatchedTarget(val target: PhaseControl, val sentAt: Instant)

    private fun recordSample(
        elapsed: Long,
        machine: DeviceState,
        weight: Float?,
        cupFlow: Float?,
        reportedFlow: Float?,
        target: Float?,
        phaseId: String?,
        scaleReceivedAt: Instant?,
        scaleAgeMillis: Long?,
        requestedControlValue: Float? = null,
        requestedControlUnit: String? = null,
        requestedPressureBar: Float? = null,
        commandedPressureBar: Float? = null,
        constraints: String? = null,
    ) {
        val machineAge = machine.telemetryTime?.let { (Clock.System.now() - it).inWholeMilliseconds }
        val sample = LiveTestSample(
            elapsed, machine.telemetryTime, scaleReceivedAt, machineAge, scaleAgeMillis,
            phaseId, machine.pressure, machine.flowRate, weight, cupFlow, reportedFlow, target,
            requestedControlValue, requestedControlUnit, requestedPressureBar, commandedPressureBar, constraints,
        )
        mutableState.update { current ->
            val fields = mapOf(
                "machine_telemetry_at" to machine.telemetryTime.toString(),
                "scale_received_at" to scaleReceivedAt.toString(),
                "machine_sample_age_ms" to machineAge?.toString().orEmpty(),
                "scale_sample_age_ms" to scaleAgeMillis?.toString().orEmpty(),
                "phase_id" to phaseId.orEmpty(),
                "pressure_bar" to machine.pressure?.toString().orEmpty(),
                "machine_flow_ml_per_s" to machine.flowRate?.toString().orEmpty(),
                "weight_g" to weight?.toString().orEmpty(),
                "derived_cup_flow_g_per_s" to cupFlow?.toString().orEmpty(),
                "reported_cup_flow_g_per_s" to reportedFlow?.toString().orEmpty(),
                "target_pressure_bar" to target?.toString().orEmpty(),
                "requested_control_value" to requestedControlValue?.toString().orEmpty(),
                "requested_control_unit" to requestedControlUnit.orEmpty(),
                "requested_pressure_bar" to requestedPressureBar?.toString().orEmpty(),
                "commanded_pressure_bar" to commandedPressureBar?.toString().orEmpty(),
                "constraints" to constraints.orEmpty(),
            )
            val event = LiveTestCaptureEvent("sample", Clock.System.now(), elapsed, fields)
            current.copy(
                phaseId = phaseId,
                lastSample = sample,
                samples = (current.samples + sample).takeLast(MAX_RETAINED_SAMPLES),
                captureEvents = (current.captureEvents + event).takeLast(MAX_CAPTURE_EVENTS),
            )
        }
    }

    private suspend fun confirmStop(
        controller: DeviceController,
        session: LiveBrewSession?,
        startedAt: Instant,
        onStopRequest: () -> Unit = {},
    ): Boolean = try {
        withTimeout(STOP_TIMEOUT_MILLIS.milliseconds) {
            val machine = controller.deviceState.value
            if (machine.brewStatus != DeviceState.BrewStatus.Idle) {
                check(machine.connectionStatus == DeviceState.ConnectionStatus.Connected)
                onStopRequest()
                session?.stop() ?: controller.profiles.stopLiveSession()
            }
            controller.deviceState.first {
                it.connectionStatus == DeviceState.ConnectionStatus.Connected &&
                    it.brewStatus == DeviceState.BrewStatus.Idle &&
                    it.statusTime?.let { stamp -> stamp >= startedAt } == true
            }
        }
        true
    } catch (_: Exception) {
        false
    }

    private fun retryStop(
        controller: DeviceController,
        session: LiveBrewSession?,
        startedAt: Instant,
        scaleCaptureJob: Job?,
        scaleSamplesJob: Job?,
        recipe: LiveTestRecipe,
        runId: Long,
    ) {
        scope.launch {
            while (currentCoroutineContext().isActive && state.value.stopPending) {
                delay(STOP_RETRY_MILLIS.milliseconds)
                controller.deviceState.first { it.connectionStatus == DeviceState.ConnectionStatus.Connected }
                val hasLinkLoss = state.value.captureEvents.any { it.type == "machine_link_lost" }
                if (hasLinkLoss && state.value.captureEvents.none { it.type == "machine_reconnected" }) {
                    captureEvent("machine_reconnected", null, mapOf("source" to "connection_observed"), runId = runId)
                }
                captureEvent("stop_verification_retry", null, mapOf("source" to if (hasLinkLoss) "after_reconnect" else "retry"), runId = runId)
                if (confirmStop(controller, session, startedAt) {
                    captureEvent(
                        "reconnect_stop_request",
                        null,
                        mapOf("source" to if (hasLinkLoss) "after_reconnect" else "retry"),
                        runId = runId,
                    )
                }) {
                    captureEvent("fresh_idle_confirmation", null, mapOf("source" to "machine_status"), runId = runId)
                    mutableState.update { if (it.runId == runId) it.copy(stopPending = false) else it }
                    state.value.deviceId?.let { execution.endExternalControl(it) }
                    scheduleSettledWeight(
                        runId,
                        recipe.document?.settings?.yieldGrams
                            ?: (recipe.profile.finishCondition as? app.geeflow.data.brew.model.Condition.Weight)?.target,
                        scaleCaptureJob,
                        scaleSamplesJob,
                    )
                }
            }
        }
    }

    private fun scheduleSettledWeight(
        runId: Long,
        targetGrams: Float?,
        scaleCaptureJob: Job?,
        scaleSamplesJob: Job?,
    ) {
        settledWeightJob?.cancel()
        settledWeightJob = scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            try {
                recordSettledWeight(targetGrams, runId)
            } finally {
                withContext(NonCancellable) {
                    scaleCaptureJob?.cancelAndJoin()
                    scaleSamplesJob?.cancelAndJoin()
                }
            }
        }
    }

    private suspend fun cancelSettledWeightJob() {
        val job = settledWeightJob
        settledWeightJob = null
        job?.cancelAndJoin()
    }

    private suspend fun recordSettledWeight(targetGrams: Float?, runId: Long) {
        val settled = awaitSettledScaleReading()
        if (settled == null) {
            val latest = scale.state.value.reading?.takeIf { scale.state.value.connected }
            captureEvent(
                "settled_weight_not_observed",
                null,
                mapOf(
                    "target_g" to targetGrams?.toString().orEmpty(),
                    "last_weight_g" to latest?.frame?.weightGrams?.toString().orEmpty(),
                    "timeout_ms" to SETTLED_WEIGHT_TIMEOUT_MILLIS.toString(),
                ),
                runId = runId,
            )
        } else {
            captureEvent(
                "settled_weight_observed",
                null,
                mapOf(
                    "weight_g" to settled.frame.weightGrams.toString(),
                    "target_g" to targetGrams?.toString().orEmpty(),
                    "stability_window_ms" to SETTLED_WEIGHT_WINDOW_MILLIS.toString(),
                    "tolerance_g" to SETTLED_WEIGHT_TOLERANCE_GRAMS.toString(),
                ),
                settled.receivedAt,
                runId,
            )
        }
    }

    private suspend fun awaitSettledScaleReading(): BookooScaleReading? {
        if (!scale.state.value.connected) return null
        var latest: BookooScaleReading? = null
        var stableSince: Instant? = null
        var minimumWeight = Float.POSITIVE_INFINITY
        var maximumWeight = Float.NEGATIVE_INFINITY
        return withTimeoutOrNull(SETTLED_WEIGHT_TIMEOUT_MILLIS.milliseconds) {
            scale.readings.first { reading ->
                if (!scale.state.value.connected) return@first false
                val previous = latest
                val gap = previous?.let { (reading.receivedAt - it.receivedAt).inWholeMilliseconds }
                if (previous == null || gap == null || gap > SETTLED_WEIGHT_MAX_SAMPLE_GAP_MILLIS) {
                    stableSince = reading.receivedAt
                    minimumWeight = reading.frame.weightGrams
                    maximumWeight = reading.frame.weightGrams
                } else {
                    minimumWeight = minOf(minimumWeight, reading.frame.weightGrams)
                    maximumWeight = maxOf(maximumWeight, reading.frame.weightGrams)
                    if (maximumWeight - minimumWeight > SETTLED_WEIGHT_TOLERANCE_GRAMS) {
                        stableSince = reading.receivedAt
                        minimumWeight = reading.frame.weightGrams
                        maximumWeight = reading.frame.weightGrams
                    }
                }
                latest = reading
                stableSince?.let { (reading.receivedAt - it).inWholeMilliseconds >= SETTLED_WEIGHT_WINDOW_MILLIS } == true
            }
            latest
        }
    }

    private fun initialPressure(recipe: LiveTestRecipe): PhaseControl.Pressure {
        if (recipe.document != null) return PhaseControl.Pressure(0f)
        val first = (recipe.profile.program as app.geeflow.data.brew.model.BrewProgram.Phases).phases.first()
        return when (val target = recipe.targets.getValue(first.id)) {
            is LiveControlTarget.Pressure -> PhaseControl.Pressure(target.bar, PressureLocation.Pump)
            is LiveControlTarget.CupFlow -> PhaseControl.Pressure(target.basePressureBar, PressureLocation.Pump)
            LiveControlTarget.Pause -> PhaseControl.Pressure(0f, PressureLocation.Pump)
        }
    }

    private fun DeviceController.quantizePressureTarget(target: PhaseControl): PhaseControl = when (target) {
        is PhaseControl.Pressure -> target.withValue(
            requireNotNull(profilingCapabilities.livePressure[target.location]).quantize(target.bar),
        )
        is PhaseControl.Flow -> error("Live test recipe cannot switch to machine flow mode")
        PhaseControl.PumpPause -> target
    }

    private fun PhaseControl.withSlewLimit(
        currentPressure: Float,
        elapsedSinceCommandMillis: Long,
        settings: LiveControlSettings,
    ): PhaseControl = when (this) {
        is PhaseControl.Pressure -> {
            val maxDelta = settings.slewBarPerSecond * elapsedSinceCommandMillis
                .coerceAtLeast(settings.commandIntervalMillis) / 1_000f
            copy(bar = currentPressure + (bar - currentPressure).coerceIn(-maxDelta, maxDelta))
        }
        is PhaseControl.Flow -> error("Live test recipe cannot switch to machine flow mode")
        PhaseControl.PumpPause -> this
    }

    private fun PhaseControl.toCaptureString() = when (this) {
        is PhaseControl.Pressure -> "pressure:${bar}bar@${location.name}"
        is PhaseControl.Flow -> "flow:${millilitresPerSecond}ml/s"
        PhaseControl.PumpPause -> "pump_pause"
    }

    private companion object {
        const val SCALE_STALE_MILLIS = 1_000L
        const val TELEMETRY_STALE_MILLIS = 2_000L
        const val SCALE_FALLBACK_MILLIS = 5_000L
        const val START_WEIGHT_TOLERANCE_GRAMS = 1f
        const val EVALUATION_INTERVAL_MILLIS = 50L
        const val MIN_PRESSURE_RESPONSE_BAR = 0.05f
        const val STOP_TIMEOUT_MILLIS = 3_000L
        const val STOP_RETRY_MILLIS = 500L
        const val SETTLED_WEIGHT_TIMEOUT_MILLIS = 2_500L
        const val SETTLED_WEIGHT_WINDOW_MILLIS = 750L
        const val SETTLED_WEIGHT_MAX_SAMPLE_GAP_MILLIS = 400L
        const val SETTLED_WEIGHT_TOLERANCE_GRAMS = 0.25f
        const val MAX_RETAINED_SAMPLES = 2_000
        const val MAX_CAPTURE_EVENTS = 12_000
    }
}

private data class DerivedScaleSample(val reading: BookooScaleReading, val flow: Float? = null, val atMillis: Long = 0, val invalid: ScaleInvalidReason? = null, val sequence: Long = 0)
