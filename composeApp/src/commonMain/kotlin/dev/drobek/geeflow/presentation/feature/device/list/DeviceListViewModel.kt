package dev.drobek.geeflow.presentation.feature.device.list

import androidx.lifecycle.viewModelScope
import dev.drobek.geeflow.domain.device.usecase.GetDevicesUseCase
import dev.drobek.geeflow.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class DeviceListViewModel(
    getDevicesUseCase: GetDevicesUseCase
) : BaseViewModel<DeviceListViewState, DeviceLitViewModelEvent>(DeviceListViewState()) {

    init {
        getDevicesUseCase()
            .onEach { domainDevices ->
                modify {
                    copy(
                        devices = domainDevices.map { domainDevice ->
                            DeviceListViewState.Device(
                                id = domainDevice.serialNumber,
                                name = domainDevice.name
                            )
                        }
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun handleEvent(event: DeviceListEvent) = when (event) {
        is DeviceListEvent.AddDeviceClicked -> emitEvent(Navigation.AddDevice)
        is DeviceListEvent.BackClicked -> emitEvent(Navigation.Back)
        is DeviceListEvent.DeviceClicked -> emitEvent(Navigation.DeviceDetails(event.device.id))
    }
}
