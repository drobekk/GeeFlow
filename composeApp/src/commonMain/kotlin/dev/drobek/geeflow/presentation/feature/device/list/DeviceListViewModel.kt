package dev.drobek.geeflow.presentation.feature.device.list

import androidx.lifecycle.viewModelScope
import dev.drobek.geeflow.core.presentation.BaseViewModel
import dev.drobek.geeflow.domain.device.usecase.DeleteDeviceUseCase
import dev.drobek.geeflow.domain.device.usecase.DisconnectCurrentDeviceUseCase
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
    private val setFavoriteDeviceUseCase: SetFavoriteDeviceUseCase,
    getSelectedUserUseCase: GetSelectedUserUseCase
) : BaseViewModel<DeviceListViewState, Unit>(DeviceListViewState()) {

    init {
        combine(
            getDevicesUseCase(),
            getSelectedUserUseCase()
        ) { domainDevices, selectedUser ->
            modify {
                copy(
                    devices = domainDevices.map { domainDevice ->
                        DeviceListViewState.Device(
                            id = domainDevice.macAddress,
                            name = domainDevice.name,
                            favourite = selectedUser?.favoriteDeviceMacAddress == domainDevice.macAddress
                        )
                    },
                    user = selectedUser?.let {
                        DeviceListViewState.User(
                            id = selectedUser.id,
                            name = selectedUser.name
                        )
                    }
                )
            }
        }.launchIn(viewModelScope)
    }

    fun handleEvent(event: DeviceListEvent) = when (event) {
        is AddDeviceClicked -> navigate(NavEvent.To(AddDevice))
        is BackClicked -> navigate(NavEvent.Back)
        is DeviceClicked -> {
            disconnectCurrentDevice()
            navigate(NavEvent.To(DeviceDashboard(event.device.id)))
        }

        is DeviceRemoveClicked -> deleteDeviceUseCase(event.device.id)
        is DeviceSetAsDefaultClicked -> {
            viewState.value.user?.let { user ->
                setFavoriteDeviceUseCase(user.id, event.device.id)
            }
        }
    }
}
