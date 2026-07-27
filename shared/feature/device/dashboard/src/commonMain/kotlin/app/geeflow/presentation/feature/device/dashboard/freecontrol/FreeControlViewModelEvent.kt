package app.geeflow.presentation.feature.device.dashboard.freecontrol

sealed interface FreeControlViewModelEvent {
    data class ShowSnackbar(val message: String) : FreeControlViewModelEvent
}
