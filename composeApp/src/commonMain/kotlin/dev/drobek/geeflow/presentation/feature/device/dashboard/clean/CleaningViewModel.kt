package dev.drobek.geeflow.presentation.feature.device.dashboard.clean

import dev.drobek.geeflow.domain.device.usecase.GetCleaningStatusUseCase
import dev.drobek.geeflow.domain.device.usecase.StartCleaningUseCase
import dev.drobek.geeflow.domain.device.usecase.StopCleaningUseCase
import dev.drobek.geeflow.presentation.feature.device.dashboard.clean.CleaningEvent.CloseClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.clean.CleaningEvent.MoreSettingsClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.clean.CleaningEvent.ToggleCleaningClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.navigation.DeviceDashboardDestinations.Clean
import dev.drobek.geeflow.viewmodel.BaseViewModel
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class CleaningViewModel(
    @InjectedParam private val arguments: Clean,
    private val getCleaningStatus: GetCleaningStatusUseCase,
    private val startCleaning: StartCleaningUseCase,
    private val stopCleaning: StopCleaningUseCase
) : BaseViewModel<CleaningViewState, CleanViewModelEvent>(CleaningViewState()) {

    init {
        launch {
            getCleaningStatus(arguments.deviceId).collect { status ->
                modify {
                    copy(
                        isCleaning = status.inProgress,
                        flushProgress = CleaningViewState.Progress(status.flush.current, status.flush.target),
                        restProgress = CleaningViewState.Progress(status.rest.current, status.rest.target),
                        cycleProgress = CleaningViewState.Progress(status.cycle.current, status.cycle.target)
                    )
                }
            }
        }
    }

    fun handleEvent(event: CleaningEvent) = when (event) {
        ToggleCleaningClicked -> launch {
            if (viewState.value.isCleaning) {
                stopCleaning(arguments.deviceId)
            } else {
                startCleaning(arguments.deviceId)
            }
        }

        CloseClicked -> emitEvent(Navigation.Back)
        MoreSettingsClicked -> emitEvent(Navigation.DeviceSettings(arguments.deviceId))
    }
}
