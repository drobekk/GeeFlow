package dev.drobek.geeflow.domain.brew.usecase

import dev.drobek.geeflow.data.device.api.DeviceController
import dev.drobek.geeflow.data.users.api.UserRepository
import dev.drobek.geeflow.domain.brew.model.BrewSession
import dev.drobek.geeflow.domain.device.model.MachineState
import dev.drobek.geeflow.domain.user.model.User
import dev.drobek.geeflow.domain.user.usecase.GetSelectedUserUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeDeviceController : DeviceController {
    override val machineState = MutableStateFlow(MachineState())

    override fun connect(macAddress: String) {}
    override fun disconnect() {}
    override suspend fun setBoilerState(boilerType: MachineState.BoilerType, enabled: Boolean) {}
    override suspend fun setBrewTemperature(temp: Int) {}
    override suspend fun setSteamTemperature(temp: Int) {}
    override suspend fun startManualBrewing() {}
    override suspend fun triggerShortPress() {}
    override suspend fun setHeatingMode(heatingMode: MachineState.HeatingMode) {}
    override suspend fun stopCleaning() {}
    override suspend fun startCleaning() {}
    override suspend fun stopManualBrewing() {}
}

class FakeUserRepository : UserRepository {
    override val users: StateFlow<List<User>> = MutableStateFlow(emptyList())
    override val selectedUser: Flow<User?> = MutableStateFlow(User(id = 1L, name = "Test User"))

    override fun addUser(user: User) {}
    override fun getUserById(id: Long): User? = null
    override fun removeUser(id: Long) {}
    override fun setSelectedUser(id: Long) {}
    override fun setFavoriteDevice(userId: Long, deviceMacAddress: String?) {}
}

class ObserveBrewDataUseCaseTest {
    private val controller = FakeDeviceController()
    private val userRepository = FakeUserRepository()
    private val getSelectedUserUseCase = GetSelectedUserUseCase(userRepository)
    private val useCase = ObserveBrewDataUseCase(controller, getSelectedUserUseCase)

    @Test
    fun aggregationStartsOnManualStatus() = runTest {
        val results = mutableListOf<BrewSession>()
        val job = launch { useCase().collect { results.add(it) } }

        // Initial emission from scan
        assertTrue(results.last().dataPoints.isEmpty())

        // Start brewing
        controller.machineState.value = MachineState(
            brewStatus = MachineState.BrewStatus.Manual,
            time = 1,
            pressure = 9f,
            weight = 2.0f
        )

        assertEquals(1, results.last().dataPoints.size)
        assertEquals(9f, results.last().dataPoints[1f]?.pressure)
        assertEquals("M", results.last().profileName)
        assertEquals(null, results.last().profileId)
        assertEquals(1L, results.last().userId)

        // Another second
        controller.machineState.value = MachineState(
            brewStatus = MachineState.BrewStatus.Manual,
            time = 2,
            pressure = 8.5f,
            weight = 4.0f
        )

        assertEquals(2, results.last().dataPoints.size)
        assertEquals(8.5f, results.last().dataPoints[2f]?.pressure)

        job.cancel()
    }

    @Test
    fun aggregationResetsOnNewBrew() = runTest {
        val results = mutableListOf<BrewSession>()
        val job = launch { useCase().collect { results.add(it) } }

        // First brew
        controller.machineState.value = MachineState(
            brewStatus = MachineState.BrewStatus.Manual,
            time = 1,
            pressure = 9f
        )

        assertEquals(1, results.last().dataPoints.size)

        // Stop
        controller.machineState.value = MachineState(brewStatus = MachineState.BrewStatus.Idle)

        // Next brew starts
        controller.machineState.value = MachineState(
            brewStatus = MachineState.BrewStatus.Manual,
            time = 1,
            pressure = 8f
        )

        // It should have cleared the previous data because isBrewing was set to false on Idle
        assertEquals(1, results.last().dataPoints.size)
        assertEquals(8f, results.last().dataPoints[1f]?.pressure)

        job.cancel()
    }
}
