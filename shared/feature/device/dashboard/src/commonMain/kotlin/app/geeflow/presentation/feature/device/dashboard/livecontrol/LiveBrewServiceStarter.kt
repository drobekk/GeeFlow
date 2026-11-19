package app.geeflow.presentation.feature.device.dashboard.livecontrol

import androidx.compose.runtime.Composable
import app.geeflow.domain.device.LiveControlSettings
import app.geeflow.domain.device.LiveModeProbePath
import app.geeflow.domain.device.LiveModeProbeSequence
import app.geeflow.domain.device.LiveTestRunState
import app.geeflow.domain.device.recipe.LiveRecipeDocument
import app.geeflow.domain.device.recipe.LocalLiveRecipeStore

interface LiveBrewServiceStarter {
    val recipes: LocalLiveRecipeStore
    fun start(deviceId: Long, scaleId: String, settings: LiveControlSettings): String?
    fun startRecipe(deviceId: Long, scaleId: String, recipe: LiveRecipeDocument): String?
    fun startCommandEncodingProbe(
        deviceId: Long,
        path: LiveModeProbePath = LiveModeProbePath.EncodingMatrix,
        sequence: LiveModeProbeSequence = LiveModeProbeSequence.FlowPressureFlowPressure,
    ): String?
    fun requestNotificationPermission()
    fun loadSettings(): LiveControlSettings?
    fun saveSettings(settings: LiveControlSettings)
    fun exportCapture(state: LiveTestRunState): String?
}

@Composable
expect fun rememberLiveBrewServiceStarter(): LiveBrewServiceStarter
