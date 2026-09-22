package app.geeflow.presentation.feature.device.dashboard.quickmaintenance

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.core.presentation.launchCatching
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.DeviceState.ConnectionStatus
import app.geeflow.data.device.model.MaintenanceSettings
import app.geeflow.domain.device.usecase.CleaningStatus
import app.geeflow.domain.device.usecase.GetCleaningStatusUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import app.geeflow.domain.device.usecase.ObserveMaintenanceSettingsUseCase
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
import geeflow.shared.core.ui.generated.resources.Res
import geeflow.shared.core.ui.generated.resources.error_generic
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.milliseconds

@KoinViewModel
internal class QuickMaintenanceViewModel(
    @InjectedParam private val arguments: QuickMaintenance,
    private val getCleaningStatus: GetCleaningStatusUseCase,
    private val observeDeviceState: ObserveDeviceStateUseCase,
    private val observeMaintenance: ObserveMaintenanceSettingsUseCase,
    private val startCleaning: StartCleaningUseCase,
    private val stopCleaning: StopCleaningUseCase,
) : BaseViewModel<QuickMaintenanceViewState, QuickMaintenanceViewModelEvent>(
    QuickMaintenanceViewState(selectedType = arguments.type),
) {

    private var stateJob: Job? = null
    private var cleaningJob: Job? = null

    private var settings: MaintenanceSettings? = null
    private var latestStatus: CleaningStatus? = null
    private var latestState: DeviceState? = null

    init {
        stateJob = launch {
            combine(
                flow = cleaningStatus(),
                flow2 = observeDeviceState(arguments.deviceId),
                flow3 = observeMaintenance(arguments.deviceId),
                transform = { status, state, saved -> Triple(status, state, saved) },
            ).collect { (status, state, saved) ->
                settings = saved
                latestStatus = status
                latestState = state
                maintenanceStateChanged(status, state)
            }
        }
        cleaningJob = launch {
            if (!cleaningStatus().first().inProgress) return@launch
            awaitCleaningEnd()
        }
    }

    fun handleEvent(event: QuickMaintenanceEvent) = when (event) {
        is QuickMaintenanceEvent.TypeSelected -> {
            if (!viewState.value.isStarting && !viewState.value.isCleaning) {
                modify { copy(selectedType = event.type) }
                latestStatus?.let { status -> latestState?.let { maintenanceStateChanged(status, it) } }
            }
            Unit
        }
        ToggleCleaningClicked -> toggleCleaning()
        CloseClicked -> if (!viewState.value.isStarting) close() else Unit
        is MoreSettingsClicked -> {
            close()
            navigate(To(DeviceSettings(arguments.deviceId, if (event.isExpanded) EntryPoint.Maintenance else null)))
        }
    }

    private fun toggleCleaning() {
        if (viewState.value.isStarting) return
        if (viewState.value.isCleaning) {
            cleaningJob?.cancel()
            launch { stopCleaning(arguments.deviceId) }
            return
        }
        if (!viewState.value.canStart) return
        val type = viewState.value.selectedType
        modify { copy(isStarting = true) }
        cleaningJob = launchCatching(onError = {
            modify { copy(isStarting = false) }
            launch { emitEvent(QuickMaintenanceViewModelEvent.ShowSnackbar(getString(Res.string.error_generic))) }
        }) {
            startCleaning(arguments.deviceId, type)
            withTimeout(START_STATUS_TIMEOUT_MILLIS.milliseconds) { cleaningStatus().first { it.inProgress } }
            modify { copy(isStarting = false) }
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

        val program = settings?.program(viewState.value.selectedType)
        modify {
            copy(
                canStart = program != null && state.brewStatus == DeviceState.BrewStatus.Idle && !state.waterLevelAlarm,
                waterLevelAlarm = state.waterLevelAlarm,
                isCleaning = status.inProgress,
                flushProgress = QuickMaintenanceViewState.Progress(
                    current = status.flush.current,
                    target = if (status.inProgress) status.flush.target else program?.flushSeconds ?: 0,
                ),
                restProgress = QuickMaintenanceViewState.Progress(
                    current = status.rest.current,
                    target = if (status.inProgress) status.rest.target else program?.restSeconds ?: 0,
                ),
                cycleProgress = QuickMaintenanceViewState.Progress(
                    current = status.cycle.current,
                    target = if (status.inProgress) status.cycle.target else program?.cycles ?: 0,
                ),
            )
        }
    }

    /** Dropping both subscriptions first: a later emission reaching [close] again would pop the dashboard too. */
    private fun close() {
        stateJob?.cancel()
        cleaningJob?.cancel()
        navigate(Back)
    }
    private companion object {
        const val START_STATUS_TIMEOUT_MILLIS = 5000L
    }
}
