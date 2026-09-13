package app.geeflow.domain.device

import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.EmittedTarget
import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.brew.model.FreeHandSample
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.ProfileExecutionTrace
import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.LiveBrewSession
import app.geeflow.data.device.assessProfile
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.ProfileExecution
import app.geeflow.data.device.model.requiredMetrics
import app.geeflow.data.device.model.value
import app.geeflow.data.device.model.withValue
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.TimeSource

data class ProfileRunState(
    val deviceId: Long? = null,
    val active: Boolean = false,
    val trace: ProfileExecutionTrace? = null,
    val manualStopRequired: Boolean = false,
    val message: String? = null,
)

/** Owns live profile commands independently of navigation and ViewModels. */
@Single
class ProfileExecutionCoordinator(
    private val provider: DeviceControllerProvider,
    private val scope: CoroutineScope,
    private val recorder: ProfileExecutionRecorder,
) {
    private val mutex = Mutex()
    private var job: Job? = null
    private val mutableState = MutableStateFlow(ProfileRunState())
    val state: StateFlow<ProfileRunState> = mutableState.asStateFlow()
    private val foreground = MutableStateFlow(true)

    suspend fun start(deviceId: Long, profile: BrewProfile) = mutex.withLock {
        check(job?.isActive != true && !state.value.manualStopRequired) { "Another brew owns device control" }
        val controller = provider.getController(deviceId)
        check(controller.deviceState.value.connectionStatus == DeviceState.ConnectionStatus.Connected)
        check(controller.deviceState.value.brewStatus == DeviceState.BrewStatus.Idle) {
            "The machine is already brewing"
        }
        val support = controller.assessProfile(profile, checkAvailability = true)
        require(support.execution != ProfileExecution.Unsupported && support.issues.isEmpty()) {
            "Profile unavailable: ${support.issues}"
        }
        if (support.execution == ProfileExecution.Native) {
            controller.startProfileBrewing(profile)
        } else {
            check(foreground.value) { "Keep the app in the foreground to run this profile" }
            // Copy collections into an immutable execution snapshot.
            val snapshot = Json.decodeFromString<BrewProfile>(Json.encodeToString(profile))
            val clock = TimeSource.Monotonic.markNow()
            mutableState.value = ProfileRunState(deviceId, true, ProfileExecutionTrace(snapshot, Clock.System.now()))
            job = scope.launch { run(controller, snapshot, clock) }
        }
    }

    fun owns(deviceId: Long) = state.value.active && state.value.deviceId == deviceId

    suspend fun stop(deviceId: Long): Boolean {
        val running = mutex.withLock {
            if (!owns(deviceId)) return false
            job
        }
        running?.cancelAndJoin()
        return true
    }

    fun setForeground(value: Boolean) {
        foreground.value = value
        if (!value) scope.launch { mutex.withLock { job?.cancel(CancellationException("App moved to background")) } }
    }

    fun checkUnowned(deviceId: Long) {
        check(!state.value.active && !state.value.manualStopRequired) {
            "An app-controlled profile owns the machine: $deviceId"
        }
    }

    suspend fun <T> withUnownedControl(deviceId: Long, action: suspend () -> T): T = mutex.withLock {
        checkUnowned(deviceId)
        action()
    }

    private suspend fun run(
        controller: DeviceController,
        profile: BrewProfile,
        clock: TimeSource.Monotonic.ValueTimeMark
    ) {
        var session: LiveBrewSession? = null
        var reason = "user_stop"
        try {
            val oldStamp = controller.deviceState.value.telemetryTime
            session = controller.openLiveSession(profile.initialControl())
            withTimeout(TELEMETRY_TIMEOUT_MS) {
                controller.deviceState.first { it.telemetryTime != oldStamp && controller.isLiveSessionActive(it) }
            }
            reason = controlLoop(controller, session, profile, clock)
        } catch (error: CancellationException) {
            reason = error.message ?: "user_stop"
        } catch (error: Exception) {
            reason = error.message ?: "control_failure"
        } finally {
            val stopped = confirmStop(controller, session)
            mutableState.update {
                it.copy(
                    active = false,
                    manualStopRequired = !stopped,
                    message = reason,
                    trace = it.trace?.copy(endReason = reason)
                )
            }
            try {
                state.value.trace?.let(recorder::save)
            } catch (error: Exception) {
                Logger.e(error) { "Could not save the app-controlled brew" }
            }
            if (!stopped) reconcileOnReconnect(controller)
        }
    }

    private suspend fun controlLoop(
        controller: DeviceController,
        session: LiveBrewSession,
        profile: BrewProfile,
        clock: TimeSource.Monotonic.ValueTimeMark,
    ): String {
        var freshAt = TimeSource.Monotonic.markNow()
        var stamp: kotlin.time.Instant? = null
        var lastWrite: Long? = null
        var lastTarget: PhaseControl? = null
        var lastSent: PhaseControl? = null
        val engine = ProfileProgramEngine(profile)
        while (currentCoroutineContext().isActive) {
            val device = controller.deviceState.value
            check(device.connectionStatus == DeviceState.ConnectionStatus.Connected) { "Connection lost" }
            if (device.brewStatus == DeviceState.BrewStatus.Idle) return "machine_stop"
            check(controller.isLiveSessionActive(device)) { "Machine control changed" }
            if (device.telemetryTime != stamp) {
                stamp = device.telemetryTime
                freshAt = TimeSource.Monotonic.markNow()
                val sample = FreeHandSample(
                    clock.elapsedNow().inWholeMilliseconds,
                    BrewDataPoint(
                        pressure = device.pressure ?: 0f,
                        weight = device.weight ?: 0f,
                        volume = device.volume ?: 0f,
                        flowRate = device.flowRate ?: 0f,
                        weightRate = device.weightRate ?: 0f,
                    )
                )
                mutableState.update { state ->
                    state.copy(
                        trace = state.trace?.let {
                            it.copy(measurements = it.measurements + sample)
                        }
                    )
                }
            }
            check(freshAt.elapsedNow().inWholeMilliseconds < TELEMETRY_TIMEOUT_MS) { "Telemetry timed out" }
            val telemetry = controller.telemetry()
            check(profile.requiredMetrics().all { telemetry[it] != null }) { "Required measurement lost" }
            val elapsed = clock.elapsedNow().inWholeMilliseconds
            val output = engine.tick(elapsed, telemetry)
            mutableState.update { it.copy(trace = it.trace?.copy(transitions = engine.transitions.toList())) }
            output.finish?.let { return it }
            val target = controller.quantize(requireNotNull(output.target))
            val interval = maxOf(MINIMUM_WRITE_INTERVAL_MS, controller.profilingCapabilities.minimumWriteIntervalMillis)
            if ((target != lastTarget || session.requiresContinuousUpdates) &&
                (lastWrite == null || elapsed - lastWrite >= interval)
            ) {
                val sent = withTimeout(TELEMETRY_TIMEOUT_MS) { session.applyTarget(target) }
                lastTarget = target
                lastWrite = clock.elapsedNow().inWholeMilliseconds
                if (sent != lastSent) {
                    val event = EmittedTarget(lastWrite, output.phaseId, sent)
                    mutableState.update { state ->
                        state.copy(trace = state.trace?.let { it.copy(targets = it.targets + event) })
                    }
                    lastSent = sent
                }
            }
            delay(EVALUATION_INTERVAL_MS)
        }
        return "user_stop"
    }

    /** No more targets can be sent after this point, including after cancellation. */
    private suspend fun confirmStop(controller: DeviceController, session: LiveBrewSession?): Boolean =
        withContext(NonCancellable) {
            // A partially acknowledged start must never be treated as a confirmed idle machine.
            if (session == null) return@withContext false
            val startedAt = state.value.trace?.startedAt
            try {
                withTimeout(STOP_TIMEOUT_MS) {
                    if (controller.deviceState.value.brewStatus != DeviceState.BrewStatus.Idle) {
                        check(controller.deviceState.value.connectionStatus == DeviceState.ConnectionStatus.Connected)
                        session.stop()
                    }
                    controller.deviceState.first {
                        it.brewStatus == DeviceState.BrewStatus.Idle &&
                            it.connectionStatus == DeviceState.ConnectionStatus.Connected &&
                            it.statusTime?.let { stamp -> startedAt != null && stamp >= startedAt } == true
                    }
                }
                true
            } catch (_: Exception) {
                false
            }
        }

    private fun reconcileOnReconnect(controller: DeviceController) {
        val previousStatus = controller.deviceState.value.statusTime
        scope.launch {
            controller.deviceState.first {
                it.connectionStatus == DeviceState.ConnectionStatus.Connected &&
                    it.brewStatus == DeviceState.BrewStatus.Idle && it.statusTime != previousStatus
            }
            mutableState.update { it.copy(manualStopRequired = false) }
        }
    }
}

private fun BrewProfile.initialControl(): PhaseControl = when (val program = program) {
    is BrewProgram.Phases -> program.phases.firstOrNull { it.control != PhaseControl.PumpPause }?.control
        ?: PhaseControl.PumpPause
    is BrewProgram.Recording -> if (program.recording.controlMode == FreeHandControlMode.Flow) {
        PhaseControl.Flow(0f)
    } else {
        PhaseControl.Pressure(0f)
    }
}

private fun DeviceController.quantize(target: PhaseControl): PhaseControl = when (target) {
    is PhaseControl.Pressure -> target.withValue(
        requireNotNull(profilingCapabilities.livePressure[target.location]).quantize(target.bar)
    )
    is PhaseControl.Flow -> target.withValue(requireNotNull(profilingCapabilities.liveFlow).quantize(target.millilitresPerSecond))
    PhaseControl.PumpPause -> target
}

private const val TELEMETRY_TIMEOUT_MS = 2000L
private const val STOP_TIMEOUT_MS = 3000L
private const val MINIMUM_WRITE_INTERVAL_MS = 200L
private const val EVALUATION_INTERVAL_MS = 100L
