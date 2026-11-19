package app.geeflow.domain.device

import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.BrewScaleSample
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.EmittedTarget
import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.brew.model.FreeHandSample
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.ProfileExecutionTrace
import app.geeflow.data.brew.model.ProfileMachineDiagnostic
import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.LiveBrewSession
import app.geeflow.data.device.MachineDiagnosticEventSource
import app.geeflow.data.device.assessProfile
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.BrewTelemetry
import app.geeflow.data.device.model.ProfileExecution
import app.geeflow.data.device.model.requiredMetrics
import app.geeflow.data.device.model.withValue
import app.geeflow.domain.device.usecase.requireReady
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
    val weightCutoff: AppWeightCutoff? = null,
    val nativeExecution: Boolean = false,
)

/** Owns live profile commands independently of navigation and ViewModels. */
@Single
class ProfileExecutionCoordinator(
    private val provider: DeviceControllerProvider,
    private val scale: app.geeflow.data.device.ScaleReadingSource,
    private val scope: CoroutineScope,
    private val recorder: ProfileExecutionRecorder,
) {
    private val mutex = Mutex()
    private var job: Job? = null
    private var externalOwnerDeviceId: Long? = null
    private val mutableState = MutableStateFlow(ProfileRunState())
    val state: StateFlow<ProfileRunState> = mutableState.asStateFlow()
    private val foreground = MutableStateFlow(true)

    suspend fun start(
        deviceId: Long,
        profile: BrewProfile,
        weightCutoff: AppWeightCutoff? = null,
    ) = mutex.withLock {
        if (job?.isActive == true || state.value.stopPending || externalOwnerDeviceId != null) {
            throw MachineBusyException(deviceId)
        }
        val executionProfile = weightCutoff?.let {
            profile.copy(finishCondition = Condition.Weight(it.targetGrams))
        } ?: profile
        val controller = provider.getController(deviceId)
        controller.requireReady(deviceId)
        if (controller.deviceState.value.brewStatus != DeviceState.BrewStatus.Idle) {
            throw MachineBusyException(deviceId)
        }

        val support = controller.assessProfile(
            executionProfile,
            checkAvailability = true,
            weightFinishHandledByAppCutoff = weightCutoff != null,
        )
        if (support.execution == ProfileExecution.Unsupported || support.issues.isNotEmpty()) {
            throw ProfileUnavailableException()
        }

        if (support.execution == ProfileExecution.Native && weightCutoff == null) {
            controller.profiles.startProfileBrewing(executionProfile)
        } else {
            if (!foreground.value) throw AppBackgroundedException()
            val nativeExecution = support.execution == ProfileExecution.Native
            // Copy collections into an immutable execution snapshot.
            val snapshot = Json.decodeFromString<BrewProfile>(Json.encodeToString(executionProfile))
            val source = weightCutoff?.source?.name
            val trace = ProfileExecutionTrace(
                profile = snapshot,
                startedAt = Clock.System.now(),
                stopMode = weightCutoff?.let { "app_weight_cutoff" },
                scaleSource = source,
                stopTargetGrams = weightCutoff?.targetGrams,
            )
            mutableState.value = ProfileRunState(
                deviceId = deviceId,
                active = true,
                trace = trace,
                weightCutoff = weightCutoff,
                nativeExecution = nativeExecution,
            )
            try {
                if (nativeExecution) controller.profiles.startProfileBrewing(snapshot)
            } catch (error: Exception) {
                var stopRequestedAt: Instant? = null
                val stopped = confirmStop(controller, session = null, nativeExecution = nativeExecution) {
                    stopRequestedAt = Clock.System.now()
                }
                mutableState.update { current ->
                    current.copy(
                        active = false,
                        stopPending = !stopped,
                        message = error.message,
                        trace = current.trace?.let { trace ->
                            trace.copy(
                                endReason = "start_failure",
                                stopRequestedAt = stopRequestedAt ?: trace.stopRequestedAt,
                                stopConfirmedAt = Clock.System.now().takeIf { stopped },
                            )
                        },
                    )
                }
                if (!stopped) retryStop(controller, session = null, nativeExecution = nativeExecution)
                throw error
            }
            job = scope.launch { run(controller, snapshot, weightCutoff, nativeExecution) }
        }
    }

    fun owns(deviceId: Long) = (state.value.active || state.value.stopPending) && state.value.deviceId == deviceId

    fun updateShotMetadata(
        deviceId: Long,
        startedAt: Instant,
        doseGrams: Float?,
        grindSetting: String?,
        extractionStartMillis: Long?,
        extractionStartEstimated: Boolean?,
    ) {
        mutableState.update { current ->
            val trace = current.trace
            if (current.deviceId != deviceId || trace?.startedAt != startedAt) current
            else current.copy(
                trace = trace.copy(
                    doseGrams = doseGrams,
                    grindSetting = grindSetting,
                    extractionStartMillis = extractionStartMillis,
                    extractionStartEstimated = extractionStartEstimated,
                ),
            )
        }
    }

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
        if (state.value.active || state.value.stopPending || externalOwnerDeviceId == deviceId) {
            throw MachineBusyException(deviceId)
        }
    }

    suspend fun <T> withUnownedControl(deviceId: Long, action: suspend () -> T): T = mutex.withLock {
        checkUnowned(deviceId)
        action()
    }

    /** Reserve the normal machine command path for another attended controller. */
    suspend fun beginExternalControl(deviceId: Long) = mutex.withLock {
        if (job?.isActive == true || state.value.stopPending || externalOwnerDeviceId != null) {
            throw MachineBusyException(deviceId)
        }
        externalOwnerDeviceId = deviceId
    }

    /** Release ownership only after the external controller confirms the machine is idle. */
    suspend fun endExternalControl(deviceId: Long) = mutex.withLock {
        if (externalOwnerDeviceId == deviceId) externalOwnerDeviceId = null
    }

    private suspend fun run(
        controller: DeviceController,
        profile: BrewProfile,
        weightCutoff: AppWeightCutoff?,
        nativeExecution: Boolean,
    ) {
        var session: LiveBrewSession? = null
        var reason = "user_stop"
        var scaleTelemetryJob: Job? = null
        var machineDiagnosticJob: Job? = null
        try {
            controller.requireReady(requireNotNull(state.value.deviceId))
            val clock = TimeSource.Monotonic.markNow()
            val startedAt = state.value.trace?.startedAt ?: Clock.System.now()
            machineDiagnosticJob = (controller as? MachineDiagnosticEventSource)?.let { source ->
                scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                    source.diagnosticEvents.collect { event ->
                        // Keep unmatched and bad-CRC frames as well as validated replies: they
                        // explain failures that would otherwise look like a missing response.
                        if (event.receivedAt >= startedAt) {
                            mutableState.update { current ->
                                current.copy(trace = current.trace?.let { trace ->
                                    trace.copy(machineDiagnostics = (trace.machineDiagnostics + ProfileMachineDiagnostic(event.receivedAt, event.name, event.fields)).takeLast(5000))
                                })
                            }
                        }
                    }
                }
            }
            scaleTelemetryJob = scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                var previous: BrewScaleSample? = null
                val flowSmoother = CupFlowEstimator(windowMillis = 1_000L, minimumWindowMillis = 500L)
                scale.packets.collect { packet ->
                    if (packet.receivedAt < startedAt) return@collect
                    val elapsed = (packet.receivedAt - startedAt).inWholeMilliseconds.coerceAtLeast(0L)
                    val smoothedFlow = if (packet.decoded) {
                        packet.frame?.let { flowSmoother.add(elapsed, it.weightGrams) }
                    } else null
                    val sample = packet.toBrewScaleSample(startedAt, previous, smoothedFlow)
                    if (sample.decoded) previous = sample
                    mutableState.update { current ->
                        current.copy(
                            trace = current.trace?.copy(scaleSamples = current.trace.scaleSamples + sample),
                        )
                    }
                }
            }
            if (nativeExecution) {
                reason = monitorNativeProfile(controller, profile, requireNotNull(weightCutoff), clock)
            } else {
                val oldStamp = controller.deviceState.value.telemetryTime
                session = controller.profiles.openLiveSession(profile.initialControl())
                val engine = ProfileProgramEngine(
                    profile,
                    finishWeightHandledExternally = profile.weightFinishHandledByBookoo(weightCutoff),
                )
                val initialTelemetry = controller.profiles.telemetry()
                val initial = engine.tick(
                    0,
                    initialTelemetry.copy(sampledAtMillis = initialTelemetry.values.keys.associateWith { 0L }),
                )
                val target = initial.target?.let(controller::quantize)
                val sent = target?.let {
                    withTimeout(TELEMETRY_TIMEOUT_MS.milliseconds) { requireNotNull(session).applyTarget(it) }
                }
                if (sent != null) {
                    mutableState.update { current ->
                        current.copy(trace = current.trace?.copy(targets = listOf(EmittedTarget(0, initial.phaseId, sent))))
                    }
                }
                withTimeout(TELEMETRY_TIMEOUT_MS.milliseconds) {
                    controller.deviceState.first {
                        it.telemetryTime != oldStamp && controller.profiles.isLiveSessionActive(it)
                    }
                }
                reason = initial.finish ?: controlLoop(controller, requireNotNull(session), profile, clock, engine, target, sent, weightCutoff)
            }
        } catch (error: CancellationException) {
            reason = error.message ?: "user_stop"
        } catch (error: Exception) {
            reason = error.message ?: "control_failure"
        } finally {
            withContext(NonCancellable) { scaleTelemetryJob?.cancelAndJoin() }
            var stopRequestedAt: Instant? = null
            val stopped = confirmStop(
                controller = controller,
                session = session,
                nativeExecution = nativeExecution,
                waitForNativeFinish = nativeExecution && reason == "profile_finish",
            ) { stopRequestedAt = Clock.System.now() }
            withContext(NonCancellable) { machineDiagnosticJob?.cancelAndJoin() }
            val finalWeight = weightCutoff?.let { currentWeight(controller, it)?.first }
            mutableState.update {
                it.copy(
                    active = false,
                    stopPending = !stopped,
                    message = reason,
                    trace = it.trace?.let { trace ->
                        trace.copy(
                            endReason = reason,
                            stopRequestedAt = stopRequestedAt ?: trace.stopRequestedAt,
                            stopConfirmedAt = Clock.System.now().takeIf { stopped },
                            finalScaleWeightGrams = finalWeight ?: trace.finalScaleWeightGrams,
                        )
                    },
                )
            }
            try {
                val completed = state.value
                completed.trace?.let { trace -> recorder.save(requireNotNull(completed.deviceId), trace) }
            } catch (error: Exception) {
                Logger.e(error) { "Could not save the app-controlled brew" }
            }
            if (!stopped) retryStop(controller, session, nativeExecution)
        }
    }

    private suspend fun monitorNativeProfile(
        controller: DeviceController,
        profile: BrewProfile,
        cutoff: AppWeightCutoff,
        clock: TimeSource.Monotonic.ValueTimeMark,
    ): String {
        val oldStamp = controller.deviceState.value.telemetryTime
        withTimeout(TELEMETRY_TIMEOUT_MS.milliseconds) {
            controller.deviceState.first {
                it.telemetryTime != oldStamp && it.brewStatus == DeviceState.BrewStatus.Profile
            }
        }
        var stamp: Instant? = null
        var freshAt = TimeSource.Monotonic.markNow()
        while (currentCoroutineContext().isActive) {
            val device = controller.deviceState.value
            if (device.connectionStatus != DeviceState.ConnectionStatus.Connected) {
                throw DeviceNotConnectedException(state.value.deviceId ?: 0L)
            }
            val telemetry = controller.profiles.telemetry()
            val cutoffReading = currentWeight(controller, cutoff)
            if (cutoffReading != null) {
                mutableState.update { current ->
                    current.copy(trace = current.trace?.copy(finalScaleWeightGrams = cutoffReading.first))
                }
                if (cutoffReading.first >= cutoff.targetGrams) return "app_weight_goal"
            }
            if (device.brewStatus == DeviceState.BrewStatus.Idle) {
                val machineFinishReached = !profile.weightFinishHandledByBookoo(cutoff) &&
                    profile.finishConditionReached(telemetry)
                return if (machineFinishReached) "profile_finish" else "machine_stop"
            }
            if (device.brewStatus != DeviceState.BrewStatus.Profile) throw MachineControlChangedException()

            if (device.telemetryTime != stamp) {
                stamp = device.telemetryTime
                freshAt = TimeSource.Monotonic.markNow()
                recordTelemetrySample(device, clock.elapsedNow().inWholeMilliseconds)
            }
            if (freshAt.elapsedNow().inWholeMilliseconds >= TELEMETRY_TIMEOUT_MS) throw TelemetryTimeoutException()

            if (!(profile.machineRequiredMetrics(cutoff) - BrewMetric.CupWeight).all { telemetry[it] != null }) {
                throw RequiredMeasurementLostException()
            }
            if (!profile.weightFinishHandledByBookoo(cutoff) && profile.finishConditionReached(telemetry)) {
                return "profile_finish"
            }
            delay(EVALUATION_INTERVAL_MS.milliseconds)
        }
        return "user_stop"
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
        weightCutoff: AppWeightCutoff?,
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
            controller.requireReady(currentDeviceId)

            if (device.brewStatus == DeviceState.BrewStatus.Idle) return "machine_stop"
            if (!controller.profiles.isLiveSessionActive(device)) {
                throw MachineControlChangedException()
            }

            // Check the selected cutoff sensor independently of machine telemetry freshness.
            // Its one-second timeout should not stretch to the longer machine telemetry timeout.
            val cutoffReading = weightCutoff?.let { cutoff -> currentWeight(controller, cutoff) }

            if (device.telemetryTime != stamp) {
                stamp = device.telemetryTime
                freshAt = TimeSource.Monotonic.markNow()
                val elapsed = clock.elapsedNow().inWholeMilliseconds
                recordTelemetrySample(device, elapsed)
            }
            if (freshAt.elapsedNow().inWholeMilliseconds >= TELEMETRY_TIMEOUT_MS) throw TelemetryTimeoutException()
            val baseTelemetry = controller.profiles.telemetry()
            val bookooWeight = if (weightCutoff?.source == WeightCutoffSource.Bookoo) cutoffReading?.first else null
            val telemetry = if (bookooWeight != null) {
                baseTelemetry.copy(
                    values = baseTelemetry.values + (BrewMetric.CupWeight to bookooWeight),
                )
            } else baseTelemetry
            if (!(profile.machineRequiredMetrics(weightCutoff) - BrewMetric.CupWeight).all { telemetry[it] != null }) {
                throw RequiredMeasurementLostException()
            }
            val elapsed = clock.elapsedNow().inWholeMilliseconds
            val sourceTime = elapsed - freshAt.elapsedNow().inWholeMilliseconds
            val stamped = telemetry.copy(
                sampledAtMillis = telemetry.values.keys.associateWith { sourceTime.coerceAtLeast(0) },
            )
            val output = engine.tick(elapsed, stamped)

            mutableState.update {
                it.copy(
                    trace = it.trace?.copy(
                        transitions = engine.transitions.toList(),
                        captures = engine.captureEvents.toList(),
                        decisions = engine.decisions.toList(),
                    ),
                )
            }
            if (weightCutoff != null && cutoffReading != null && cutoffReading.first >= weightCutoff.targetGrams) {
                return "app_weight_goal"
            }
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
    private suspend fun confirmStop(
        controller: DeviceController,
        session: LiveBrewSession?,
        nativeExecution: Boolean = false,
        waitForNativeFinish: Boolean = false,
        onStopRequest: () -> Unit = {},
    ): Boolean =
        withContext(NonCancellable) {
            val startedAt = state.value.trace?.startedAt

            try {
                withTimeout(STOP_TIMEOUT_MS.milliseconds) {
                    val device = controller.deviceState.value
                    check(device.connectionStatus == DeviceState.ConnectionStatus.Connected)
                    val confirmedIdle = device.brewStatus == DeviceState.BrewStatus.Idle &&
                        device.statusTime?.let { startedAt != null && it >= startedAt } == true
                    if (!confirmedIdle) {
                        if (!waitForNativeFinish) {
                            onStopRequest()
                            when {
                                session != null -> session.stop()
                                nativeExecution -> controller.profiles.stopProfileBrewing()
                                else -> controller.profiles.stopLiveSession()
                            }
                        }
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

    private fun retryStop(controller: DeviceController, session: LiveBrewSession?, nativeExecution: Boolean) {
        scope.launch {
            while (isActive && state.value.stopPending) {
                delay(STOP_RETRY_INTERVAL_MS.milliseconds)
                controller.deviceState.first { it.connectionStatus == DeviceState.ConnectionStatus.Connected }
                var stopRequestedAt: Instant? = null
                if (confirmStop(controller, session, nativeExecution) { stopRequestedAt = Clock.System.now() }) {
                    mutableState.update { current ->
                        current.copy(
                            stopPending = false,
                            trace = current.trace?.let { trace ->
                                trace.copy(
                                    stopRequestedAt = trace.stopRequestedAt ?: stopRequestedAt,
                                    stopConfirmedAt = Clock.System.now(),
                                    finalScaleWeightGrams = current.weightCutoff?.let { cutoff ->
                                        currentWeight(controller, cutoff)?.first
                                    } ?: trace.finalScaleWeightGrams,
                                )
                            },
                        )
                    }
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
                brewBoilerTempC = device.brewBoilerTemp,
                steamBoilerTempC = device.steamBoilerTemp,
                machineElapsedSeconds = device.time,
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

    private fun currentWeight(
        controller: DeviceController,
        cutoff: AppWeightCutoff,
    ): Pair<Float, Instant>? {
        val now = Clock.System.now()
        val sample = when (cutoff.source) {
            WeightCutoffSource.MachineScale -> controller.deviceState.value.let { machine ->
                if (machine.smartScale?.isConnected != true) null
                else machine.weight?.let { weight -> machine.telemetryTime?.let { weight to it } }
            }
            WeightCutoffSource.Bookoo -> if (!cutoff.bookooConnected()) null else {
                cutoff.bookooReading()?.let { it.frame.weightGrams to it.receivedAt }
            }
        } ?: return null
        return sample.takeIf { (now - it.second).inWholeMilliseconds in 0..CUTOFF_SCALE_STALE_MILLIS }
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

private fun BrewProfile.finishConditionReached(telemetry: BrewTelemetry): Boolean = when (val finish = finishCondition) {
    is Condition.Weight -> telemetry[BrewMetric.CupWeight]?.let { it >= finish.target } == true
    is Condition.Volume -> telemetry[BrewMetric.PumpedVolume]?.let { it >= finish.target } == true
    null -> false
}

private fun BrewProfile.weightFinishHandledByBookoo(cutoff: AppWeightCutoff?): Boolean =
    cutoff?.source == WeightCutoffSource.Bookoo &&
        (finishCondition as? Condition.Weight) != null &&
        BrewMetric.CupWeight !in copy(finishCondition = null).requiredMetrics()

private fun BrewProfile.machineRequiredMetrics(cutoff: AppWeightCutoff?): Set<BrewMetric> =
    if (weightFinishHandledByBookoo(cutoff)) {
        requiredMetrics() - BrewMetric.CupWeight
    } else {
        requiredMetrics()
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
private const val CUTOFF_SCALE_STALE_MILLIS = 1_000L
