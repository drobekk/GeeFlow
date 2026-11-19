package app.geeflow.domain.device

import app.geeflow.data.brew.BrewHistoryRepository
import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewHistoryEntry
import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.BrewPhase
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.ExitCondition
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.ProfileExecutionTrace
import app.geeflow.data.brew.model.ProfileMachineDiagnostic
import app.geeflow.data.brew.model.PhaseRamp
import app.geeflow.data.brew.model.RampStyle
import app.geeflow.data.brew.model.ThresholdComparison
import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.BookooScalePacketEvent
import app.geeflow.data.device.MachineDiagnosticEvent
import app.geeflow.data.device.MachineDiagnosticEventSource
import app.geeflow.data.device.BookooScaleState
import app.geeflow.data.device.ScaleReadingSource
import app.geeflow.data.device.DeviceProfileControl
import app.geeflow.data.device.LiveBrewSession
import app.geeflow.data.device.BookooScaleReading
import app.geeflow.data.device.impl.controller.DemoDeviceController
import app.geeflow.data.device.model.DeviceError
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.SmartScale
import app.geeflow.data.device.model.BookooScaleFrame
import app.geeflow.data.device.model.pumpTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

class ProfileExecutionCoordinatorTest {
    @Test
    fun `diagnostics are saved when a profile ends before the first sensor sample`() {
        val fixture = Fixture()
        try {
            val frame = ProfileMachineDiagnostic(Clock.System.now(), "modbus_response_timeout", mapOf("timeoutMs" to "1000"))
            fixture.recorder.save(1, ProfileExecutionTrace(profile = fixture.profile, startedAt = Clock.System.now(), machineDiagnostics = listOf(frame)))
            assertEquals(frame, fixture.saved.value.single().executionTrace!!.machineDiagnostics.single())
        } finally {
            fixture.scope.cancel()
        }
    }

    @Test
    fun `profile records raw machine frames in saved history`() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.coordinator.start(1, fixture.profile)
            withTimeout(4000) { fixture.writes.first { it.isNotEmpty() } }
            val event = MachineDiagnosticEvent("modbus_response_frame", Clock.System.now(), mapOf("hex" to "0102", "crcValid" to "false"))
            fixture.machineDiagnostics.emit(event)
            withTimeout(4000) { fixture.coordinator.state.first { it.trace?.machineDiagnostics?.isNotEmpty() == true } }
            withTimeout(4000) { fixture.coordinator.state.first { it.trace?.measurements?.isNotEmpty() == true } }
            fixture.coordinator.stop(1)
            withTimeout(4000) { fixture.saved.first { it.isNotEmpty() } }
            val saved = fixture.saved.value.single().executionTrace!!.machineDiagnostics.single()
            assertEquals("modbus_response_frame", saved.type)
            assertEquals("false", saved.fields["crcValid"])
            assertEquals("0102", saved.fields["hex"])
        } finally {
            fixture.scope.cancel()
        }
    }

    @Test
    fun `profile diagnostics survive history serialization and old traces remain readable`() {
        val fixture = Fixture()
        try {
            val original = ProfileExecutionTrace(profile = fixture.profile)
            val oldJson = Json.encodeToString(original)
            val event = ProfileMachineDiagnostic(Clock.System.now(), "modbus_tx", mapOf("hex" to "010605b3000078e1"))
            val newJson = Json.encodeToString(original.copy(machineDiagnostics = listOf(event)))
            assertTrue(Json.decodeFromString<ProfileExecutionTrace>(oldJson).machineDiagnostics.isEmpty())
            assertEquals(event, Json.decodeFromString<ProfileExecutionTrace>(newJson).machineDiagnostics.single())
        } finally {
            fixture.scope.cancel()
        }
    }

    @Test
    fun `profile saves received scale packets and final weight`() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.coordinator.start(1, fixture.profile)
            withTimeout(4000) { fixture.writes.first { it.isNotEmpty() } }
            fixture.scalePackets.emit(BookooScalePacketEvent(
                receivedAt = Clock.System.now(), packetHex = "030B00", decoded = true,
                frame = BookooScaleFrame(300, 12.3f, 0.5f, 91),
            ))
            withTimeout(4000) { fixture.coordinator.state.first { it.trace?.scaleSamples?.isNotEmpty() == true } }
            fixture.coordinator.stop(1)
            withTimeout(4000) { fixture.saved.first { it.isNotEmpty() } }
            val saved = fixture.saved.value.single()
            assertEquals("030B00", saved.scaleSamples.single().rawPacketHex)
            assertEquals(12.3f, saved.scaleSamples.single().weightGrams)
            assertEquals(12.3f, saved.finalWeightGrams)
        } finally {
            fixture.scope.cancel()
        }
    }

    @Test
    fun `when a fault occurs during live profile then stops the session`() = runBlocking {
        val fixture = Fixture(continuous = true)

        try {
            fixture.coordinator.start(1, fixture.profile)
            withTimeout(4000) { fixture.writes.first { it.isNotEmpty() } }
            fixture.device.update { it.copy(error = DeviceError(8)) }
            withTimeout(4000) { fixture.coordinator.state.first { !it.active } }

            assertEquals(1, fixture.stops)
            assertEquals(DeviceState.BrewStatus.Idle, fixture.device.value.brewStatus)
            assertTrue(fixture.coordinator.state.value.message.orEmpty().contains("reports an active error: 8"))
        } finally {
            fixture.scope.cancel()
        }
    }

    @Test
    fun `app yield cutoff replaces a volume finish with the selected weight target`() = runBlocking {
        val fixture = Fixture()
        val profile = fixture.profile.copy(finishCondition = Condition.Volume(60f))
        fixture.device.update {
            it.copy(
                smartScale = SmartScale("Machine scale", true),
                weight = 0f,
                telemetryTime = Clock.System.now(),
            )
        }
        val cutoff = AppWeightCutoff(
            targetGrams = 30f,
            source = WeightCutoffSource.MachineScale,
            bookooConnected = { false },
            bookooReading = { null },
        )

        try {
            fixture.coordinator.start(1, profile, cutoff)
            delay(100)
            fixture.device.update { it.copy(weight = 30f, telemetryTime = Clock.System.now()) }
            withTimeout(4_000) { fixture.coordinator.state.first { !it.active } }

            assertEquals("app_weight_goal", fixture.coordinator.state.value.trace?.endReason)
            assertEquals(Condition.Weight(30f), fixture.coordinator.state.value.trace?.profile?.finishCondition)
            assertEquals(30f, fixture.coordinator.state.value.trace?.finalScaleWeightGrams)
            assertEquals(1, fixture.stops)
        } finally {
            fixture.scope.cancel()
        }
    }

    @Test
    fun `app weight cutoff wins when volume and cup yield qualify together`() = runBlocking {
        val fixture = Fixture()
        val profile = fixture.profile.copy(finishCondition = Condition.Volume(60f))
        fixture.device.update {
            it.copy(
                smartScale = SmartScale("Machine scale", true),
                weight = 0f,
                telemetryTime = Clock.System.now(),
            )
        }
        val cutoff = AppWeightCutoff(30f, WeightCutoffSource.MachineScale, { false }, { null })

        try {
            fixture.coordinator.start(1, profile, cutoff)
            delay(100)
            fixture.device.update {
                it.copy(weight = 30f, volume = 60f, telemetryTime = Clock.System.now())
            }
            withTimeout(4_000) { fixture.coordinator.state.first { !it.active } }

            assertEquals("app_weight_goal", fixture.coordinator.state.value.trace?.endReason)
            assertEquals(Condition.Weight(30f), fixture.coordinator.state.value.trace?.profile?.finishCondition)
            assertEquals(1, fixture.stops)
        } finally {
            fixture.scope.cancel()
        }
    }

    @Test
    fun `stale selected scale falls back to the programmed recipe timing`() = runBlocking {
        val fixture = Fixture()
        val reading = BookooScaleReading(
            BookooScaleFrame(1_000, 0f, 0f, 100),
            Clock.System.now() - 1_500.milliseconds,
            "",
        )
        val cutoff = AppWeightCutoff(
            30f,
            WeightCutoffSource.Bookoo,
            bookooConnected = { true },
            bookooReading = { reading },
        )

        try {
            fixture.coordinator.start(1, fixture.profile.copy(finishCondition = Condition.Volume(60f)), cutoff)
            withTimeout(4_000) { fixture.coordinator.state.first { !it.active } }

            assertEquals("program_complete", fixture.coordinator.state.value.trace?.endReason)
            assertEquals(1, fixture.stops)
        } finally {
            fixture.scope.cancel()
        }
    }

    @Test
    fun `manual stop racing the cup cutoff sends one stop and retains final scale weight`() = runBlocking {
        val fixture = Fixture()
        val profile = fixture.profile.copy(finishCondition = Condition.Volume(60f))
        fixture.device.update {
            it.copy(
                smartScale = SmartScale("Machine scale", true),
                weight = 0f,
                telemetryTime = Clock.System.now(),
            )
        }
        val cutoff = AppWeightCutoff(30f, WeightCutoffSource.MachineScale, { false }, { null })

        try {
            fixture.coordinator.start(1, profile, cutoff)
            withTimeout(4_000) { fixture.writes.first { it.isNotEmpty() } }
            fixture.device.update { it.copy(weight = 30f, telemetryTime = Clock.System.now()) }
            val manualStop = async { fixture.coordinator.stop(1) }
            withTimeout(4_000) { manualStop.await() }
            withTimeout(4_000) { fixture.coordinator.state.first { !it.active && !it.stopPending } }

            assertEquals(1, fixture.stops)
            assertTrue(fixture.coordinator.state.value.trace?.finalScaleWeightGrams == 30f)
            assertTrue(fixture.coordinator.state.value.trace?.stopRequestedAt != null)
        } finally {
            fixture.scope.cancel()
        }
    }

    private class Fixture(val continuous: Boolean = false) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val device = MutableStateFlow(
            DeviceState(
                connectionStatus = DeviceState.ConnectionStatus.Connected,
                pressure = 0f,
                flowRate = 0f,
                volume = 0f
            )
        )
        var failWrite = false
        var failStart = false
        var failNativeStart = false
        var stopFailures = 0
        var telemetryDelayMillis = 0L
        var stops = 0
        val writes = MutableStateFlow<List<PhaseControl>>(emptyList())
        val saved = MutableStateFlow<List<BrewHistoryEntry>>(emptyList())
        val scalePackets = MutableSharedFlow<BookooScalePacketEvent>(extraBufferCapacity = 32)
        val scale = object : ScaleReadingSource {
            override val state = MutableStateFlow(BookooScaleState())
            override val packets = scalePackets
        }
        var nativeStarts = 0
        val recorder = ProfileExecutionRecorder(object : BrewHistoryRepository {
            override fun addBrew(entry: BrewHistoryEntry, dataPoints: Map<Float, BrewDataPoint>) {
                saved.update { it + entry }
            }
            override fun getBrews(userId: Long, query: String, limit: Int, offset: Int) = saved.value
            override fun getBrewDataPoints(id: Long): Map<Float, BrewDataPoint> = emptyMap()
            override fun deleteBrew(id: Long) = Unit
        })
        val machineDiagnostics = MutableSharedFlow<MachineDiagnosticEvent>(extraBufferCapacity = 32)
        val controller: DeviceController = object : DeviceController by DemoDeviceController(scope), MachineDiagnosticEventSource {
            override val diagnosticEvents = machineDiagnostics
            override val deviceState = device
            override val profiles = object : DeviceProfileControl by DemoDeviceController(scope).profiles {
                override fun telemetry() = device.value.pumpTelemetry()
                override suspend fun startProfileBrewing(profile: BrewProfile) {
                    nativeStarts++
                    val startedAt = Clock.System.now()
                    device.update {
                        it.copy(
                            statusTime = startedAt,
                            telemetryTime = startedAt,
                            brewStatus = DeviceState.BrewStatus.Profile,
                        )
                    }
                    if (failNativeStart) error("start_acknowledgement_lost")
                    scope.launch {
                        while (device.value.brewStatus == DeviceState.BrewStatus.Profile) {
                            delay(50)
                            device.update { it.copy(telemetryTime = Clock.System.now()) }
                        }
                    }
                }
                override suspend fun stopProfileBrewing() {
                    stops++
                    device.update {
                        it.copy(statusTime = Clock.System.now(), brewStatus = DeviceState.BrewStatus.Idle)
                    }
                }
                override suspend fun stopLiveSession() {
                    stops++
                    if (stopFailures-- > 0) error("stop_failed")
                    device.update {
                        it.copy(statusTime = Clock.System.now(), brewStatus = DeviceState.BrewStatus.Idle)
                    }
                }
                override suspend fun openLiveSession(initial: PhaseControl): LiveBrewSession {
                    device.update {
                        it.copy(
                            statusTime = Clock.System.now(),
                            brewStatus = DeviceState.BrewStatus.FreeVariable,
                            telemetryTime = if (telemetryDelayMillis == 0L) Clock.System.now() else null
                        )
                    }
                    if (failStart) error("start_acknowledgement_lost")
                    scope.launch {
                        delay(telemetryDelayMillis)
                        while (device.value.brewStatus != DeviceState.BrewStatus.Idle) {
                            delay(50)
                            device.update { it.copy(telemetryTime = Clock.System.now()) }
                        }
                    }
                    return object : LiveBrewSession {
                        override val requiresContinuousUpdates = continuous
                        override suspend fun applyTarget(target: PhaseControl): PhaseControl {
                            if (failWrite) error("write_failed")
                            delay(10)
                            writes.update { it + target }
                            return if (continuous) PhaseControl.Pressure(bar = writes.value.size.toFloat()) else target
                        }
                        override suspend fun stop() {
                            stopLiveSession()
                        }
                    }
                }
            }
        }
        val coordinator = ProfileExecutionCoordinator(
            object : DeviceControllerProvider {
                override fun observeController(deviceId: Long) = flowOf(getController(deviceId))
                override val currentDeviceId = MutableStateFlow<Long?>(1L)
                override fun getController(deviceId: Long) = controller
                override suspend fun disconnectCurrent() = Unit
            },
            scale,
            scope,
            recorder,
        )
        val profile = BrewProfile(
            userId = 1,
            name = "App",
            description = "",
            program = BrewProgram.Phases(
                listOf(
                    BrewPhase(
                        "phase",
                        control = PhaseControl.Pressure(6.24f),
                        maximumDurationMillis = 300,
                        exitConditions = listOf(ExitCondition(BrewMetric.PumpedVolume, ThresholdComparison.Above, 100f))
                    )
                )
            )
        )
    }

    @Test
    fun `native volume profile keeps firmware execution and app weight cutoff stops it`() = runBlocking {
        val fixture = Fixture()
        val profile = BrewProfile(
            userId = 1,
            name = "Native volume",
            description = "",
            program = BrewProgram.Phases(
                listOf(BrewPhase("phase", control = PhaseControl.Pressure(6f), maximumDurationMillis = 5_000)),
            ),
            finishCondition = Condition.Volume(60f),
        )
        fixture.device.update {
            it.copy(smartScale = SmartScale("Machine scale", true), weight = 0f, telemetryTime = Clock.System.now())
        }
        val cutoff = AppWeightCutoff(30f, WeightCutoffSource.MachineScale, { false }, { null })

        try {
            fixture.coordinator.start(1, profile, cutoff)
            delay(100)
            fixture.device.update { it.copy(weight = 30f, telemetryTime = Clock.System.now()) }
            withTimeout(4_000) { fixture.coordinator.state.first { !it.active && !it.stopPending } }

            assertEquals(1, fixture.nativeStarts)
            assertEquals(1, fixture.stops)
            assertTrue(fixture.writes.value.isEmpty())
            assertEquals("app_weight_goal", fixture.coordinator.state.value.trace?.endReason)
        } finally {
            fixture.scope.cancel()
        }
    }

    @Test
    fun `BOOKOO cutoff stops a weight recipe before its own finish without a machine scale`() = runBlocking {
        val fixture = Fixture()
        var scaleWeight = 0f
        var scaleAt = Clock.System.now()
        val profile = BrewProfile(
            userId = 1,
            name = "Native weight",
            description = "",
            program = BrewProgram.Phases(
                listOf(BrewPhase("phase", control = PhaseControl.Pressure(6f), maximumDurationMillis = 5_000)),
            ),
            finishCondition = Condition.Weight(60f),
        )
        val cutoff = AppWeightCutoff(
            targetGrams = 30f,
            source = WeightCutoffSource.Bookoo,
            bookooConnected = { true },
            bookooReading = {
                BookooScaleReading(
                    BookooScaleFrame(1_000, scaleWeight, 0f, 100),
                    scaleAt,
                    "",
                )
            },
        )

        try {
            fixture.coordinator.start(1, profile, cutoff)
            delay(100)
            scaleWeight = 30f
            scaleAt = Clock.System.now()
            withTimeout(4_000) { fixture.coordinator.state.first { !it.active && !it.stopPending } }

            assertEquals(1, fixture.nativeStarts)
            assertEquals(1, fixture.stops)
            assertEquals("app_weight_goal", fixture.coordinator.state.value.trace?.endReason)
            assertEquals(30f, fixture.coordinator.state.value.trace?.finalScaleWeightGrams)
        } finally {
            fixture.scope.cancel()
        }
    }

    @Test
    fun `unexpected native stop below the weight target is not recorded as a successful finish`() = runBlocking {
        val fixture = Fixture()
        val profile = BrewProfile(
            userId = 1,
            name = "Native volume",
            description = "",
            program = BrewProgram.Phases(
                listOf(BrewPhase("phase", control = PhaseControl.Pressure(6f), maximumDurationMillis = 5_000)),
            ),
            finishCondition = Condition.Volume(60f),
        )
        fixture.device.update {
            it.copy(smartScale = SmartScale("Machine scale", true), weight = 0f, telemetryTime = Clock.System.now())
        }
        val cutoff = AppWeightCutoff(30f, WeightCutoffSource.MachineScale, { false }, { null })

        try {
            fixture.coordinator.start(1, profile, cutoff)
            delay(100)
            fixture.device.update {
                it.copy(
                    weight = 29f,
                    volume = 60f,
                    telemetryTime = Clock.System.now(),
                    statusTime = Clock.System.now(),
                    brewStatus = DeviceState.BrewStatus.Idle,
                )
            }
            withTimeout(4_000) { fixture.coordinator.state.first { !it.active && !it.stopPending } }

            assertEquals(1, fixture.nativeStarts)
            assertEquals(0, fixture.stops)
            assertEquals("machine_stop", fixture.coordinator.state.value.trace?.endReason)
        } finally {
            fixture.scope.cancel()
        }
    }

    @Test
    fun `native cutoff disconnect requests a stop and confirms after reconnect`() = runBlocking {
        val fixture = Fixture()
        val profile = BrewProfile(
            userId = 1,
            name = "Native volume",
            description = "",
            program = BrewProgram.Phases(
                listOf(BrewPhase("phase", control = PhaseControl.Pressure(6f), maximumDurationMillis = 5_000)),
            ),
            finishCondition = Condition.Volume(60f),
        )
        fixture.device.update {
            it.copy(smartScale = SmartScale("Machine scale", true), weight = 0f, telemetryTime = Clock.System.now())
        }
        val cutoff = AppWeightCutoff(30f, WeightCutoffSource.MachineScale, { false }, { null })

        try {
            fixture.coordinator.start(1, profile, cutoff)
            delay(100)
            fixture.device.update { it.copy(connectionStatus = DeviceState.ConnectionStatus.Disconnected) }
            withTimeout(4_000) { fixture.coordinator.state.first { !it.active && it.stopPending } }
            assertEquals(0, fixture.stops)

            fixture.device.update {
                it.copy(connectionStatus = DeviceState.ConnectionStatus.Connected, telemetryTime = Clock.System.now())
            }
            withTimeout(4_000) { fixture.coordinator.state.first { !it.stopPending } }

            assertEquals(1, fixture.stops)
            assertTrue(fixture.coordinator.state.value.trace?.stopRequestedAt != null)
            assertTrue(fixture.coordinator.state.value.trace?.stopConfirmedAt != null)
        } finally {
            fixture.scope.cancel()
        }
    }

    @Test
    fun `native start acknowledgement failure still confirms or tracks a stop`() = runBlocking {
        val fixture = Fixture()
        fixture.failNativeStart = true
        val profile = BrewProfile(
            userId = 1,
            name = "Native volume",
            description = "",
            program = BrewProgram.Phases(
                listOf(BrewPhase("phase", control = PhaseControl.Pressure(6f), maximumDurationMillis = 5_000)),
            ),
            finishCondition = Condition.Volume(60f),
        )
        fixture.device.update {
            it.copy(smartScale = SmartScale("Machine scale", true), weight = 0f, telemetryTime = Clock.System.now())
        }
        val cutoff = AppWeightCutoff(30f, WeightCutoffSource.MachineScale, { false }, { null })

        try {
            val failure = runCatching { fixture.coordinator.start(1, profile, cutoff) }.exceptionOrNull()

            assertEquals("start_acknowledgement_lost", failure?.message)
            assertEquals(1, fixture.nativeStarts)
            assertEquals(1, fixture.stops)
            assertFalse(fixture.coordinator.state.value.active)
            assertFalse(fixture.coordinator.state.value.stopPending)
            assertEquals("start_failure", fixture.coordinator.state.value.trace?.endReason)
        } finally {
            fixture.scope.cancel()
        }
    }

    @Test fun completionSendsQuantizedTargetAndStopsExactlyOnce() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.coordinator.start(1, fixture.profile)
            withTimeout(4000) { fixture.coordinator.state.first { !it.active } }
            assertEquals(listOf(PhaseControl.Pressure(6.2f)), fixture.writes.value)
            assertEquals(1, fixture.stops)
            assertFalse(fixture.coordinator.state.value.stopPending)
            assertEquals(1, fixture.coordinator.state.value.trace?.transitions?.size)
            withTimeout(4000) { fixture.saved.first { it.isNotEmpty() } }
            assertEquals("App", fixture.saved.value.single().profileName)
            assertEquals(1L, fixture.saved.value.single().deviceId)
        } finally {
            fixture.scope.cancel()
        }
    }

    @Test
    fun feedbackSessionReceivesConstantTargetsAndRecordsActualCommands() = runBlocking {
        val fixture = Fixture(continuous = true)
        val program = fixture.profile.program as BrewProgram.Phases
        val profile = fixture.profile.copy(
            program = BrewProgram.Phases(program.phases.map { it.copy(maximumDurationMillis = 900) }),
        )
        try {
            fixture.coordinator.start(1, profile)
            withTimeout(4000) { fixture.coordinator.state.first { !it.active } }
            assertTrue(fixture.writes.value.size >= 2)
            assertTrue(fixture.writes.value.all { it == PhaseControl.Pressure(6.2f) })
            assertEquals(PhaseControl.Pressure(1f), fixture.coordinator.state.value.trace?.targets?.first()?.target)
            assertEquals(1, fixture.stops)
        } finally {
            fixture.scope.cancel()
        }
    }

    @Test fun backgroundCancelsCommandsAndStops() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.coordinator.start(1, fixture.profile)
            withTimeout(4000) { fixture.writes.first { it.isNotEmpty() } }
            fixture.coordinator.setForeground(false)
            withTimeout(4000) { fixture.coordinator.state.first { !it.active } }
            assertEquals(1, fixture.stops)
            assertEquals(1, fixture.writes.value.size)
        } finally {
            fixture.scope.cancel()
        }
    }

    @Test fun failedWriteStillStops() = runBlocking {
        val fixture = Fixture()
        fixture.failWrite = true
        try {
            fixture.coordinator.start(1, fixture.profile)
            withTimeout(4000) { fixture.coordinator.state.first { !it.active } }
            assertEquals(1, fixture.stops)
            assertEquals("write_failed", fixture.coordinator.state.value.trace?.endReason)
        } finally {
            fixture.scope.cancel()
        }
    }

    @Test
    fun initialPressureIsSentBeforeTelemetryConfirmsStart() = runBlocking {
        val fixture = Fixture()
        fixture.telemetryDelayMillis = 1000L
        try {
            fixture.coordinator.start(1, fixture.profile)
            withTimeout(500) { fixture.writes.first { it.isNotEmpty() } }
            assertEquals(PhaseControl.Pressure(6.2f), fixture.writes.value.first())
            assertEquals(null, fixture.device.value.telemetryTime)
            fixture.coordinator.stop(1)
            assertEquals(1, fixture.stops)
        } finally {
            fixture.scope.cancel()
        }
    }

    @Test
    fun initialRampDoesNotJumpStraightToFinalPressure() = runBlocking {
        val fixture = Fixture()
        fixture.telemetryDelayMillis = 1000L
        val program = fixture.profile.program as BrewProgram.Phases
        val profile = fixture.profile.copy(
            program = BrewProgram.Phases(
                program.phases.map {
                    it.copy(
                        maximumDurationMillis = 5000,
                        ramp = PhaseRamp(style = RampStyle.Linear, durationMillis = 2000),
                    )
                },
            ),
        )
        try {
            fixture.coordinator.start(1, profile)
            withTimeout(500) { fixture.writes.first { it.isNotEmpty() } }
            assertEquals(PhaseControl.Pressure(0f), fixture.writes.value.first())
            fixture.coordinator.stop(1)
            assertEquals(1, fixture.stops)
        } finally {
            fixture.scope.cancel()
        }
    }

    @Test
    fun partialStartStillStopsWithoutALiveSession() = runBlocking {
        val fixture = Fixture()
        fixture.failStart = true
        try {
            fixture.coordinator.start(1, fixture.profile)
            withTimeout(4000) { fixture.coordinator.state.first { !it.active } }
            assertEquals(1, fixture.stops)
            assertFalse(fixture.coordinator.state.value.stopPending)
            assertEquals(DeviceState.BrewStatus.Idle, fixture.device.value.brewStatus)
        } finally {
            fixture.scope.cancel()
        }
    }

    @Test
    fun failedStopIsRetriedWithoutSendingMoreTargets() = runBlocking {
        val fixture = Fixture()
        fixture.stopFailures = 2
        try {
            fixture.coordinator.start(1, fixture.profile)
            withTimeout(4000) { fixture.coordinator.state.first { it.stopPending } }
            val targets = fixture.writes.value
            withTimeout(4000) { fixture.coordinator.state.first { !it.active && !it.stopPending } }
            assertEquals(3, fixture.stops)
            assertEquals(targets, fixture.writes.value)
        } finally {
            fixture.scope.cancel()
        }
    }

    @Test
    fun reconnectWithRunningPumpAutomaticallyStops() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.coordinator.start(1, fixture.profile)
            withTimeout(4000) { fixture.writes.first { it.isNotEmpty() } }
            fixture.device.update { it.copy(connectionStatus = DeviceState.ConnectionStatus.Disconnected) }
            withTimeout(4000) { fixture.coordinator.state.first { it.stopPending } }
            val targets = fixture.writes.value
            fixture.device.update { it.copy(connectionStatus = DeviceState.ConnectionStatus.Connected) }
            withTimeout(4000) { fixture.coordinator.state.first { !it.stopPending } }
            assertEquals(1, fixture.stops)
            assertEquals(targets, fixture.writes.value)
            assertEquals(DeviceState.BrewStatus.Idle, fixture.device.value.brewStatus)
        } finally {
            fixture.scope.cancel()
        }
    }

    @Test fun disconnectKeepsStopPendingAndReconnectNeverResumes() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.coordinator.start(1, fixture.profile)
            withTimeout(4000) { fixture.writes.first { it.isNotEmpty() } }
            fixture.device.update { it.copy(connectionStatus = DeviceState.ConnectionStatus.Disconnected) }
            withTimeout(4000) { fixture.coordinator.state.first { !it.active } }
            assertTrue(fixture.coordinator.state.value.stopPending)
            assertEquals(0, fixture.stops)
            fixture.device.update {
                it.copy(
                    connectionStatus = DeviceState.ConnectionStatus.Connected,
                    statusTime = Clock.System.now(),
                    brewStatus = DeviceState.BrewStatus.Idle
                )
            }
            withTimeout(4000) { fixture.coordinator.state.first { !it.stopPending } }
            assertFalse(fixture.coordinator.state.value.active)
            assertEquals(1, fixture.writes.value.size)
        } finally {
            fixture.scope.cancel()
        }
    }
}
