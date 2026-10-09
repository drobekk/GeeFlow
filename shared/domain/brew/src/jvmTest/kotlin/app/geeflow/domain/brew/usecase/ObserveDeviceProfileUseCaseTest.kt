package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewProfileRepository
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.data.device.DeviceRepository
import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveDeviceProfileUseCaseTest {
    private val profiles = mockk<BrewProfileRepository>(relaxUnitFun = true)
    private val devices = mockk<DeviceRepository>(relaxUnitFun = true)
    private val profile = BrewProfile(
        id = 3,
        userId = 7,
        name = "Shot",
        description = "Recipe",
        finishCondition = null,
        steps = listOf(ProfileStep.Pressure(30, 9f)),
        autoLinkOpen = true,
        position = 4
    )
    private val device = Device(
        id = 9,
        name = "Machine",
        connection = DeviceConnection.Ble("peripheral", "AA:BB:CC:DD:EE:FF"),
        boundProfileId = 3
    )

    @Test
    fun `when dependency contains data then returns requested data`() = runTest {
        every { devices.devices } returns MutableStateFlow(listOf(device))
        every { profiles.brewProfiles } returns MutableStateFlow(listOf(profile))
        val useCase = ObserveDeviceProfileUseCase(devices, profiles)

        val result = useCase(9).first()

        assertEquals(profile, result)
    }
}
