package app.geeflow.app.navigation

import app.geeflow.data.device.DeviceRepository
import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.model.User
import app.geeflow.domain.device.usecase.ObserveDevicesUseCase
import app.geeflow.domain.user.usecase.GetUsersUseCase
import app.geeflow.navigation.destination.DeviceDashboard
import app.geeflow.navigation.destination.DeviceList
import app.geeflow.navigation.destination.Intro
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetInitialDestinationUseCaseTest {
    private val users = mockk<UserRepository>()
    private val devices = mockk<DeviceRepository>()

    @Test
    fun `when no users or devices exist then opens intro`() = runTest {
        every { users.users } returns MutableStateFlow(emptyList())
        every { devices.devices } returns MutableStateFlow(emptyList())
        val useCase = GetInitialDestinationUseCase(GetUsersUseCase(users), ObserveDevicesUseCase(devices))

        val result = useCase()

        assertEquals(Intro, result)
    }

    @Test
    fun `when users exist without devices then opens device list`() = runTest {
        every { users.users } returns MutableStateFlow(listOf(User(id = 7, name = "Ada")))
        every { devices.devices } returns MutableStateFlow(emptyList())
        val useCase = GetInitialDestinationUseCase(GetUsersUseCase(users), ObserveDevicesUseCase(devices))

        val result = useCase()

        assertEquals(DeviceList, result)
    }

    @Test
    fun `when selected user has valid favorite then opens favorite dashboard`() = runTest {
        every { users.users } returns MutableStateFlow(
            listOf(User(id = 7, name = "Ada", isSelected = true, favoriteDeviceId = 10))
        )
        every { devices.devices } returns MutableStateFlow(
            listOf(
                Device(id = 9, name = "A", connection = DeviceConnection.Ble("a", "a")),
                Device(id = 10, name = "B", connection = DeviceConnection.Ble("b", "b"))
            )
        )
        val useCase = GetInitialDestinationUseCase(GetUsersUseCase(users), ObserveDevicesUseCase(devices))

        val result = useCase()

        assertEquals(DeviceDashboard(10), result)
    }

    @Test
    fun `when favorite device no longer exists then opens first dashboard`() = runTest {
        every { users.users } returns MutableStateFlow(
            listOf(User(id = 7, name = "Ada", isSelected = true, favoriteDeviceId = 10))
        )
        every { devices.devices } returns MutableStateFlow(
            listOf(Device(id = 9, name = "A", connection = DeviceConnection.Ble("a", "a")))
        )
        val useCase = GetInitialDestinationUseCase(GetUsersUseCase(users), ObserveDevicesUseCase(devices))

        val result = useCase()

        assertEquals(DeviceDashboard(9), result)
    }
}
