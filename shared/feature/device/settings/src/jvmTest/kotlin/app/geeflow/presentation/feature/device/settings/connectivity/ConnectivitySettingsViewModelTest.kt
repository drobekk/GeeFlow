package app.geeflow.presentation.feature.device.settings.connectivity

import androidx.lifecycle.viewModelScope
import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.SingleDoseGrinder
import app.geeflow.data.device.model.SmartScale
import app.geeflow.domain.device.usecase.ConnectSingleDoseGrinderUseCase
import app.geeflow.domain.device.usecase.ConnectSmartScaleUseCase
import app.geeflow.domain.device.usecase.DisconnectSingleDoseGrinderUseCase
import app.geeflow.domain.device.usecase.DisconnectSmartScaleUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import app.geeflow.domain.device.usecase.ObserveFoundScalesUseCase
import app.geeflow.domain.device.usecase.ObserveFoundSingleDoseGrindersUseCase
import app.geeflow.domain.device.usecase.RequestSingleDoseGrinderListUseCase
import app.geeflow.domain.device.usecase.RequestSmartScaleListUseCase
import app.geeflow.domain.device.usecase.SetSingleDoseGrinderConnectivityUseCase
import app.geeflow.domain.device.usecase.SetSmartScaleConnectivityUseCase
import app.geeflow.presentation.feature.device.settings.ConnectivitySettings
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivityAccessoryType.SingleDose
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivityAccessoryType.SmartScale
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.ConnectionClicked
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.RescanClicked
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.SearchToggled
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.TypeSelected
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsViewState.AccessoryConnectionStatus
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivitySettingsViewModelTest {
    @Test
    fun `when enabling search in each tab then only the selected integration receives the command`() = runTest {
        withFixture {
            runCurrent()

            viewModel.handleEvent(SearchToggled(true))
            runCurrent()
            viewModel.handleEvent(TypeSelected(SingleDose))
            viewModel.handleEvent(SearchToggled(true))
            runCurrent()

            coVerify(exactly = 1) { controller.setSmartScaleConnectivity(true) }
            coVerify(exactly = 1) { controller.setSingleDoseGrinderConnectivity(true) }
        }
    }

    @Test
    fun `when switching tabs during connection then pending grinder remains in its own tab`() = runTest {
        withFixture {
            runCurrent()
            viewModel.handleEvent(TypeSelected(SingleDose))

            viewModel.handleEvent(ConnectionClicked("Milo Demo"))
            viewModel.handleEvent(TypeSelected(SmartScale))
            runCurrent()

            assertEquals("BOOKOO_Test", viewModel.viewState.value.selectedAccessory.devices.single().name)
            assertEquals(
                AccessoryConnectionStatus.Disconnected,
                viewModel.viewState.value.scale.devices.single().connectionStatus,
            )
            assertEquals(
                AccessoryConnectionStatus.Connecting,
                viewModel.viewState.value.grinder.devices.single().connectionStatus,
            )
            coVerify(exactly = 1) { controller.connectSingleDoseGrinder("Milo Demo") }
            coVerify(exactly = 0) { controller.connectSmartScale(any()) }
        }
    }

    @Test
    fun `when grinder connection is confirmed then its button changes to connected`() = runTest {
        withFixture {
            runCurrent()
            viewModel.handleEvent(TypeSelected(SingleDose))
            viewModel.handleEvent(ConnectionClicked("Milo Demo"))
            runCurrent()

            state.value = state.value.copy(singleDoseGrinder = SingleDoseGrinder("Milo Demo", true))
            runCurrent()

            assertEquals(
                AccessoryConnectionStatus.Connected,
                viewModel.viewState.value.grinder.devices.single().connectionStatus,
            )
        }
    }

    @Test
    fun `when rescanning and disconnecting grinder then scale commands are not sent`() = runTest {
        withFixture {
            state.value = state.value.copy(singleDoseGrinder = SingleDoseGrinder("Milo Demo", true))
            runCurrent()
            viewModel.handleEvent(TypeSelected(SingleDose))

            viewModel.handleEvent(RescanClicked)
            viewModel.handleEvent(ConnectionClicked("Milo Demo"))
            runCurrent()

            coVerify(exactly = 1) { controller.requestSingleDoseGrinderList() }
            coVerify(exactly = 1) { controller.disconnectSingleDoseGrinder() }
            coVerify(exactly = 0) { controller.requestSmartScaleList() }
            coVerify(exactly = 0) { controller.disconnectSmartScale() }
        }
    }

    private suspend fun TestScope.withFixture(block: suspend Fixture.() -> Unit) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fixture = Fixture()
        try {
            fixture.block()
        } finally {
            fixture.viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    private class Fixture {
        val controller = mockk<DeviceController>(relaxUnitFun = true)
        val state = MutableStateFlow(
            DeviceState(
                connectionStatus = DeviceState.ConnectionStatus.Connected,
                smartScaleEnabled = true,
                singleDoseGrinderEnabled = true,
            ),
        )
        private val provider = mockk<DeviceControllerProvider>()
        private val observeState = mockk<ObserveDeviceStateUseCase>()

        init {
            every { provider.getController(9) } returns controller
            every { controller.deviceState } returns state
            every { controller.foundScales } returns MutableStateFlow(listOf(SmartScale("BOOKOO_Test", false)))
            every { controller.foundSingleDoseGrinders } returns
                MutableStateFlow(listOf(SingleDoseGrinder("Milo Demo", false)))
            every { observeState(9) } returns state
        }

        val viewModel = ConnectivitySettingsViewModel(
            arguments = ConnectivitySettings(9),
            observeDeviceState = observeState,
            observeFoundScales = ObserveFoundScalesUseCase(provider),
            setSmartScaleConnectivity = SetSmartScaleConnectivityUseCase(provider),
            requestSmartScaleList = RequestSmartScaleListUseCase(provider),
            connectSmartScale = ConnectSmartScaleUseCase(provider),
            disconnectSmartScale = DisconnectSmartScaleUseCase(provider),
            observeFoundGrinders = ObserveFoundSingleDoseGrindersUseCase(provider),
            setGrinderConnectivity = SetSingleDoseGrinderConnectivityUseCase(provider),
            requestGrinderList = RequestSingleDoseGrinderListUseCase(provider),
            connectGrinder = ConnectSingleDoseGrinderUseCase(provider),
            disconnectGrinder = DisconnectSingleDoseGrinderUseCase(provider),
        )
    }
}
