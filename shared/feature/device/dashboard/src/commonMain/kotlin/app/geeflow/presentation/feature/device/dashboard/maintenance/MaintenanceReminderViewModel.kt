package app.geeflow.presentation.feature.device.dashboard.maintenance

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launchCatching
import app.geeflow.data.device.model.CleaningType
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.MaintenanceSettings
import app.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import app.geeflow.domain.device.usecase.ObserveMaintenanceSettingsUseCase
import app.geeflow.domain.device.usecase.SkipMaintenanceReminderUseCase
import app.geeflow.domain.device.usecase.maintenanceDay
import kotlinx.coroutines.flow.combine
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class MaintenanceReminderViewModel(
    @InjectedParam private val deviceId: Long,
    observeMaintenance: ObserveMaintenanceSettingsUseCase,
    observeDevice: ObserveDeviceStateUseCase,
    private val skipReminder: SkipMaintenanceReminderUseCase,
) : BaseViewModel<MaintenanceReminderViewState, Unit>(MaintenanceReminderViewState()) {
    private var settings: MaintenanceSettings? = null
    private var today = maintenanceDay()
    private var connected = false

    init {
        launchCatching {
            combine(observeMaintenance(deviceId), observeDevice(deviceId)) { saved, state -> saved to state }
                .collect { (saved, state) ->
                    settings = saved
                    val nowConnected = state.connectionStatus == DeviceState.ConnectionStatus.Connected
                    if (nowConnected && !connected) today = maintenanceDay()
                    connected = nowConnected
                    modify {
                        copy(
                            dueTypes = saved.dueTypes(today),
                            eligible = state.connectionStatus == DeviceState.ConnectionStatus.Connected &&
                                state.brewStatus == DeviceState.BrewStatus.Idle && !state.waterLevelAlarm,
                        )
                    }
                }
        }
    }

    fun resumed() {
        today = maintenanceDay()
        modify { copy(dueTypes = settings?.dueTypes(today).orEmpty()) }
    }

    fun skip(types: Set<CleaningType>) {
        if (viewState.value.busy) return
        modify { copy(busy = true) }
        launchCatching(onError = {
            modify { copy(busy = false) }
            emitEvent(Unit)
        }) {
            skipReminder(deviceId, types)
            modify { copy(busy = false, dueTypes = dueTypes - types) }
        }
    }
}
