package app.geeflow.domain.device.usecase

import app.geeflow.data.brew.BrewHistoryRepository
import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewHistoryEntry
import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.MaintenanceSettingsRepository
import app.geeflow.data.device.impl.controller.DemoDeviceController
import app.geeflow.data.device.model.CleaningProgram
import app.geeflow.data.device.model.CleaningReminder
import app.geeflow.data.device.model.CleaningType
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.MaintenanceSettings
import app.geeflow.domain.device.ProfileExecutionCoordinator
import app.geeflow.domain.device.ProfileExecutionRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StartCleaningUseCaseTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val calls = mutableListOf<String>()
    private var failUpload = false
    private var failSave = false
    private val saved = MutableStateFlow(
        MaintenanceSettings(
            daily = CleaningProgram(flushSeconds = 5, restSeconds = 5, cycles = 3),
            deep = CleaningProgram(flushSeconds = 5, restSeconds = 5, cycles = 6),
            dailyReminder = CleaningReminder(enabled = true, nextDueDay = 0),
            deepReminder = CleaningReminder(enabled = true, intervalDays = 7, nextDueDay = 0),
        ),
    )
    private val repository = object : MaintenanceSettingsRepository {
        override fun observe(deviceId: Long) = saved
        override suspend fun initialize(deviceId: Long, settings: MaintenanceSettings) = Unit
        override suspend fun save(deviceId: Long, settings: MaintenanceSettings, today: Long) = Unit
        override suspend fun remove(deviceId: Long) = Unit
        override suspend fun postpone(deviceId: Long, types: Set<CleaningType>, today: Long) {
            calls += "postpone"
            check(!failSave)
            saved.value = saved.value.postpone(types, today)
        }
    }
    private val controller = object : DeviceController by DemoDeviceController(scope) {
        override val deviceState = MutableStateFlow(
            DeviceState(connectionStatus = DeviceState.ConnectionStatus.Connected),
        )
        override suspend fun setCleaningSettings(timeSec: Float, standbySec: Float, count: Int) {
            assertEquals(5f, timeSec)
            assertEquals(5f, standbySec)
            calls += "settings:$count"
            check(!failUpload)
        }
        override suspend fun startCleaning() {
            calls += "start"
            deviceState.value = deviceState.value.copy(brewStatus = DeviceState.BrewStatus.Cleaning)
        }
        override suspend fun stopCleaning() = Unit
    }
    private val provider = object : DeviceControllerProvider {
        override val currentDeviceId = MutableStateFlow<Long?>(1L)
        override fun getController(deviceId: Long) = controller
        override fun disconnectCurrent() = Unit
    }
    private val history = object : BrewHistoryRepository {
        override fun addBrew(entry: BrewHistoryEntry, dataPoints: Map<Float, BrewDataPoint>) = Unit
        override fun getBrews(userId: Long, query: String, limit: Int, offset: Int) = emptyList<BrewHistoryEntry>()
        override fun getBrewDataPoints(id: Long) = emptyMap<Float, BrewDataPoint>()
        override fun deleteBrew(id: Long) = Unit
    }
    private val start = StartCleaningUseCase(
        coordinator = ProfileExecutionCoordinator(provider, scope, ProfileExecutionRecorder(history)),
        maintenance = repository,
        provider = provider,
    )

    @AfterTest
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `when upload fails then keeps postponed reminders`() = runBlocking {
        failUpload = true

        val result = runCatching { start(1, CleaningType.Deep) }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals(listOf("postpone", "settings:6"), calls)
        assertEquals(maintenanceDay() + 1, saved.value.dailyReminder.nextDueDay)
        assertEquals(maintenanceDay() + 7, saved.value.deepReminder.nextDueDay)
    }

    @Test
    fun `when daily cleaning stops then keeps postponed reminder`() = runBlocking {
        start(1, CleaningType.Daily)
        controller.stopCleaning()

        assertEquals(listOf("postpone", "settings:3", "start"), calls)
        assertEquals(maintenanceDay() + 1, saved.value.dailyReminder.nextDueDay)
        assertEquals(0L, saved.value.deepReminder.nextDueDay)
    }

    @Test
    fun `when persistence fails then prevents hardware writes`() = runBlocking {
        failSave = true

        val result = runCatching { start(1, CleaningType.Daily) }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals(listOf("postpone"), calls)
    }

    @Test
    fun `when cleaning is running then prevents another start`() = runBlocking {
        start(1, CleaningType.Daily)

        val result = runCatching { start(1, CleaningType.Daily) }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals(listOf("postpone", "settings:3", "start", "postpone"), calls)
    }
}
