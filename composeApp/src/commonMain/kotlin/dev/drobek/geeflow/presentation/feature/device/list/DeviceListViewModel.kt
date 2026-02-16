package dev.drobek.geeflow.presentation.feature.device.list

import androidx.lifecycle.viewModelScope
import dev.drobek.geeflow.domain.device.usecase.DeleteDeviceUseCase
import dev.drobek.geeflow.domain.device.usecase.GetDevicesUseCase
import dev.drobek.geeflow.domain.user.usecase.GetSelectedUserUseCase
import dev.drobek.geeflow.domain.user.usecase.SetFavoriteDeviceUseCase
import dev.drobek.geeflow.presentation.feature.device.list.DeviceListEvent.AddDeviceClicked
import dev.drobek.geeflow.presentation.feature.device.list.DeviceListEvent.BackClicked
import dev.drobek.geeflow.presentation.feature.device.list.DeviceListEvent.DeviceClicked
import dev.drobek.geeflow.presentation.feature.device.list.DeviceListEvent.DeviceRemoveClicked
import dev.drobek.geeflow.presentation.feature.device.list.DeviceListEvent.DeviceSetAsDefaultClicked
import dev.drobek.geeflow.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class DeviceListViewModel(
    getDevicesUseCase: GetDevicesUseCase,
    private val deleteDeviceUseCase: DeleteDeviceUseCase,
    private val setFavoriteDeviceUseCase: SetFavoriteDeviceUseCase,
    getSelectedUserUseCase: GetSelectedUserUseCase
) : BaseViewModel<DeviceListViewState, DeviceLitViewModelEvent>(DeviceListViewState()) {

    init {
        combine(
            getDevicesUseCase(),
            getSelectedUserUseCase()
        ) { domainDevices, selectedUser ->
            modify {
                copy(
                    devices = domainDevices.map { domainDevice ->
                        DeviceListViewState.Device(
                            id = domainDevice.serialNumber,
                            name = domainDevice.name,
                            favourite = selectedUser?.favoriteDeviceSerialNumber == domainDevice.serialNumber
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
        is AddDeviceClicked -> emitEvent(Navigation.AddDevice)
        is BackClicked -> emitEvent(Navigation.Back)
        is DeviceClicked -> Unit // TODO emitEvent(Navigation.Dashboard(event.device))
        is DeviceRemoveClicked -> deleteDeviceUseCase(event.device.id)
        is DeviceSetAsDefaultClicked -> {
            viewState.value.user?.let { user ->
                setFavoriteDeviceUseCase(user.id, event.device.id)
            }
        }
    }
}
