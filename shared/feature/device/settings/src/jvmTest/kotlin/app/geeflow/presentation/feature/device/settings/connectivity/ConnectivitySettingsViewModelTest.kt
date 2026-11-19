package app.geeflow.presentation.feature.device.settings.connectivity

import androidx.lifecycle.viewModelScope
import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.SmartScale
import app.geeflow.domain.device.usecase.ConnectSmartScaleUseCase
import app.geeflow.domain.device.usecase.DisconnectSmartScaleUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import app.geeflow.domain.device.usecase.ObserveFoundScalesUseCase
import app.geeflow.domain.device.usecase.RequestSmartScaleListUseCase
import app.geeflow.domain.device.usecase.SetSmartScaleConnectivityUseCase
import app.geeflow.presentation.feature.device.settings.ConnectivitySettings
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.ConnectionClicked
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.RescanClicked
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.SearchToggled
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsViewState.AccessoryConnectionStatus
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
    fun `when enabling search then smart scale discovery is enabled`() = runTest {
        withFixture {
            runCurrent()

            viewModel.handleEvent(SearchToggled(true))
            runCurrent()

            coVerify(exactly = 1) { accessories.setSmartScaleConnectivity(true) }
        }
    }

    @Test
    fun `when connecting and disconnecting a scale then its state is reflected`() = runTest {
        withFixture {
            runCurrent()
            viewModel.handleEvent(ConnectionClicked("BOOKOO_Test"))
            runCurrent()
            coVerify(exactly = 1) { accessories.connectSmartScale("BOOKOO_Test") }

            state.value = state.value.copy(smartScale = SmartScale("BOOKOO_Test", true))
            runCurrent()
            assertEquals(
                AccessoryConnectionStatus.Connected,
                viewModel.viewState.value.scale.devices.single().connectionStatus,
            )

            viewModel.handleEvent(ConnectionClicked("BOOKOO_Test"))
            runCurrent()
            coVerify(exactly = 1) { accessories.disconnectSmartScale() }
        }
    }

    @Test
    fun `when rescanning then only smart scales are requested`() = runTest {
        withFixture {
            runCurrent()
            viewModel.handleEvent(RescanClicked)
            runCurrent()

            coVerify(exactly = 1) { accessories.requestSmartScaleList() }
        }
    }

    private suspend fun TestScope.withFixture(block: suspend Fixture.() -> Unit) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fixture = Fixture()
        try {
            fixture.block()
        } finally {
            fixture.viewModel.viewModelScope.coroutineContext[Job]?.cancelAndJoin()
            Dispatchers.resetMain()
        }
    }

    private class Fixture {
        val controller = mockk<DeviceController>(relaxed = true)
        val accessories = controller.accessories
        val state = MutableStateFlow(
            DeviceState(
                connectionStatus = DeviceState.ConnectionStatus.Connected,
                smartScaleEnabled = true,
            ),
        )
        private val provider = mockk<DeviceControllerProvider>()
        private val observeState = mockk<ObserveDeviceStateUseCase>()

        init {
            every { provider.getController(9) } returns controller
            every { provider.observeController(9) } returns flowOf(controller)
            every { controller.deviceState } returns state
            every { controller.foundScales } returns MutableStateFlow(listOf(SmartScale("BOOKOO_Test", false)))
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
        )
    }
}
