package dev.drobek.geeflow.device

import dev.drobek.geeflow.data.devices.api.DeviceRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import dev.drobek.geeflow.domain.device.model.Device
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryDeviceRepository : DeviceRepository {
    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    override val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    override fun addDevice(device: Device) {
        _devices.value = _devices.value + device
    }

    override fun removeDeviceBySerialNumber(serialNumber: String) {
        _devices.value = _devices.value.filter { it.serialNumber != serialNumber }
    }

    override fun setLastUsedDevice(serialNumber: String) {
        _devices.value = _devices.value.map {
            it.copy(isLastUsed = it.serialNumber == serialNumber)
        }
    }

    override fun clearAll() {
        _devices.value = emptyList()
    }
}

class DeviceRepositoryTest {
    @Test
    fun addDevice_emitsDevice() {
        val repo = InMemoryDeviceRepository()
        val device = Device(serialNumber = "1", name = "Device 1", macAddress = "AA:BB:CC")

        repo.addDevice(device)

        val items = repo.devices.value
        assertEquals(1, items.size)
        assertEquals(device, items.first())
    }

    @Test
    fun removeDeviceBySerialNumber_removesDevice() {
        val repo = InMemoryDeviceRepository()
        val d1 = Device(serialNumber = "1", name = "Device 1", macAddress = "AA:BB:CC")
        val d2 = Device(serialNumber = "2", name = "Device 2", macAddress = "DD:EE:FF")

        repo.addDevice(d1)
        repo.addDevice(d2)

        var items = repo.devices.value
        assertEquals(2, items.size)

        repo.removeDeviceBySerialNumber("1")
        items = repo.devices.value
        assertEquals(1, items.size)
        assertEquals(d2, items.first())
    }
}
