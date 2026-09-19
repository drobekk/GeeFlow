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
import app.geeflow.data.device.model.withValue
import app.geeflow.domain.exception.AppBackgroundedException
import app.geeflow.domain.exception.DeviceNotConnectedException
import app.geeflow.domain.exception.MachineBusyException
import app.geeflow.domain.exception.MachineControlChangedException
import app.geeflow.domain.exception.ProfileUnavailableException
import app.geeflow.domain.exception.RequiredMeasurementLostException
import app.geeflow.domain.exception.TelemetryTimeoutException
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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlin.time.TimeSource

data class ProfileRunState(
    val deviceId: Long? = null,
    val active: Boolean = false,
    val trace: ProfileExecutionTrace? = null,
    val stopPending: Boolean = false,
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
        if (job?.isActive == true || state.value.stopPending) throw MachineBusyException(deviceId)
        val controller = provider.getController(deviceId)
        if (controller.deviceState.value.connectionStatus != DeviceState.ConnectionStatus.Connected) {
            throw DeviceNotConnectedException(deviceId)
        }
        if (controller.deviceState.value.brewStatus != DeviceState.BrewStatus.Idle) {
            throw MachineBusyException(deviceId)
        }

        val support = controller.assessProfile(profile, checkAvailability = true)
        if (support.execution == ProfileExecution.Unsupported || support.issues.isNotEmpty()) {
            throw ProfileUnavailableException()
        }

        if (support.execution == ProfileExecution.Native) {
            controller.startProfileBrewing(profile)
        } else {
            if (!foreground.value) throw AppBackgroundedException()
            // Copy collections into an immutable execution snapshot.
            val snapshot = Json.decodeFromString<BrewProfile>(Json.encodeToString(profile))
            mutableState.value = ProfileRunState(deviceId, true, ProfileExecutionTrace(snapshot, Clock.System.now()))
            job = scope.launch { run(controller, snapshot) }
        }
    }

    fun owns(deviceId: Long) = (state.value.active || state.value.stopPending) && state.value.deviceId == deviceId

    suspend fun stop(deviceId: Long): Boolean {
        val running = mutex.withLock {
            if (!owns(deviceId)) return false
            if (state.value.stopPending) return true
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
        if (state.value.active || state.value.stopPending) throw MachineBusyException(deviceId)
    }

    suspend fun <T> withUnownedControl(deviceId: Long, action: suspend () -> T): T = mutex.withLock {
        checkUnowned(deviceId)
        action()
    }

    private suspend fun run(
        controller: DeviceController,
        profile: BrewProfile,
    ) {
        var session: LiveBrewSession? = null
        var reason = "user_stop"
        try {
            val oldStamp = controller.deviceState.value.telemetryTime
            session = controller.openLiveSession(profile.initialControl())
            val clock = TimeSource.Monotonic.markNow()
            val engine = ProfileProgramEngine(profile)
            val initial = engine.tick(0, controller.telemetry())
            val target = initial.target?.let(controller::quantize)
            val sent = target?.let {
                withTimeout(TELEMETRY_TIMEOUT_MS.milliseconds) { session.applyTarget(it) }
            }
            if (sent != null) {
                mutableState.update { state ->
                    state.copy(trace = state.trace?.copy(targets = listOf(EmittedTarget(0, initial.phaseId, sent))))
                }
            }
            withTimeout(TELEMETRY_TIMEOUT_MS.milliseconds) {
                controller.deviceState.first { it.telemetryTime != oldStamp && controller.isLiveSessionActive(it) }
            }
            reason = initial.finish ?: controlLoop(controller, session, profile, clock, engine, target, sent)
        } catch (error: CancellationException) {
            reason = error.message ?: "user_stop"
        } catch (error: Exception) {
            reason = error.message ?: "control_failure"
        } finally {
            val stopped = confirmStop(controller, session)
            mutableState.update {
                it.copy(
                    active = false,
                    stopPending = !stopped,
                    message = reason,
                    trace = it.trace?.copy(endReason = reason),
                )
            }
            try {
                state.value.trace?.let(recorder::save)
            } catch (error: Exception) {
                Logger.e(error) { "Could not save the app-controlled brew" }
            }
            if (!stopped) retryStop(controller, session)
        }
    }

    @Suppress("ThrowsCount")
    private suspend fun controlLoop(
        controller: DeviceController,
        session: LiveBrewSession,
        profile: BrewProfile,
        clock: TimeSource.Monotonic.ValueTimeMark,
        engine: ProfileProgramEngine,
        initialTarget: PhaseControl?,
        initialSent: PhaseControl?,
    ): String {
        var freshAt = TimeSource.Monotonic.markNow()
        var stamp: Instant? = null
        var lastWrite = 0L
        var lastTarget = initialTarget
        var lastSent = initialSent

        while (currentCoroutineContext().isActive) {
            val device = controller.deviceState.value
            val currentDeviceId = state.value.deviceId ?: 0L
            if (device.connectionStatus != DeviceState.ConnectionStatus.Connected) {
                throw DeviceNotConnectedException(currentDeviceId)
            }

            if (device.brewStatus == DeviceState.BrewStatus.Idle) return "machine_stop"
            if (!controller.isLiveSessionActive(device)) {
                throw MachineControlChangedException()
            }

            if (device.telemetryTime != stamp) {
                stamp = device.telemetryTime
                freshAt = TimeSource.Monotonic.markNow()
                val elapsed = clock.elapsedNow().inWholeMilliseconds
                recordTelemetrySample(device, elapsed)
            }
            if (freshAt.elapsedNow().inWholeMilliseconds >= TELEMETRY_TIMEOUT_MS) throw TelemetryTimeoutException()
            val telemetry = controller.telemetry()
            if (!profile.requiredMetrics().all { telemetry[it] != null }) throw RequiredMeasurementLostException()
            val elapsed = clock.elapsedNow().inWholeMilliseconds
            val output = engine.tick(elapsed, telemetry)

            mutableState.update { it.copy(trace = it.trace?.copy(transitions = engine.transitions.toList())) }
            output.finish?.let { return it }

            val target = controller.quantize(requireNotNull(output.target))
            val interval = maxOf(MINIMUM_WRITE_INTERVAL_MS, controller.profilingCapabilities.minimumWriteIntervalMillis)
            val shouldWriteTarget = target != lastTarget || session.requiresContinuousUpdates
            val isIntervalPassed = elapsed - lastWrite >= interval

            if (shouldWriteTarget && isIntervalPassed) {
                val sent = withTimeout(TELEMETRY_TIMEOUT_MS.milliseconds) { session.applyTarget(target) }
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
            delay(EVALUATION_INTERVAL_MS.milliseconds)
        }
        return "user_stop"
    }

    /** No more targets can be sent after this point, including after cancellation. */
    private suspend fun confirmStop(controller: DeviceController, session: LiveBrewSession?): Boolean =
        withContext(NonCancellable) {
            val startedAt = state.value.trace?.startedAt

            try {
                withTimeout(STOP_TIMEOUT_MS.milliseconds) {
                    val device = controller.deviceState.value
                    check(device.connectionStatus == DeviceState.ConnectionStatus.Connected)
                    val confirmedIdle = device.brewStatus == DeviceState.BrewStatus.Idle &&
                        device.statusTime?.let { startedAt != null && it >= startedAt } == true
                    if (!confirmedIdle) {
                        if (session != null) session.stop() else controller.stopLiveSession()
                    }
                    controller.deviceState.first {
                        it.brewStatus == DeviceState.BrewStatus.Idle &&
                            it.connectionStatus == DeviceState.ConnectionStatus.Connected &&
                            it.statusTime?.let { stamp -> startedAt != null && stamp >= startedAt } == true
                    }
                }
                true
            } catch (error: Exception) {
                Logger.w(error) { "App-controlled brew stop was not confirmed; will retry" }
                false
            }
        }

    private fun retryStop(controller: DeviceController, session: LiveBrewSession?) {
        scope.launch {
            while (isActive && state.value.stopPending) {
                delay(STOP_RETRY_INTERVAL_MS.milliseconds)
                controller.deviceState.first { it.connectionStatus == DeviceState.ConnectionStatus.Connected }
                if (confirmStop(controller, session)) {
                    mutableState.update { it.copy(stopPending = false) }
                }
            }
        }
    }

    private fun recordTelemetrySample(device: DeviceState, elapsed: Long) {
        val sample = FreeHandSample(
            elapsed,
            BrewDataPoint(
                pressure = device.pressure ?: 0f,
                weight = device.weight ?: 0f,
                volume = device.volume ?: 0f,
                flowRate = device.flowRate ?: 0f,
                weightRate = device.weightRate ?: 0f,
            ),
        )
        mutableState.update { state ->
            state.copy(
                trace = state.trace?.let {
                    it.copy(measurements = it.measurements + sample)
                },
            )
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
        requireNotNull(profilingCapabilities.livePressure[target.location]).quantize(target.bar),
    )

    is PhaseControl.Flow -> target.withValue(requireNotNull(profilingCapabilities.liveFlow).quantize(target.millilitresPerSecond))
    PhaseControl.PumpPause -> target
}

private const val TELEMETRY_TIMEOUT_MS = 2000L
private const val STOP_TIMEOUT_MS = 3000L
private const val STOP_RETRY_INTERVAL_MS = 500L
private const val MINIMUM_WRITE_INTERVAL_MS = 50L
private const val EVALUATION_INTERVAL_MS = 50L
