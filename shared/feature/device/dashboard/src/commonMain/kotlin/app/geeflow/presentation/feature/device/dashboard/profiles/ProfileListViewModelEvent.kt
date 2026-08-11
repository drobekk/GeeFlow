package app.geeflow.presentation.feature.device.dashboard.profiles

import app.geeflow.presentation.feature.device.dashboard.model.ChartData

sealed interface ProfileListViewModelEvent {
    data class SelectProfile(val id: String?) : ProfileListViewModelEvent
    data class ShowHistoryBrew(
        val name: String,
        val durationSeconds: Int,
        val data: Map<Float, ChartData>,
        /** Target curve of the profile this brew ran, empty for manual and freehand brews. */
        val targetData: Map<Float, ChartData>,
    ) : ProfileListViewModelEvent
    data class ShowSnackbar(val message: String) : ProfileListViewModelEvent
}
