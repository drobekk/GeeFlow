package dev.drobek.geeflow.device

import kotlin.test.Test
import kotlin.test.assertEquals
import dev.drobek.geeflow.domain.device.model.Device

class DeviceRepositoryTest {
    @Test
    fun addDevice_emitsDevice() {
        val repo = InMemoryDeviceRepository()
        val device = Device(id = "1", macAddress = "AA:BB:CC")

        repo.addDevice(device)

        val items = repo.devices.value
        assertEquals(1, items.size)
        assertEquals(device, items.first())
    }

    @Test
    fun removeDeviceById_removesDevice() {
        val repo = InMemoryDeviceRepository()
        val d1 = Device(id = "1")
        val d2 = Device(id = "2")

        repo.addDevice(d1)
        repo.addDevice(d2)

        var items = repo.devices.value
        assertEquals(2, items.size)

        repo.removeDeviceById("1")
        items = repo.devices.value
        assertEquals(1, items.size)
        assertEquals(d2, items.first())
    }
}
