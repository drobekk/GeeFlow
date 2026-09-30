package app.geeflow.presentation.feature.device.dashboard.maintenance

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.viewModelScope
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
import app.geeflow.data.device.model.DeviceState.BrewStatus
import app.geeflow.data.device.model.DeviceState.ConnectionStatus
import app.geeflow.data.device.model.MaintenanceSettings
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.impl.UserSettingsRepositoryImpl
import app.geeflow.data.user.model.User
import app.geeflow.domain.device.ProfileExecutionCoordinator
import app.geeflow.domain.device.ProfileExecutionRecorder
import app.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import app.geeflow.domain.device.usecase.ObserveMaintenanceSettingsUseCase
import app.geeflow.domain.device.usecase.SkipMaintenanceReminderUseCase
import app.geeflow.domain.device.usecase.maintenanceDay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MaintenanceReminderViewModelTest {
    @Test
    fun `dashboard reminder is eligible only while its machine is connected and idle`() = runTest {
        withFixture {
            runCurrent()
            assertEquals(CleaningType.entries.toSet(), viewModel.viewState.value.dueTypes)
            assertTrue(viewModel.viewState.value.eligible)

            firstState.value = firstState.value.copy(brewStatus = BrewStatus.Cleaning)
            runCurrent()
            assertFalse(viewModel.viewState.value.eligible)
            firstState.value = firstState.value.copy(brewStatus = BrewStatus.Idle)
            runCurrent()
            assertTrue(viewModel.viewState.value.eligible)
            firstState.value = firstState.value.copy(connectionStatus = ConnectionStatus.Disconnected)
            runCurrent()
            assertFalse(viewModel.viewState.value.eligible)
            assertEquals(CleaningType.entries.toSet(), viewModel.viewState.value.dueTypes)
            assertTrue(postponedDevices.isEmpty())
        }
    }

    @Test
    fun `each dashboard observes only its own machine reminders`() = runTest {
        withFixture {
            runCurrent()
            assertEquals(CleaningType.entries.toSet(), viewModel.viewState.value.dueTypes)
            assertEquals(setOf(CleaningType.Daily), secondViewModel.viewState.value.dueTypes)

            firstState.value = firstState.value.copy(connectionStatus = ConnectionStatus.Disconnected)
            firstSettings.value = firstSettings.value?.postpone(CleaningType.entries.toSet(), maintenanceDay())
            runCurrent()
            assertFalse(viewModel.viewState.value.eligible)
            assertTrue(secondViewModel.viewState.value.eligible)
            assertEquals(setOf(CleaningType.Daily), secondViewModel.viewState.value.dueTypes)
        }
    }

    @Test
    fun `skip postpones both overdue reminders for the displayed machine`() = runTest {
        withFixture {
            runCurrent()
            viewModel.skip(viewModel.viewState.value.dueTypes)
            runCurrent()
            assertEquals(listOf(1L), postponedDevices)
            assertEquals(maintenanceDay() + 1, firstSettings.value?.dailyReminder?.nextDueDay)
            assertEquals(maintenanceDay() + 7, firstSettings.value?.deepReminder?.nextDueDay)
            assertTrue(viewModel.viewState.value.dueTypes.isEmpty())
            assertFalse(viewModel.viewState.value.busy)
        }
    }

    @Test
    fun `pending skip does not affect another dashboard and a second click does not repeat the write`() = runTest {
        withFixture {
            val release = CompletableDeferred<Unit>()
            skipRelease = release
            runCurrent()
            viewModel.skip(viewModel.viewState.value.dueTypes)
            runCurrent()
            assertTrue(viewModel.viewState.value.busy)
            viewModel.skip(viewModel.viewState.value.dueTypes)
            runCurrent()

            release.complete(Unit)
            runCurrent()
            assertEquals(listOf(1L), postponedDevices)
            assertEquals(setOf(CleaningType.Daily), secondViewModel.viewState.value.dueTypes)
            assertFalse(viewModel.viewState.value.busy)
        }
    }

    private suspend fun TestScope.withFixture(block: suspend Fixture.() -> Unit) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fixture = Fixture(backgroundScope)
        try {
            fixture.block()
        } finally {
            fixture.viewModel.viewModelScope.cancel()
            fixture.secondViewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    private class Fixture(scope: CoroutineScope) {
        val firstState = MutableStateFlow(DeviceState(connectionStatus = ConnectionStatus.Connected))
        private val secondState = MutableStateFlow(firstState.value)
        val firstSettings = MutableStateFlow<MaintenanceSettings?>(dueSettings())
        private val secondSettings = MutableStateFlow<MaintenanceSettings?>(
            dueSettings().copy(deepReminder = CleaningReminder(intervalDays = 7)),
        )
        val postponedDevices = mutableListOf<Long>()
        var skipRelease: CompletableDeferred<Unit>? = null

        private val firstController = object : DeviceController by DemoDeviceController(scope) {
            override val deviceState = firstState
        }
        private val secondController = object : DeviceController by DemoDeviceController(scope) {
            override val deviceState = secondState
        }
        private val provider = object : DeviceControllerProvider {
            override val currentDeviceId = MutableStateFlow<Long?>(1L)
            override fun getController(deviceId: Long) = if (deviceId == 1L) firstController else secondController
            override fun disconnectCurrent() = Unit
        }
        private val maintenance = object : MaintenanceSettingsRepository {
            override fun observe(deviceId: Long) = if (deviceId == 1L) firstSettings else secondSettings
            override suspend fun initialize(deviceId: Long, settings: MaintenanceSettings) = Unit
            override suspend fun save(deviceId: Long, settings: MaintenanceSettings, today: Long) = Unit
            override suspend fun remove(deviceId: Long) = Unit
            override suspend fun postpone(deviceId: Long, types: Set<CleaningType>, today: Long) {
                skipRelease?.await()
                postponedDevices += deviceId
                val saved = if (deviceId == 1L) firstSettings else secondSettings
                saved.value = saved.value?.postpone(types, today)
            }
        }
        private val user = User(id = 1, name = "Test")
        private val users = object : UserRepository {
            override val users = MutableStateFlow(listOf(user))
            override val selectedUser = MutableStateFlow(user)
            override fun addUser(user: User) = user.id
            override fun getUserById(id: Long) = user
            override fun removeUser(id: Long) = Unit
            override fun setSelectedUser(id: Long) = Unit
            override fun renameUser(id: Long, name: String) = Unit
            override fun updatePhotoUri(id: Long, uri: String?) = Unit
            override fun setFavoriteDevice(userId: Long, deviceId: Long?) = Unit
        }
        private val userSettings = UserSettingsRepositoryImpl(object : DataStore<Preferences> {
            override val data = flowOf(emptyPreferences())
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences) = transform(emptyPreferences())
        })
        private val history = object : BrewHistoryRepository {
            override fun addBrew(entry: BrewHistoryEntry, dataPoints: Map<Float, BrewDataPoint>) = Unit
            override fun getBrews(userId: Long, query: String, limit: Int, offset: Int) = emptyList<BrewHistoryEntry>()
            override fun getBrewDataPoints(id: Long) = emptyMap<Float, BrewDataPoint>()
            override fun deleteBrew(id: Long) = Unit
        }
        private val execution = ProfileExecutionCoordinator(provider, scope, ProfileExecutionRecorder(history))
        val viewModel = reminderViewModel(1)
        val secondViewModel = reminderViewModel(2)

        private fun reminderViewModel(deviceId: Long) = MaintenanceReminderViewModel(
            deviceId = deviceId,
            observeMaintenance = ObserveMaintenanceSettingsUseCase(maintenance, provider),
            observeDevice = ObserveDeviceStateUseCase(execution, provider, userSettings, users),
            skipReminder = SkipMaintenanceReminderUseCase(maintenance),
        )

        private fun dueSettings() = MaintenanceSettings(
            daily = CleaningProgram(flushSeconds = 5, restSeconds = 5, cycles = 3),
            deep = CleaningProgram(flushSeconds = 5, restSeconds = 5, cycles = 6),
            dailyReminder = CleaningReminder(enabled = true, nextDueDay = maintenanceDay()),
            deepReminder = CleaningReminder(enabled = true, intervalDays = 7, nextDueDay = maintenanceDay()),
        )
    }
}
