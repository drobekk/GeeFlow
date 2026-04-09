package dev.drobek.geeflow.presentation.feature.device.dashboard.quickmaintenance

import dev.drobek.geeflow.domain.device.usecase.GetCleaningStatusUseCase
import dev.drobek.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import dev.drobek.geeflow.domain.device.usecase.StartCleaningUseCase
import dev.drobek.geeflow.domain.device.usecase.StopCleaningUseCase
import dev.drobek.geeflow.navigation.NavEvent.Back
import dev.drobek.geeflow.navigation.NavEvent.To
import dev.drobek.geeflow.navigation.destination.DeviceSettings
import dev.drobek.geeflow.navigation.destination.DeviceSettings.EntryPoint
import dev.drobek.geeflow.presentation.feature.device.dashboard.QuickMaintenance
import dev.drobek.geeflow.presentation.feature.device.dashboard.quickmaintenance.QuickMaintenanceEvent.CloseClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.quickmaintenance.QuickMaintenanceEvent.MoreSettingsClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.quickmaintenance.QuickMaintenanceEvent.ToggleCleaningClicked
import dev.drobek.geeflow.core.presentation.BaseViewModel
import dev.drobek.geeflow.core.presentation.launch
import kotlinx.coroutines.flow.combine
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class QuickMaintenanceViewModel(
    @InjectedParam private val arguments: QuickMaintenance,
    private val getCleaningStatus: GetCleaningStatusUseCase,
    private val observeDeviceState: ObserveDeviceStateUseCase,
    private val startCleaning: StartCleaningUseCase,
    private val stopCleaning: StopCleaningUseCase
) : BaseViewModel<QuickMaintenanceViewState, QuickMaintenanceViewModelEvent>(QuickMaintenanceViewState()) {

    init {
        launch {
            combine(
                getCleaningStatus(arguments.deviceId),
                observeDeviceState(arguments.deviceId)
            ) { status, state -> status to state }
                .collect { (status, state) ->
                    modify {
                        copy(
                            waterLevelAlarm = state.waterLevelAlarm,
                            isCleaning = status.inProgress,
                            flushProgress = QuickMaintenanceViewState.Progress(status.flush.current, status.flush.target),
                            restProgress = QuickMaintenanceViewState.Progress(status.rest.current, status.rest.target),
                            cycleProgress = QuickMaintenanceViewState.Progress(status.cycle.current, status.cycle.target)
                        )
                    }
                }
        }
    }

    fun handleEvent(event: QuickMaintenanceEvent) = when (event) {
        ToggleCleaningClicked -> launch {
            if (viewState.value.isCleaning) stopCleaning(arguments.deviceId)
            else startCleaning(arguments.deviceId)
        }

        CloseClicked -> navigate(Back)
        MoreSettingsClicked -> {
            navigate(Back)
            navigate(To(DeviceSettings(arguments.deviceId, EntryPoint.Maintenance)))
        }
    }
}
