package dev.drobek.geeflow.presentation.feature.device.settings.clean

import dev.drobek.geeflow.data.device.api.DeviceController
import dev.drobek.geeflow.domain.device.usecase.GetCleaningStatusUseCase
import dev.drobek.geeflow.presentation.feature.device.settings.clean.CleanEvent.CloseClicked
import dev.drobek.geeflow.presentation.feature.device.settings.clean.CleanEvent.MoreSettingsClicked
import dev.drobek.geeflow.presentation.feature.device.settings.clean.CleanEvent.ToggleCleaningClicked
import dev.drobek.geeflow.viewmodel.BaseViewModel
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class CleanViewModel(
    private val deviceController: DeviceController,
    private val getCleaningStatus: GetCleaningStatusUseCase
) : BaseViewModel<CleanViewState, CleanViewModelEvent>(CleanViewState()) {

    init {
        launch {
            getCleaningStatus().collect { status ->
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
                deviceController.stopCleaning()
            } else {
                deviceController.startCleaning()
            }
        }

        CloseClicked -> emitEvent(Navigation.Back)
        MoreSettingsClicked -> Unit
    }
}
