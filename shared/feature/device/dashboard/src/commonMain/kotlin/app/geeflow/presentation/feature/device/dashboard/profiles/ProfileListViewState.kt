package app.geeflow.presentation.feature.device.dashboard.profiles

import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.presentation.feature.device.dashboard.model.ChartData

data class ProfileListViewState(
    val profiles: List<Profile> = emptyList(),
    val smartScaleConnected: Boolean = false,
    val showHistory: Boolean = false,
    val history: List<HistoryBrew> = emptyList(),
    val historyLoading: Boolean = false,
) {
    data class Profile(
        val id: String,
        val number: String,
        val name: String,
        val description: String,
        val brewByWeight: Boolean,
        val experimental: Boolean = false,
        val canBind: Boolean = true,
        val bound: Boolean = false,
        val selected: Boolean = false,
        val program: BrewProgram? = null,
        val finishCondition: Condition? = null,
        val targetData: Map<Float, ChartData> = emptyMap(),
    )

    data class HistoryBrew(
        val id: String,
        val name: String,
        val description: String,
        val selected: Boolean = false,
    )
}
