package app.geeflow.presentation.feature.device.dashboard.quickmaintenance

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.DeviceState.ConnectionStatus
import app.geeflow.domain.device.usecase.CleaningStatus
import app.geeflow.domain.device.usecase.GetCleaningStatusUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import app.geeflow.domain.device.usecase.StartCleaningUseCase
import app.geeflow.domain.device.usecase.StopCleaningUseCase
import app.geeflow.navigation.NavEvent.Back
import app.geeflow.navigation.NavEvent.To
import app.geeflow.navigation.destination.DeviceSettings
import app.geeflow.navigation.destination.DeviceSettings.EntryPoint
import app.geeflow.presentation.feature.device.dashboard.QuickMaintenance
import app.geeflow.presentation.feature.device.dashboard.quickmaintenance.QuickMaintenanceEvent.CloseClicked
import app.geeflow.presentation.feature.device.dashboard.quickmaintenance.QuickMaintenanceEvent.MoreSettingsClicked
import app.geeflow.presentation.feature.device.dashboard.quickmaintenance.QuickMaintenanceEvent.ToggleCleaningClicked
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class QuickMaintenanceViewModel(
    @InjectedParam private val arguments: QuickMaintenance,
    private val getCleaningStatus: GetCleaningStatusUseCase,
    private val observeDeviceState: ObserveDeviceStateUseCase,
    private val startCleaning: StartCleaningUseCase,
    private val stopCleaning: StopCleaningUseCase,
) : BaseViewModel<QuickMaintenanceViewState, QuickMaintenanceViewModelEvent>(QuickMaintenanceViewState()) {

    private var stateJob: Job? = null
    private var cleaningJob: Job? = null

    init {
        stateJob = launch {
            combine(
                flow = cleaningStatus(),
                flow2 = observeDeviceState(arguments.deviceId),
                transform = { status, state -> status to state },
            ).collect { (status, state) -> maintenanceStateChanged(status, state) }
        }
        cleaningJob = launch {
            if (!cleaningStatus().first().inProgress) return@launch
            awaitCleaningEnd()
        }
    }

    fun handleEvent(event: QuickMaintenanceEvent) = when (event) {
        ToggleCleaningClicked -> toggleCleaning()
        CloseClicked -> close()
        is MoreSettingsClicked -> {
            close()
            navigate(To(DeviceSettings(arguments.deviceId, if (event.isExpanded) EntryPoint.Maintenance else null)))
        }
    }

    private fun toggleCleaning() {
        if (viewState.value.isCleaning) {
            cleaningJob?.cancel()
            launch { stopCleaning(arguments.deviceId) }
            return
        }
        cleaningJob = launch {
            startCleaning(arguments.deviceId)
            cleaningStatus().first { it.inProgress }
            awaitCleaningEnd()
        }
    }

    private suspend fun awaitCleaningEnd() {
        cleaningStatus().first { !it.inProgress }
        close()
    }

    private fun cleaningStatus() = getCleaningStatus(arguments.deviceId)

    private fun maintenanceStateChanged(status: CleaningStatus, state: DeviceState) {
        if (state.connectionStatus != ConnectionStatus.Connected) {
            close()
            return
        }

        modify {
            copy(
                waterLevelAlarm = state.waterLevelAlarm,
                isCleaning = status.inProgress,
                flushProgress = QuickMaintenanceViewState.Progress(status.flush.current, status.flush.target),
                restProgress = QuickMaintenanceViewState.Progress(status.rest.current, status.rest.target),
                cycleProgress = QuickMaintenanceViewState.Progress(status.cycle.current, status.cycle.target),
            )
        }
    }

    /** Dropping both subscriptions first: a later emission reaching [close] again would pop the dashboard too. */
    private fun close() {
        stateJob?.cancel()
        cleaningJob?.cancel()
        navigate(Back)
    }
}
