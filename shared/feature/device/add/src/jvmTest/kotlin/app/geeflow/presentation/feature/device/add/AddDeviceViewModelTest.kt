package app.geeflow.presentation.feature.device.add

import androidx.lifecycle.viewModelScope
import app.geeflow.core.presentation.Platform
import app.geeflow.data.device.DiscoveredBleDevice
import app.geeflow.data.device.NearbyDevicesController
import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.data.device.model.DeviceManufacturer
import app.geeflow.data.device.model.SupportedDevice
import app.geeflow.domain.device.usecase.AddDeviceUseCase
import app.geeflow.domain.device.usecase.ParseDeviceQrCodeUseCase
import app.geeflow.platform.permissions.DeniedException
import app.geeflow.platform.permissions.PermissionBluetoothScan
import app.geeflow.platform.permissions.PermissionsController
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AddDeviceViewModelTest {
    @Test
    fun `manual serial scan waits for selection confirmation and existing warning before adding`() = runTest {
        withFixture {
            startManualSearch()
            runCurrent()
            discovered.value = setOf(machine())
            runCurrent()
            assertEquals(1, vm.viewState.value.manual.devices.size)
            verify(exactly = 0) { add(any()) }
            vm.handleEvent(AddDeviceEvent.ManualDeviceClicked("live-id"))
            val confirmation = assertIs<AddDeviceViewState.Dialog.ManualConfirmation>(vm.viewState.value.dialog)
            assertEquals("LITA BA · AB00000001", confirmation.device.name)
            assertEquals(DeviceConnection.Ble("live-id", "AA:BB:CC:DD:EE:FF"), confirmation.device.connection)
            assertFalse(confirmation.serialMatches)
            vm.handleEvent(AddDeviceEvent.ConfirmManualDeviceClicked)
            assertIs<AddDeviceViewState.Dialog.ExperimentalWarning>(vm.viewState.value.dialog)
            verify(exactly = 0) { add(any()) }
            vm.handleEvent(AddDeviceEvent.AcceptWarningClicked)
            verify(exactly = 1) {
                add(match { it.modelId == SupportedDevice.LitaBA.id && it.name == "LITA BA · AB00000001" })
            }
        }
    }

    @Test
    fun `invalid manual fields do not start a Bluetooth scan or add a machine`() = runTest {
        withFixture {
            vm.handleEvent(AddDeviceEvent.SearchManualDeviceClicked)
            assertEquals(AddDeviceViewState.ManualError.SerialRequired, vm.viewState.value.manual.error)
            vm.handleEvent(AddDeviceEvent.SerialNumberChanged("AB00000001"))
            vm.handleEvent(AddDeviceEvent.BluetoothAddressChanged("invalid"))
            vm.handleEvent(AddDeviceEvent.SearchManualDeviceClicked)
            runCurrent()
            assertEquals(AddDeviceViewState.ManualError.InvalidAddress, vm.viewState.value.manual.error)
            verify(exactly = 0) { nearby.startManualScanning(any()) }
            verify(exactly = 0) { add(any()) }
        }
    }

    @Test
    fun `permission denial leaves a retryable manual page with no machine added`() = runTest {
        withFixture {
            coEvery { permissions.providePermission(any()) } throws DeniedException(PermissionBluetoothScan)
            startManualSearch()
            runCurrent()
            assertEquals(AddDeviceViewState.ManualError.PermissionDenied, vm.viewState.value.manual.error)
            assertFalse(vm.viewState.value.manual.searching)
            verify(exactly = 0) { nearby.startManualScanning(any()) }
            verify(exactly = 0) { add(any()) }
        }
    }

    @Test
    fun `search has a finite timeout and can be retried`() = runTest {
        withFixture {
            startManualSearch()
            runCurrent()
            assertTrue(vm.viewState.value.manual.searching)
            advanceTimeBy(20_001)
            runCurrent()
            assertFalse(vm.viewState.value.manual.searching)
            assertTrue(vm.viewState.value.manual.searchStarted)
            vm.handleEvent(AddDeviceEvent.SearchManualDeviceClicked)
            runCurrent()
            assertTrue(vm.viewState.value.manual.searching)
            verify(exactly = 2) { nearby.startManualScanning(any()) }
        }
    }

    @Test
    fun `editing serial cancels pending permission work and clears stale results`() = runTest {
        withFixture {
            coEvery { permissions.providePermission(any()) } coAnswers {
                delay(100)
                Unit
            }
            startManualSearch()
            runCurrent()
            vm.handleEvent(AddDeviceEvent.SerialNumberChanged("AB00000002"))
            advanceTimeBy(101)
            runCurrent()
            assertFalse(vm.viewState.value.manual.searching)
            assertTrue(vm.viewState.value.manual.devices.isEmpty())
            verify(exactly = 0) { nearby.startManualScanning(any()) }
        }
    }

    @Test
    fun `switching to nearby cancels old manual timeout without stopping new search`() = runTest {
        withFixture {
            startManualSearch()
            runCurrent()
            vm.handleEvent(AddDeviceEvent.ShowNearbyDevicesClicked)
            runCurrent()
            clearMocks(nearby, answers = false)
            advanceTimeBy(20_001)
            runCurrent()
            assertIs<AddDeviceViewState.Method.NearbyDevices>(vm.viewState.value.method)
            verify(exactly = 0) { nearby.stopScanning() }
        }
    }

    @Test
    fun `known model mismatch and an address that was not found cannot add a machine`() = runTest {
        withFixture {
            startManualSearch()
            runCurrent()
            discovered.value = setOf(machine(SupportedDevice.LitaLR))
            runCurrent()
            vm.handleEvent(AddDeviceEvent.ManualDeviceClicked("live-id"))
            assertEquals(null, vm.viewState.value.dialog)
            vm.handleEvent(AddDeviceEvent.ManualDeviceClicked("missing-id"))
            assertEquals(null, vm.viewState.value.dialog)
            verify(exactly = 0) { add(any()) }
        }
    }

    @Test
    fun `changing model resets results and QR path still uses normal model warning`() = runTest {
        withFixture {
            startManualSearch()
            runCurrent()
            vm.handleEvent(AddDeviceEvent.ManualModelClicked)
            vm.handleEvent(AddDeviceEvent.ModelSelected(SupportedDevice.LitaLA.id))
            assertEquals(SupportedDevice.LitaLA.id, vm.viewState.value.manual.modelId)
            assertFalse(vm.viewState.value.manual.searchStarted)
            vm.handleEvent(AddDeviceEvent.ShowQrCodeScannerClicked)
            vm.handleEvent(
                AddDeviceEvent.QrCodeScanned("https://machine/?data={\"blemac\":\"aabbccddeeff\",\"name\":\"LITA BA\"}")
            )
            assertNotNull(vm.viewState.value.dialog)
            assertIs<AddDeviceViewState.Dialog.ExperimentalWarning>(vm.viewState.value.dialog)
        }
    }

    @Test
    fun `manual entry preserves desktop QR unavailability`() = runTest {
        withFixture(Platform.Type.Desktop) {
            assertFalse(vm.viewState.value.qrCodeAvailable)
            assertIs<AddDeviceViewState.Method.Manual>(vm.viewState.value.method)
        }
    }

    private suspend fun TestScope.withFixture(
        platformType: Platform.Type = Platform.Type.Android,
        block: suspend Fixture.() -> Unit,
    ) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fixture = Fixture(platformType)
        try {
            runCurrent()
            fixture.vm.handleEvent(AddDeviceEvent.ShowManualEntryClicked)
            clearMocks(fixture.nearby, answers = false)
            fixture.block()
        } finally {
            fixture.vm.viewModelScope.coroutineContext[Job]?.cancelAndJoin()
            Dispatchers.resetMain()
        }
    }

    private class Fixture(platformType: Platform.Type) {
        val permissions = mockk<PermissionsController>(relaxed = true)
        val add = mockk<AddDeviceUseCase>()
        val nearby = mockk<NearbyDevicesController>(relaxed = true)
        val discovered = MutableStateFlow<Set<DiscoveredBleDevice>>(emptySet())
        val vm: AddDeviceViewModel

        init {
            every { nearby.discoveredDevices } returns discovered
            every { add(any()) } returns 7L
            vm = AddDeviceViewModel(
                object : Platform {
                    override val type = platformType
                },
                permissions,
                add,
                ParseDeviceQrCodeUseCase(),
                nearby
            )
        }

        fun startManualSearch() {
            vm.handleEvent(AddDeviceEvent.SerialNumberChanged("AB00000001"))
            vm.handleEvent(AddDeviceEvent.SearchManualDeviceClicked)
        }

        fun machine(model: SupportedDevice? = null) = DiscoveredBleDevice(
            device = Device(
                name = "Wendougee",
                manufacturer = DeviceManufacturer.WENDOUGEE,
                connection = DeviceConnection.Ble("live-id", "AA:BB:CC:DD:EE:FF"),
                modelId = model?.id,
                model = model?.model.orEmpty(),
            ),
            peripheral = mockk { every { name } returns "Wendougee_AABBCCDDEEFF" },
        )
    }
}
