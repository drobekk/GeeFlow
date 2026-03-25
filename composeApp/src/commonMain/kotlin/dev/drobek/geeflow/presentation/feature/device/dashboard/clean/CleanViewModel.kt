package dev.drobek.geeflow.presentation.feature.device.dashboard.clean

import dev.drobek.geeflow.domain.device.usecase.GetCleaningStatusUseCase
import dev.drobek.geeflow.domain.device.usecase.StartCleaningUseCase
import dev.drobek.geeflow.domain.device.usecase.StopCleaningUseCase
import dev.drobek.geeflow.presentation.feature.device.dashboard.clean.CleanEvent.CloseClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.clean.CleanEvent.MoreSettingsClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.clean.CleanEvent.ToggleCleaningClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.navigation.DeviceDashboardDestinations.Clean
import dev.drobek.geeflow.viewmodel.BaseViewModel
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class CleanViewModel(
    @InjectedParam private val arguments: Clean,
    private val getCleaningStatus: GetCleaningStatusUseCase,
    private val startCleaning: StartCleaningUseCase,
    private val stopCleaning: StopCleaningUseCase
) : BaseViewModel<CleanViewState, CleanViewModelEvent>(CleanViewState()) {

    init {
        launch {
            getCleaningStatus(arguments.deviceId).collect { status ->
                modify {
                    copy(
                        isCleaning = status.inProgress,
                        flushProgress = CleanViewState.Progress(status.flush.current, status.flush.target),
                        restProgress = CleanViewState.Progress(status.rest.current, status.rest.target),
                        cycleProgress = CleanViewState.Progress(status.cycle.current, status.cycle.target)
                    )
                }
            }
        }
    }

    fun handleEvent(event: CleanEvent) = when (event) {
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
