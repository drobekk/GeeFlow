package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceBrewingSettingsRepository
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.DeviceRepository
import app.geeflow.data.device.MaintenanceSettingsRepository
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.model.User
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DeleteDeviceUseCaseTest {
    private val provider = mockk<DeviceControllerProvider>(relaxUnitFun = true)
    private val devices = mockk<DeviceRepository>(relaxUnitFun = true)
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val brewing = mockk<DeviceBrewingSettingsRepository>(relaxUnitFun = true)
    private val maintenance = mockk<MaintenanceSettingsRepository>(relaxUnitFun = true)

    init {
        every { provider.currentDeviceId } returns MutableStateFlow(9L)
    }

    @Test
    fun `when deleted device is active and favorite then clears settings connection and favorites`() = runTest {
        every { users.users } returns MutableStateFlow(
            listOf(User(id = 7, name = "Ada", favoriteDeviceId = 9), User(id = 8, name = "Grace", favoriteDeviceId = 10))
        )
        val useCase =
            DeleteDeviceUseCase(brewing, maintenance, devices, users, provider)

        useCase(9)

        coVerifyOrder {
            brewing.remove(9)
            maintenance.remove(9)
            provider.disconnectCurrent()
            devices.removeDeviceById(9)
            users.setFavoriteDevice(7, null)
        }
        verify(exactly = 0) { users.setFavoriteDevice(8, any()) }
    }
}
