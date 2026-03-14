package dev.drobek.geeflow.domain.brew.usecase

import dev.drobek.geeflow.data.device.api.DeviceController
import dev.drobek.geeflow.domain.brew.model.BrewDataPoint
import dev.drobek.geeflow.domain.device.model.MachineState
import kotlinx.coroutines.flow.MutableStateFlow
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

class ObserveBrewDataUseCaseTest {
    private val controller = FakeDeviceController()
    private val useCase = ObserveBrewDataUseCase(controller)

    @Test
    fun aggregationStartsOnManualStatus() = runTest {
        val results = mutableListOf<Map<Int, BrewDataPoint>>()
        val job = launch { useCase().collect { results.add(it) } }

        // Initial emission from scan
        assertTrue(results.last().isEmpty())

        // Start brewing
        controller.machineState.value = MachineState(
            brewStatus = MachineState.BrewStatus.Manual,
            time = 1,
            pressure = 9f,
            weight = 2.0f
        )

        assertEquals(1, results.last().size)
        assertEquals(9f, results.last()[1]?.pressure)

        // Another second
        controller.machineState.value = MachineState(
            brewStatus = MachineState.BrewStatus.Manual,
            time = 2,
            pressure = 8.5f,
            weight = 4.0f
        )

        assertEquals(2, results.last().size)
        assertEquals(8.5f, results.last()[2]?.pressure)

        job.cancel()
    }

    @Test
    fun aggregationResetsOnNewBrew() = runTest {
        val results = mutableListOf<Map<Int, BrewDataPoint>>()
        val job = launch { useCase().collect { results.add(it) } }

        // First brew
        controller.machineState.value = MachineState(
            brewStatus = MachineState.BrewStatus.Manual,
            time = 1,
            pressure = 9f
        )
        
        assertEquals(1, results.last().size)

        // Stop
        controller.machineState.value = MachineState(brewStatus = MachineState.BrewStatus.Idle)
        
        // Next brew starts
        controller.machineState.value = MachineState(
            brewStatus = MachineState.BrewStatus.Manual,
            time = 1,
            pressure = 8f
        )

        // It should have cleared the previous data because isBrewing was set to false on Idle
        assertEquals(1, results.last().size)
        assertEquals(8f, results.last()[1]?.pressure)

        job.cancel()
    }
}
