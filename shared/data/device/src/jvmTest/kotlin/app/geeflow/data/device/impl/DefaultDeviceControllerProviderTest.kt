package app.geeflow.data.device.impl

import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerFactory
import app.geeflow.data.device.DeviceRepository
import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.data.device.model.SupportedDevice
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class DefaultDeviceControllerProviderTest {
    private val repository = mockk<DeviceRepository>(relaxed = true)
    private val factory = mockk<DeviceControllerFactory>()
    private val controller = mockk<DeviceController>(relaxed = true)
    private val device = Device(
        id = 1,
        name = "Coffee",
        connection = DeviceConnection.Ble("old", "mac"),
        modelId = SupportedDevice.WendougeeDataS.id
    )

    init {
        every { repository.getDeviceById(1) } returns device
        every { factory.create(SupportedDevice.WendougeeDataS) } returns controller
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when model changes from Data S to LITA and back then observers receive a fresh controller`() = runTest {
        val devices = MutableStateFlow(listOf(device))
        val lita = mockk<DeviceController>(relaxed = true)
        val replacement = mockk<DeviceController>(relaxed = true)
        every { repository.devices } returns devices
        every { repository.getDeviceById(1) } answers { devices.value.single() }
        every { factory.create(SupportedDevice.WendougeeDataS) } returnsMany listOf(controller, replacement)
        every { factory.create(SupportedDevice.LitaBA) } returns lita
        val provider = DefaultDeviceControllerProvider(repository, factory, backgroundScope)
        val observed = mutableListOf<DeviceController>()
        backgroundScope.launch { provider.observeController(1).collect { observed += it } }
        runCurrent()

        provider.forgetController(1)
        devices.value = listOf(device.copy(modelId = SupportedDevice.LitaBA.id))
        runCurrent()
        provider.forgetController(1)
        devices.value = listOf(device)
        runCurrent()

        assertEquals(listOf(controller, lita, replacement), observed)
        assertSame(replacement, provider.getController(1))
    }

    @Test
    fun `when controller is inspected then caches it without changing active device`() = runTest {
        val provider = DefaultDeviceControllerProvider(repository, factory, backgroundScope)

        val first = provider.getController(1)
        val second = provider.getController(1)

        assertSame(first, second)
        assertNull(provider.currentDeviceId.value)
        verify(exactly = 1) { factory.create(SupportedDevice.WendougeeDataS) }
    }

    @Test
    fun `when connection resolves peripheral then persists it before returning`() = runTest {
        val resolved = DeviceConnection.Ble("new", "mac")
        val connection = controller.connection
        coEvery { connection.connect(device) } returns resolved
        val provider = DefaultDeviceControllerProvider(repository, factory, backgroundScope)

        provider.connect(1)

        assertEquals(1, provider.currentDeviceId.value)
        verify(exactly = 1) { repository.updateConnection(1, resolved) }
    }

    @Test
    fun `when switching models then disconnects and recreates controller`() = runTest {
        val connection = controller.connection
        val provider = DefaultDeviceControllerProvider(repository, factory, backgroundScope)
        provider.getController(1)

        provider.forgetController(1)
        provider.getController(1)

        coVerifyOrder {
            factory.create(SupportedDevice.WendougeeDataS)
            connection.disconnect()
            factory.create(SupportedDevice.WendougeeDataS)
        }
    }

    @Test
    fun `when model is unknown then does not create a fallback controller`() = runTest {
        every { repository.getDeviceById(1) } returns device.copy(modelId = null)
        val provider = DefaultDeviceControllerProvider(repository, factory, backgroundScope)

        val result = runCatching { provider.getController(1) }

        assertEquals(true, result.isFailure)
        verify(exactly = 0) { factory.create(any()) }
    }
}
