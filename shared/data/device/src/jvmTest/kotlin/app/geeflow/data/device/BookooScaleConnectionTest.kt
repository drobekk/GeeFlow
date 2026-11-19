package app.geeflow.data.device

import app.geeflow.data.device.ble.BleScanCoordinator
import dev.bluefalcon.core.BlueFalcon
import dev.bluefalcon.core.BluetoothPeripheral
import dev.bluefalcon.core.BluetoothPeripheralState
import dev.bluefalcon.core.BlueFalconEngine
import dev.bluefalcon.core.BluetoothService
import dev.bluefalcon.core.BluetoothCharacteristic
import dev.bluefalcon.core.CharacteristicNotification
import dev.bluefalcon.core.ServiceDiscoveryPhase
import dev.bluefalcon.core.ServiceDiscoveryUpdate
import dev.bluefalcon.core.toUuid
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookooScaleConnectionTest {
    @Test
    fun `notifications survive requesting scope cancellation and dropped connection retries same scale`() = runTest {
        val client = mockk<BlueFalcon>(relaxed = true)
        val engine = mockk<BlueFalconEngine>(relaxed = true)
        val peripheral = mockk<BluetoothPeripheral>(relaxed = true)
        val service = mockk<BluetoothService>(relaxed = true)
        val weight = mockk<BluetoothCharacteristic>(relaxed = true)
        val command = mockk<BluetoothCharacteristic>(relaxed = true)
        val discovery = MutableSharedFlow<ServiceDiscoveryUpdate>()
        val notifications = MutableSharedFlow<CharacteristicNotification>()
        every { peripheral.name } returns "BOOKOO"
        every { peripheral.uuid } returns "AA:BB:CC:DD:EE:FF"
        every { peripheral.services } returns listOf(service)
        every { peripheral.characteristics } returns listOf(weight, command)
        every { service.uuid } returns "0ffe".toUuid()
        every { weight.uuid } returns "ff11".toUuid()
        every { command.uuid } returns "ff12".toUuid()
        every { client.peripherals } returns MutableStateFlow(setOf(peripheral))
        var linkState = BluetoothPeripheralState.Connected
        every { client.connectionState(peripheral) } answers { linkState }
        coEvery { client.connect(peripheral, false) } coAnswers { linkState = BluetoothPeripheralState.Connected }
        every { client.engine } returns engine
        every { client.serviceDiscoveryUpdates } returns discovery
        every { engine.characteristicNotifications } returns notifications
        coEvery { client.discoverServices(peripheral, any()) } coAnswers {
            discovery.emit(ServiceDiscoveryUpdate(peripheral, ServiceDiscoveryPhase.ServicesDiscovered))
        }
        val connection = BookooScaleConnection(client, BleScanCoordinator(client, backgroundScope), backgroundScope)
        val requester = CoroutineScope(backgroundScope.coroutineContext + Job())
        connection.connect(peripheral.uuid, requester)
        requester.cancel()
        val packet = "030B000000012B0000002B00023200460100007E".chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        notifications.emit(CharacteristicNotification(peripheral, weight, packet))
        runCurrent()
        assertTrue(connection.state.value.receiving)
        assertTrue(connection.state.value.reading != null)
        linkState = BluetoothPeripheralState.Disconnected
        advanceTimeBy(1_001)
        runCurrent()
        assertTrue(connection.state.value.connecting)
        advanceTimeBy(1_001)
        runCurrent()
        assertTrue(connection.state.value.connected)
        coVerify(exactly = 2) { client.connect(peripheral, false) }
        connection.disconnect()
        advanceTimeBy(5_000)
        runCurrent()
        assertFalse(connection.state.value.connected)
        coVerify(exactly = 2) { client.connect(peripheral, false) }
    }

    @Test
    fun `connected scale without readings reconnects once instead of staying falsely ready`() = runTest {
        val client = mockk<BlueFalcon>(relaxed = true)
        val engine = mockk<BlueFalconEngine>(relaxed = true)
        val peripheral = mockk<BluetoothPeripheral>(relaxed = true)
        val service = mockk<BluetoothService>(relaxed = true)
        val weight = mockk<BluetoothCharacteristic>(relaxed = true)
        val command = mockk<BluetoothCharacteristic>(relaxed = true)
        val discovery = MutableSharedFlow<ServiceDiscoveryUpdate>()
        every { peripheral.name } returns "BOOKOO"
        every { peripheral.uuid } returns "AA:BB:CC:DD:EE:FF"
        every { peripheral.services } returns listOf(service)
        every { peripheral.characteristics } returns listOf(weight, command)
        every { service.uuid } returns "0ffe".toUuid()
        every { weight.uuid } returns "ff11".toUuid()
        every { command.uuid } returns "ff12".toUuid()
        every { client.peripherals } returns MutableStateFlow(setOf(peripheral))
        every { client.connectionState(peripheral) } returns BluetoothPeripheralState.Connected
        every { client.engine } returns engine
        every { client.serviceDiscoveryUpdates } returns discovery
        every { engine.characteristicNotifications } returns MutableSharedFlow()
        coEvery { client.discoverServices(peripheral, any()) } coAnswers {
            discovery.emit(ServiceDiscoveryUpdate(peripheral, ServiceDiscoveryPhase.ServicesDiscovered))
        }
        val connection = BookooScaleConnection(client, BleScanCoordinator(client, backgroundScope), backgroundScope)

        connection.connect(peripheral.uuid)
        assertTrue(connection.state.value.connected)
        assertFalse(connection.state.value.receiving)

        advanceTimeBy(7_001)
        runCurrent()

        assertTrue(connection.state.value.connected)
        assertFalse(connection.state.value.receiving)
        coVerify(exactly = 2) { client.connect(peripheral, false) }
        connection.disconnect()
    }

    @Test
    fun `multiple discovered scales do not connect to an arbitrary device`() = runTest {
        val client = mockk<BlueFalcon>(relaxed = true)
        val first = mockk<BluetoothPeripheral>(relaxed = true)
        val second = mockk<BluetoothPeripheral>(relaxed = true)
        every { first.name } returns "BOOKOO One"
        every { first.uuid } returns "AA:BB:CC:DD:EE:01"
        every { second.name } returns "BOOKOO Two"
        every { second.uuid } returns "AA:BB:CC:DD:EE:02"
        every { client.peripherals } returns MutableStateFlow(setOf(first, second))
        val connection = BookooScaleConnection(client, BleScanCoordinator(client, backgroundScope), backgroundScope)

        val failure = assertFailsWith<IllegalStateException> { connection.connectBookoo() }

        assertTrue(failure.message.orEmpty().contains("Multiple BOOKOO"))
        coVerify(exactly = 0) { client.connect(any(), any()) }
        assertFalse(connection.state.value.connected)
        assertFalse(connection.state.value.connecting)
    }

    @Test
    fun `connection timeout retries same scale then reports failure`() = runTest {
        val client = mockk<BlueFalcon>(relaxed = true)
        val peripheral = mockk<BluetoothPeripheral>(relaxed = true)
        every { peripheral.name } returns "BOOKOO"
        every { peripheral.uuid } returns "AA:BB:CC:DD:EE:FF"
        every { client.peripherals } returns MutableStateFlow(setOf(peripheral))
        every { client.connectionState(peripheral) } returns BluetoothPeripheralState.Disconnected
        val connection = BookooScaleConnection(client, BleScanCoordinator(client, backgroundScope), backgroundScope)

        val failure = assertFailsWith<IllegalStateException> { connection.connectBookoo() }

        assertTrue(failure.message.orEmpty().contains("Timed out"))
        coVerify(exactly = 2) { client.connect(peripheral, false) }
        assertFalse(connection.state.value.connected)
        assertFalse(connection.state.value.connecting)
    }
}
