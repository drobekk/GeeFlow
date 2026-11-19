package app.geeflow.domain.device.recipe

import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.PressureLocation
import app.geeflow.data.device.*
import app.geeflow.data.device.model.*
import app.geeflow.domain.device.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.TimeSource
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class LiveRecipeCoordinatorTest {
    private class Scale : ScaleReadingSource {
        var origin = TimeSource.Monotonic.markNow() - 700.milliseconds
        override val state = MutableStateFlow(BookooScaleState(connected = true, reading = BookooScaleReading(BookooScaleFrame(1_000, 0f, 0f, 100), Clock.System.now(), "", origin)))
        override val packets = MutableSharedFlow<BookooScalePacketEvent>()
        override val readings = MutableSharedFlow<BookooScaleReading>(extraBufferCapacity = 50)
        suspend fun send(index: Int) {
            val reading = BookooScaleReading(BookooScaleFrame(1_000 + index * 100L, index / 10f, 1f, 100), Clock.System.now(), "", origin + (index * 100).milliseconds)
            state.value = state.value.copy(reading = reading)
            readings.emit(reading)
        }
    }
    private class Fixture(scope: CoroutineScope, val confirmsStop: Boolean = true, temperature: Int? = null) {
        val device = MutableStateFlow(DeviceState(connectionStatus = DeviceState.ConnectionStatus.Connected, brewStatus = DeviceState.BrewStatus.Idle,
            telemetryTime = Clock.System.now(), statusTime = Clock.System.now(), pressure = 0f, flowRate = 0f, volume = 0f))
        val provider = mockk<DeviceControllerProvider>()
        val controller = mockk<DeviceController>(relaxed = true)
        val profiles = mockk<DeviceProfileControl>(relaxed = true)
        val session = mockk<LiveBrewSession>()
        val execution = mockk<ProfileExecutionCoordinator>(relaxUnitFun = true)
        val scale = Scale()
        val commandStarted = CompletableDeferred<Unit>()
        val releaseCommand = CompletableDeferred<Unit>()
        val coordinator = LiveBrewTestCoordinator(provider, scale, execution, scope)
        val document = LiveRecipeDocument(id = "coordinator", name = "coordinator", phases = listOf(RecipePhase("pressure", target = RecipeValue(value = 4f), maximumDurationMillis = 10_000)), settings = RecipeShotSettings(temperatureC = temperature))
        init {
            every { provider.getController(1) } returns controller
            every { controller.deviceState } returns device
            every { controller.profiles } returns profiles
            every { controller.profilingCapabilities } returns ProfilingCapabilities(livePressure = mapOf(PressureLocation.Pump to TargetRange(0f, 12f, .1f)), minimumWriteIntervalMillis = 100)
            every { profiles.isLiveSessionActive(any()) } answers { firstArg<DeviceState>().brewStatus == DeviceState.BrewStatus.FreeVariable }
            every { session.requiresContinuousUpdates } returns false
            coEvery { profiles.openLiveSession(any()) } coAnswers {
                device.value = device.value.copy(brewStatus = DeviceState.BrewStatus.FreeVariable, telemetryTime = Clock.System.now())
                session
            }
            coEvery { session.applyTarget(any()) } coAnswers {
                commandStarted.complete(Unit)
                releaseCommand.await()
                firstArg<PhaseControl>()
            }
            coEvery { session.stop() } coAnswers {
                if (confirmsStop) device.value = device.value.copy(brewStatus = DeviceState.BrewStatus.Idle, statusTime = Clock.System.now())
            }
        }
        suspend fun start(scope: CoroutineScope) {
            device.value = device.value.copy(telemetryTime = Clock.System.now(), statusTime = Clock.System.now())
            scale.origin = TimeSource.Monotonic.markNow() - 700.milliseconds
            scale.state.value = scale.state.value.copy(reading = scale.state.value.reading!!.copy(receivedAt = Clock.System.now(), monotonicMark = scale.origin))
            coordinator.startLive(1, LiveTestRecipe.fromDocument(document), document.controlSettings(), scope)
        }
    }
    @Test fun scaleNotificationsAreAllProcessedWhileMachineAcknowledgementIsBlocked() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.start(backgroundScope)
        runCurrent()
        assertTrue(fixture.commandStarted.isCompleted)
        repeat(6) { fixture.scale.send(it + 1); runCurrent() }
        val events = fixture.coordinator.state.value.captureEvents.filter { it.type == "scale_measurement" }
        assertEquals(6, events.size)
        assertEquals(1f, events.last().fields.getValue("cup_flow_g_per_s").toFloat(), .01f)
        val stopping = backgroundScope.async { fixture.coordinator.stop(1) }
        runCurrent()
        assertTrue(stopping.await())
        assertFalse(fixture.coordinator.state.value.active)
        assertFalse(fixture.coordinator.state.value.stopPending)
        coVerify(exactly = 1) { fixture.session.stop() }
        coVerify(exactly = 1) { fixture.session.applyTarget(any()) }
        coVerify(exactly = 1) { fixture.execution.endExternalControl(1) }
    }
    @Test fun passiveCaptureStartsWithConnectedScaleBeforeAnyPacketCanBeDecoded() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.scale.state.value = BookooScaleState(
            connected = true,
            device = BookooScaleDevice("bookoo", "BOOKOO"),
            lastMessage = "Ignored invalid or unsupported scale packet",
        )

        fixture.coordinator.startMonitoring(
            1,
            LiveTestMode.PassiveCapture,
            LiveTestRecipe.fromDocument(fixture.document),
            fixture.document.controlSettings(),
        )
        runCurrent()
        assertTrue(fixture.coordinator.state.value.active)

        fixture.scale.packets.emit(BookooScalePacketEvent(Clock.System.now(), "030B...", decoded = false))
        runCurrent()
        val packet = fixture.coordinator.state.value.captureEvents.first { it.type == "scale_packet" }
        assertEquals("false", packet.fields["decoded"])
        assertEquals("030B...", packet.fields["packet_hex"])

        fixture.coordinator.stop(1)
        runCurrent()
        assertFalse(fixture.coordinator.state.value.active)
    }
    @Test fun cancellationKeepsOwnershipUntilFreshIdleStatusConfirmsStop() = runTest {
        val fixture = Fixture(backgroundScope, confirmsStop = false)
        fixture.start(backgroundScope); runCurrent()
        val stopping = backgroundScope.async { fixture.coordinator.stop(1) }
        runCurrent(); advanceTimeBy(3_001); runCurrent()
        assertTrue(stopping.await())
        assertTrue(fixture.coordinator.state.value.stopPending)
        coVerify(exactly = 0) { fixture.execution.endExternalControl(1) }
        fixture.device.value = fixture.device.value.copy(brewStatus = DeviceState.BrewStatus.Idle, statusTime = Clock.System.now() - 10.seconds)
        advanceTimeBy(501); runCurrent()
        assertTrue(fixture.coordinator.state.value.stopPending)
        fixture.device.value = fixture.device.value.copy(statusTime = Clock.System.now())
        runCurrent()
        assertFalse(fixture.coordinator.state.value.stopPending)
        coVerify(exactly = 1) { fixture.execution.endExternalControl(1) }
        coVerify(exactly = 1) { fixture.session.applyTarget(any()) }
    }
    @Test fun recipeTemperatureIsWrittenOnceBeforeActivationAndSnapshotIsRetained() = runTest {
        val fixture = Fixture(backgroundScope, temperature = 88)
        fixture.start(backgroundScope); runCurrent()
        coVerifyOrder { fixture.controller.settings.setBrewTemperature(88); fixture.profiles.openLiveSession(any()) }
        assertEquals(LiveRecipeJson.encode(fixture.document), fixture.coordinator.state.value.recipeSnapshot)
        val stop = backgroundScope.async { fixture.coordinator.stop(1) }; runCurrent(); stop.await()
        coVerify(exactly = 1) { fixture.controller.settings.setBrewTemperature(88) }
        coVerify(exactly = 1) { fixture.session.applyTarget(match { it is PhaseControl.Pressure }) }
    }
    @Test fun commandFailureImmediatelyEntersConfirmedStopHandling() = runTest {
        val fixture = Fixture(backgroundScope)
        coEvery { fixture.session.applyTarget(any()) } throws IllegalStateException("ack failed")
        fixture.start(backgroundScope); runCurrent()
        assertFalse(fixture.coordinator.state.value.active)
        assertFalse(fixture.coordinator.state.value.stopPending)
        assertTrue(fixture.coordinator.state.value.captureEvents.any { it.type == "command_failed" })
        coVerify(exactly = 1) { fixture.session.stop() }
    }

    @Test fun disconnectSimulationRecordsAutonomousStopBeforeInterventionAndReconnect() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.coordinator.startDisconnectSimulation(1, "native_profile")
        assertTrue(fixture.coordinator.state.value.stopPending)

        fixture.coordinator.recordDisconnectTrialStep("autonomous_pump_stop_observed")
        val autonomous = fixture.coordinator.state.value.captureEvents.last { it.type == "autonomous_pump_stop_observed" }
        assertEquals("true", autonomous.fields["before_intervention"])
        assertEquals("true", autonomous.fields["before_reconnect_stop_command"])

        fixture.coordinator.recordDisconnectTrialStep("simulated_reconnect_and_verify")
        assertFalse(fixture.coordinator.state.value.stopPending)
        assertTrue(fixture.coordinator.state.value.captureEvents.any { it.type == "fresh_idle_confirmation" })
        coVerify(exactly = 0) { fixture.session.stop() }
        coVerify(exactly = 0) { fixture.execution.endExternalControl(any()) }
    }
}
