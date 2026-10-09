package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewProfileRepository
import app.geeflow.data.device.DeviceRepository
import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class DeleteProfileUseCaseTest {
    private val profiles = mockk<BrewProfileRepository>(relaxUnitFun = true)
    private val devices = mockk<DeviceRepository>(relaxUnitFun = true)
    private val device = Device(
        id = 9,
        name = "Machine",
        connection = DeviceConnection.Ble("peripheral", "AA:BB:CC:DD:EE:FF"),
        boundProfileId = 3
    )

    @Test
    fun `when profile is unbound then removes profile`() = runTest {
        every { devices.devices } returns MutableStateFlow(emptyList())
        val useCase = DeleteProfileUseCase(profiles, devices)

        useCase(3)

        verify(exactly = 1) { profiles.removeBrewProfile(3) }
    }

    @Test
    fun `when profile is bound then prevents deletion`() = runTest {
        every { devices.devices } returns MutableStateFlow(listOf(device))
        val useCase = DeleteProfileUseCase(profiles, devices)

        val result = runCatching { useCase(3) }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
        verify(exactly = 0) { profiles.removeBrewProfile(any()) }
    }
}
