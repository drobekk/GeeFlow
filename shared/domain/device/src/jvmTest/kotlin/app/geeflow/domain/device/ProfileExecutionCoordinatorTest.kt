package app.geeflow.domain.device

import app.geeflow.data.brew.BrewHistoryRepository
import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewHistoryEntry
import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.BrewPhase
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.ExitCondition
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.ThresholdComparison
import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.LiveBrewSession
import app.geeflow.data.device.impl.controller.DemoDeviceController
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.pumpTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock

class ProfileExecutionCoordinatorTest {
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
        var stops = 0
        val writes = MutableStateFlow<List<PhaseControl>>(emptyList())
        val saved = MutableStateFlow<List<BrewHistoryEntry>>(emptyList())
        val recorder = ProfileExecutionRecorder(object : BrewHistoryRepository {
            override fun addBrew(entry: BrewHistoryEntry, dataPoints: Map<Float, BrewDataPoint>) {
                saved.update { it + entry }
            }
            override fun getBrews(userId: Long, query: String, limit: Int, offset: Int) = saved.value
            override fun getBrewDataPoints(id: Long): Map<Float, BrewDataPoint> = emptyMap()
            override fun deleteBrew(id: Long) = Unit
        })
        val controller = object : DeviceController by DemoDeviceController(scope) {
            override val deviceState = device
            override fun telemetry() = device.value.pumpTelemetry()
            override suspend fun openLiveSession(initial: PhaseControl): LiveBrewSession {
                device.update {
                    it.copy(
                        statusTime = Clock.System.now(),
                        brewStatus = DeviceState.BrewStatus.FreeVariable,
                        telemetryTime = Clock.System.now()
                    )
                }
                scope.launch {
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
                        stops++
                        device.update {
                            it.copy(
                                statusTime = Clock.System.now(),
                                brewStatus = DeviceState.BrewStatus.Idle
                            )
                        }
                    }
                }
            }
        }
        val coordinator = ProfileExecutionCoordinator(
            object : DeviceControllerProvider {
                override val currentDeviceId = 1L
                override fun getController(deviceId: Long) = controller
                override fun disconnectCurrent() = Unit
            },
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

    @Test fun completionSendsQuantizedTargetAndStopsExactlyOnce() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.coordinator.start(1, fixture.profile)
            withTimeout(4000) { fixture.coordinator.state.first { !it.active } }
            assertEquals(listOf(PhaseControl.Pressure(6.2f)), fixture.writes.value)
            assertEquals(1, fixture.stops)
            assertFalse(fixture.coordinator.state.value.manualStopRequired)
            assertEquals(1, fixture.coordinator.state.value.trace?.transitions?.size)
            withTimeout(4000) { fixture.saved.first { it.isNotEmpty() } }
            assertEquals("App", fixture.saved.value.single().profileName)
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

    @Test fun disconnectRequiresManualStopAndReconnectNeverResumes() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.coordinator.start(1, fixture.profile)
            withTimeout(4000) { fixture.writes.first { it.isNotEmpty() } }
            fixture.device.update { it.copy(connectionStatus = DeviceState.ConnectionStatus.Disconnected) }
            withTimeout(4000) { fixture.coordinator.state.first { !it.active } }
            assertTrue(fixture.coordinator.state.value.manualStopRequired)
            assertEquals(0, fixture.stops)
            fixture.device.update {
                it.copy(
                    connectionStatus = DeviceState.ConnectionStatus.Connected,
                    statusTime = Clock.System.now(),
                    brewStatus = DeviceState.BrewStatus.Idle
                )
            }
            withTimeout(4000) { fixture.coordinator.state.first { !it.manualStopRequired } }
            assertFalse(fixture.coordinator.state.value.active)
            assertEquals(1, fixture.writes.value.size)
        } finally {
            fixture.scope.cancel()
        }
    }
}
