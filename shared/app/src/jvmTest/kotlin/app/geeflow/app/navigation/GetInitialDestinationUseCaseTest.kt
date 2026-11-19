package app.geeflow.app.navigation

import app.geeflow.domain.user.usecase.EnsureDefaultUserUseCase
import app.geeflow.navigation.destination.DeviceList
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

class GetInitialDestinationUseCaseTest {
    @Test
    fun `always opens the device list after ensuring a default user`() {
        val ensureDefaultUser = mockk<EnsureDefaultUserUseCase>(relaxed = true)
        val useCase = GetInitialDestinationUseCase(ensureDefaultUser)

        assertEquals(DeviceList, useCase())
        verify(exactly = 1) { ensureDefaultUser() }
    }
}
