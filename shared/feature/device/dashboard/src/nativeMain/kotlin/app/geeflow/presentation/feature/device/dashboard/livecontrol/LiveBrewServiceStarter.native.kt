package app.geeflow.presentation.feature.device.dashboard.livecontrol

import androidx.compose.runtime.Composable
import app.geeflow.domain.device.LiveControlSettings
import app.geeflow.domain.device.LiveModeProbePath
import app.geeflow.domain.device.LiveModeProbeSequence
import app.geeflow.domain.device.LiveTestRunState
import app.geeflow.domain.device.recipe.*

private val localRecipes = LocalLiveRecipeStore({ null }, {})

@Composable
actual fun rememberLiveBrewServiceStarter(): LiveBrewServiceStarter = object : LiveBrewServiceStarter {
    override val recipes = localRecipes
    override fun startRecipe(deviceId: Long, scaleId: String, recipe: LiveRecipeDocument): String? = "Live machine control is available only on Android."
    override fun start(deviceId: Long, scaleId: String, settings: LiveControlSettings) =
        "Live machine control is available only on Android."
    override fun startCommandEncodingProbe(deviceId: Long, path: LiveModeProbePath, sequence: LiveModeProbeSequence) = "Live machine control is available only on Android."

    override fun requestNotificationPermission() = Unit
    override fun loadSettings(): LiveControlSettings? = null
    override fun saveSettings(settings: LiveControlSettings) = Unit
    override fun exportCapture(state: LiveTestRunState): String? = null
}
