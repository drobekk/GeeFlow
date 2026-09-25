package app.geeflow.presentation.feature.device.list

import androidx.lifecycle.viewModelScope
import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launchCatching
import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.domain.device.usecase.DeleteDeviceUseCase
import app.geeflow.domain.device.usecase.DisconnectCurrentDeviceUseCase
import app.geeflow.domain.device.usecase.GetCurrentDeviceIdUseCase
import app.geeflow.domain.device.usecase.ObserveDevicesUseCase
import app.geeflow.domain.device.usecase.RenameDeviceUseCase
import app.geeflow.domain.user.usecase.GetSelectedUserUseCase
import app.geeflow.domain.user.usecase.SetFavoriteDeviceUseCase
import app.geeflow.navigation.NavEvent
import app.geeflow.navigation.destination.AddDevice
import app.geeflow.navigation.destination.DeviceDashboard
import app.geeflow.presentation.feature.device.list.DeviceListEvent.AddDeviceClicked
import app.geeflow.presentation.feature.device.list.DeviceListEvent.BackClicked
import app.geeflow.presentation.feature.device.list.DeviceListEvent.DeviceClicked
import app.geeflow.presentation.feature.device.list.DeviceListEvent.DeviceRemoveClicked
import app.geeflow.presentation.feature.device.list.DeviceListEvent.DeviceRenameClicked
import app.geeflow.presentation.feature.device.list.DeviceListEvent.DeviceRenameConfirmed
import app.geeflow.presentation.feature.device.list.DeviceListEvent.DeviceSetAsDefaultClicked
import app.geeflow.presentation.feature.device.list.DeviceListEvent.DialogDismissed
import app.geeflow.presentation.feature.device.list.DeviceListViewState.Dialog
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class DeviceListViewModel(
    observeDevicesUseCase: ObserveDevicesUseCase,
    private val deleteDeviceUseCase: DeleteDeviceUseCase,
    private val disconnectCurrentDevice: DisconnectCurrentDeviceUseCase,
    private val getCurrentDeviceId: GetCurrentDeviceIdUseCase,
    private val setFavoriteDeviceUseCase: SetFavoriteDeviceUseCase,
    private val renameDeviceUseCase: RenameDeviceUseCase,
    getSelectedUserUseCase: GetSelectedUserUseCase,
) : BaseViewModel<DeviceListViewState, Unit>(DeviceListViewState()) {

    init {
        combine(
            observeDevicesUseCase(),
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

        is DeviceRemoveClicked -> launchCatching {
            deleteDeviceUseCase(event.device.id)
            navigate(NavEvent.Remove(DeviceDashboard(event.device.id)))
        }

        is DeviceRenameClicked -> modify { copy(dialog = Dialog.Rename(event.device)) }

        is DeviceRenameConfirmed -> {
            modify { copy(dialog = null) }
            launchCatching {
                val trimmedName = event.name.trim()
                if (trimmedName.isNotBlank()) {
                    renameDeviceUseCase(event.device.id, trimmedName)
                }
            }
        }

        is DialogDismissed -> modify { copy(dialog = null) }

        is DeviceSetAsDefaultClicked -> {
            viewState.value.user?.let { user ->
                setFavoriteDeviceUseCase(user.id, event.device.id)
            }
        }
    }
}
