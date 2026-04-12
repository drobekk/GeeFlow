package dev.drobek.geeflow.presentation.feature.device.list

import androidx.lifecycle.viewModelScope
import dev.drobek.geeflow.core.presentation.BaseViewModel
import dev.drobek.geeflow.data.device.model.DeviceConnection
import dev.drobek.geeflow.domain.device.usecase.DeleteDeviceUseCase
import dev.drobek.geeflow.domain.device.usecase.DisconnectCurrentDeviceUseCase
import dev.drobek.geeflow.domain.device.usecase.GetCurrentDeviceIdUseCase
import dev.drobek.geeflow.domain.device.usecase.GetDevicesUseCase
import dev.drobek.geeflow.domain.user.usecase.GetSelectedUserUseCase
import dev.drobek.geeflow.domain.user.usecase.SetFavoriteDeviceUseCase
import dev.drobek.geeflow.navigation.NavEvent
import dev.drobek.geeflow.navigation.destination.AddDevice
import dev.drobek.geeflow.navigation.destination.DeviceDashboard
import dev.drobek.geeflow.presentation.feature.device.list.DeviceListEvent.AddDeviceClicked
import dev.drobek.geeflow.presentation.feature.device.list.DeviceListEvent.BackClicked
import dev.drobek.geeflow.presentation.feature.device.list.DeviceListEvent.DeviceClicked
import dev.drobek.geeflow.presentation.feature.device.list.DeviceListEvent.DeviceRemoveClicked
import dev.drobek.geeflow.presentation.feature.device.list.DeviceListEvent.DeviceSetAsDefaultClicked
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class DeviceListViewModel(
    getDevicesUseCase: GetDevicesUseCase,
    private val deleteDeviceUseCase: DeleteDeviceUseCase,
    private val disconnectCurrentDevice: DisconnectCurrentDeviceUseCase,
    private val getCurrentDeviceId: GetCurrentDeviceIdUseCase,
    private val setFavoriteDeviceUseCase: SetFavoriteDeviceUseCase,
    getSelectedUserUseCase: GetSelectedUserUseCase,
) : BaseViewModel<DeviceListViewState, Unit>(DeviceListViewState()) {

    init {
        combine(
            getDevicesUseCase(),
            getSelectedUserUseCase(),
        ) { domainDevices, selectedUser ->
            modify {
                copy(
                    devices = domainDevices.map { domainDevice ->
                        val ble = domainDevice.connection as? DeviceConnection.Ble
                        DeviceListViewState.Device(
                            id = domainDevice.id,
                            name = domainDevice.name,
                            macAddress = ble?.macAddress.orEmpty(),
                            favourite = selectedUser?.favoriteDeviceId == domainDevice.id,
                        )
                    },
                    user = selectedUser?.let {
                        DeviceListViewState.User(
                            id = selectedUser.id,
                            name = selectedUser.name,
                        )
                    },
                )
            }
        }.launchIn(viewModelScope)
    }

    fun handleEvent(event: DeviceListEvent) = when (event) {
        is AddDeviceClicked -> navigate(NavEvent.To(AddDevice))
        is BackClicked -> navigate(NavEvent.Back)
        is DeviceClicked -> {
            if (getCurrentDeviceId() != event.device.id) {
                disconnectCurrentDevice()
            }
            navigate(NavEvent.To(DeviceDashboard(event.device.id)))
        }

        is DeviceRemoveClicked -> {
            deleteDeviceUseCase(event.device.id)
            navigate(NavEvent.Remove(DeviceDashboard(event.device.id)))
        }

        is DeviceSetAsDefaultClicked -> {
            viewState.value.user?.let { user ->
                setFavoriteDeviceUseCase(user.id, event.device.id)
            }
        }
    }
}
