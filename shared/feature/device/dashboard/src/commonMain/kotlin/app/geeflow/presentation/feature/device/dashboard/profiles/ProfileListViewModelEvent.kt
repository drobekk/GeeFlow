package app.geeflow.presentation.feature.device.dashboard.profiles

import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.PhaseTransition
import app.geeflow.data.brew.model.BrewScaleSample
import app.geeflow.presentation.feature.device.dashboard.model.ChartData
import kotlin.time.Instant

sealed interface ProfileListViewModelEvent {
    data class SelectProfile(val id: String?, val navigateToBrewPage: Boolean = false) : ProfileListViewModelEvent
    data class ShowHistoryBrew(
        val name: String,
        val description: String = "",
        val durationSeconds: Int,
        val data: Map<Float, ChartData>,
        val scaleSamples: List<BrewScaleSample> = emptyList(),
        /** Target curve of the profile this brew ran, empty for manual and freehand brews. */
        val targetData: Map<Float, ChartData>,
        val targetWeightGrams: Float? = null,
        val phaseProgram: BrewProgram? = null,
        val phaseTransitions: List<PhaseTransition> = emptyList(),
        val id: Long,
        val startedAt: Instant,
        val endedAt: Instant?,
        val doseGrams: Float?,
        val grindSetting: String?,
        val extractionStartMillis: Long?,
        val extractionStartEstimated: Boolean?,
        val finalWeightGrams: Float?,
        val recipeRatio: Float? = null,
        val recipeTargetGrams: Float? = null,
        val recordedPhaseIds: Set<String> = emptySet(),
    ) : ProfileListViewModelEvent
    data class ShowSnackbar(val message: String) : ProfileListViewModelEvent
}
