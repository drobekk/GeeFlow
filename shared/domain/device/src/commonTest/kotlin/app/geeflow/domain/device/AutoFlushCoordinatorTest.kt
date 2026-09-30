package app.geeflow.domain.device

import app.geeflow.data.brew.BrewHistoryRepository
import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewHistoryEntry
import app.geeflow.data.device.DeviceBrewingSettingsRepository
import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.impl.controller.DemoDeviceController
import app.geeflow.data.device.model.DeviceBrewingSettings
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.DeviceState.BrewStatus
import app.geeflow.data.device.model.DeviceState.ConnectionStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class AutoFlushCoordinatorTest {
    @Test
    fun `manual flush does not schedule auto flush while profile and freehand brewing still do`() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.settings.value = fixture.settings.value.copy(treatManualAsFlush = true)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Manual)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Idle)
        runCurrent()
        advanceTimeBy(5000)
        runCurrent()
        assertNull(fixture.coordinator.countdown.value)
        assertEquals(0, fixture.starts)

        for (status in listOf(BrewStatus.Profile, BrewStatus.FreeVariable)) {
            fixture.state.value = fixture.state.value.copy(brewStatus = status)
            runCurrent()
            fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Idle)
            runCurrent()
            assertNotNull(fixture.coordinator.countdown.value)
            fixture.coordinator.cancel()
            runCurrent()
        }
    }

    @Test
    fun `changing device manual flush setting cancels a pending manual countdown`() = runTest {
        val fixture = Fixture(backgroundScope)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Manual)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Idle)
        runCurrent()
        assertNotNull(fixture.coordinator.countdown.value)

        fixture.settings.value = fixture.settings.value.copy(treatManualAsFlush = true)
        runCurrent()
        assertNull(fixture.coordinator.countdown.value)
        fixture.settings.value = fixture.settings.value.copy(treatManualAsFlush = false)
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Manual)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Idle)
        runCurrent()
        assertNotNull(fixture.coordinator.countdown.value)

        fixture.settings.value = fixture.settings.value.copy(treatManualAsFlush = true)
        runCurrent()
        advanceTimeBy(5000)
        runCurrent()
        assertNull(fixture.coordinator.countdown.value)
        assertEquals(0, fixture.starts)
    }

    @Test
    fun `countdown survives observing the same device again and automatic flush does not repeat`() = runTest {
        val fixture = Fixture(backgroundScope)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Profile)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Idle)
        runCurrent()
        assertEquals(AutoFlushCountdown(1, 3, 3), fixture.coordinator.countdown.value)

        advanceTimeBy(1000)
        fixture.currentDeviceId.value = 1
        runCurrent()
        assertEquals(2, fixture.coordinator.countdown.value?.remainingSeconds)
        advanceTimeBy(2000)
        runCurrent()
        assertEquals(1, fixture.starts)
        assertNull(fixture.coordinator.countdown.value)

        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Idle)
        runCurrent()
        advanceTimeBy(5000)
        runCurrent()
        assertEquals(1, fixture.starts)
        assertNull(fixture.coordinator.countdown.value)
    }

    @Test
    fun `cancelling a countdown never starts the flush`() = runTest {
        val fixture = Fixture(backgroundScope)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.FreeVariable)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Idle)
        runCurrent()
        assertNotNull(fixture.coordinator.countdown.value)

        fixture.coordinator.cancel()
        runCurrent()
        assertNull(fixture.coordinator.countdown.value)
        advanceTimeBy(5000)
        runCurrent()
        assertEquals(0, fixture.starts)
    }

    @Test
    fun `backgrounding cancels a countdown and returning does not resume it`() = runTest {
        val fixture = Fixture(backgroundScope)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Manual)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Idle)
        runCurrent()
        assertNotNull(fixture.coordinator.countdown.value)

        fixture.coordinator.setForeground(false)
        runCurrent()
        assertNull(fixture.coordinator.countdown.value)
        fixture.coordinator.setForeground(true)
        advanceTimeBy(5000)
        runCurrent()
        assertEquals(0, fixture.starts)
    }

    @Test
    fun `a shot ending in the background does not schedule a flush on returning`() = runTest {
        val fixture = Fixture(backgroundScope)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Profile)
        runCurrent()
        fixture.coordinator.setForeground(false)
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Idle)
        runCurrent()
        fixture.coordinator.setForeground(true)
        advanceTimeBy(5000)
        runCurrent()
        assertNull(fixture.coordinator.countdown.value)
        assertEquals(0, fixture.starts)
    }

    @Test
    fun `flush rechecks machine state after waiting for the control lock`() = runTest {
        val fixture = Fixture(backgroundScope)
        val release = CompletableDeferred<Unit>()
        runCurrent()
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Profile)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Idle)
        runCurrent()
        backgroundScope.launch { fixture.execution.withUnownedControl(1) { release.await() } }
        runCurrent()

        advanceTimeBy(3000)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Cleaning)
        release.complete(Unit)
        runCurrent()
        assertEquals(0, fixture.starts)
        assertNull(fixture.coordinator.countdown.value)
    }

    @Test
    fun `an alarm disconnection or another operation cancels the pending flush`() = runTest {
        val fixture = Fixture(backgroundScope)
        runCurrent()
        val interruptedStates = listOf(
            fixture.state.value.copy(waterLevelAlarm = true),
            fixture.state.value.copy(connectionStatus = ConnectionStatus.Disconnected),
            fixture.state.value.copy(brewStatus = BrewStatus.Cleaning),
        )
        for (interrupted in interruptedStates) {
            fixture.state.value = DeviceState(connectionStatus = ConnectionStatus.Connected, brewStatus = BrewStatus.Profile)
            runCurrent()
            fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Idle)
            runCurrent()
            assertNotNull(fixture.coordinator.countdown.value)
            fixture.state.value = interrupted
            runCurrent()
            assertNull(fixture.coordinator.countdown.value)
            advanceTimeBy(5000)
            runCurrent()
            assertEquals(0, fixture.starts)
        }
    }

    @Test
    fun `changing settings or switching devices cancels the pending flush`() = runTest {
        val fixture = Fixture(backgroundScope)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Profile)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Idle)
        runCurrent()
        fixture.settings.value = DeviceBrewingSettings(
            treatManualAsFlush = false,
            autoFlushEnabled = true,
            autoFlushDelaySeconds = 4,
        )
        runCurrent()
        assertNull(fixture.coordinator.countdown.value)

        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Profile)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Idle)
        runCurrent()
        assertNotNull(fixture.coordinator.countdown.value)
        fixture.currentDeviceId.value = 2
        runCurrent()
        advanceTimeBy(5000)
        runCurrent()
        assertNull(fixture.coordinator.countdown.value)
        assertEquals(0, fixture.starts)
    }

    @Test
    fun `clearing the current device cancels the countdown and reconnecting does not resume it`() = runTest {
        val fixture = Fixture(backgroundScope)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Profile)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Idle)
        runCurrent()
        assertNotNull(fixture.coordinator.countdown.value)

        fixture.currentDeviceId.value = null
        runCurrent()
        assertNull(fixture.coordinator.countdown.value)
        fixture.currentDeviceId.value = 1
        runCurrent()
        advanceTimeBy(5000)
        runCurrent()
        assertNull(fixture.coordinator.countdown.value)
        assertEquals(0, fixture.starts)
    }

    @Test
    fun `failed flush reports an error and does not suppress the next manual shot`() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.failStart = true
        var error: Throwable? = null
        backgroundScope.launch { error = fixture.coordinator.errors.first() }
        runCurrent()
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Profile)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Idle)
        runCurrent()
        advanceTimeBy(3000)
        runCurrent()
        assertEquals("start failed", error?.message)
        assertNull(fixture.coordinator.countdown.value)

        fixture.failStart = false
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Manual)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(brewStatus = BrewStatus.Idle)
        runCurrent()
        assertNotNull(fixture.coordinator.countdown.value)
    }

    private class Fixture(scope: CoroutineScope) {
        val currentDeviceId = MutableStateFlow<Long?>(1L)
        val state = MutableStateFlow(DeviceState(connectionStatus = ConnectionStatus.Connected))
        val settings = MutableStateFlow(
            DeviceBrewingSettings(treatManualAsFlush = false, autoFlushEnabled = true, autoFlushDelaySeconds = 3),
        )
        private val otherSettings = MutableStateFlow(DeviceBrewingSettings())
        var starts = 0
        var failStart = false

        private val controller = object : DeviceController by DemoDeviceController(scope) {
            override val deviceState = state
            override suspend fun startManualBrewing() {
                check(!failStart) { "start failed" }
                starts++
                state.value = state.value.copy(brewStatus = BrewStatus.Manual)
            }
        }
        private val provider = object : DeviceControllerProvider {
            override val currentDeviceId = this@Fixture.currentDeviceId
            override fun getController(deviceId: Long) = controller
            override fun disconnectCurrent() = Unit
        }
        private val repository = object : DeviceBrewingSettingsRepository {
            override fun observe(deviceId: Long) = if (deviceId == 1L) settings else otherSettings
            override suspend fun save(deviceId: Long, settings: DeviceBrewingSettings) = Unit
            override suspend fun remove(deviceId: Long) = Unit
        }
        private val history = object : BrewHistoryRepository {
            override fun addBrew(entry: BrewHistoryEntry, dataPoints: Map<Float, BrewDataPoint>) = Unit
            override fun getBrews(userId: Long, query: String, limit: Int, offset: Int) = emptyList<BrewHistoryEntry>()
            override fun getBrewDataPoints(id: Long) = emptyMap<Float, BrewDataPoint>()
            override fun deleteBrew(id: Long) = Unit
        }
        val execution = ProfileExecutionCoordinator(provider, scope, ProfileExecutionRecorder(history))
        val coordinator = AutoFlushCoordinator(
            settingsRepository = repository,
            provider = provider,
            execution = execution,
            scope = scope,
        )

        init {
            coordinator.setForeground(true)
        }
    }
}
