package app.geeflow.presentation.feature.device.dashboard.livecontrol

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.geeflow.domain.device.LiveControlSettings
import app.geeflow.domain.device.LiveModeProbePath
import app.geeflow.domain.device.LiveModeProbeSequence
import app.geeflow.domain.device.LiveTestRunState
import app.geeflow.domain.device.recipe.*

private const val PREFERENCES = "live_brew_test_configuration"
private const val KEY_BASE = "base_pressure"
private const val KEY_CEILING = "pressure_ceiling"
private const val KEY_SLEW = "pressure_slew"
private const val KEY_KP = "flow_kp"
private const val KEY_KI = "flow_ki"
private const val KEY_COMMAND_INTERVAL = "command_interval_millis"
private const val KEY_DEADLINE = "shot_deadline_millis"
private object AndroidLiveRecipes {
    var store: LocalLiveRecipeStore? = null
    fun get(context: Context): LocalLiveRecipeStore = store ?: context.applicationContext.getSharedPreferences(
        "local_live_recipe_library_v01", Context.MODE_PRIVATE,
    ).let { prefs -> LocalLiveRecipeStore({ prefs.getString("documents", null) }, { value ->
        check(prefs.edit().putString("documents", value).commit()) { "Could not persist local recipes" }
    }).also { store = it } }
}

@Composable
actual fun rememberLiveBrewServiceStarter(): LiveBrewServiceStarter {
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    return remember(context, notificationPermission) {
        object : LiveBrewServiceStarter {
            override val recipes = AndroidLiveRecipes.get(context)
            override fun startRecipe(deviceId: Long, scaleId: String, recipe: LiveRecipeDocument): String? {
                LiveRecipeCompiler.compile(recipe)
                val directory = java.io.File(context.filesDir, "live-recipe-executions").apply { check(isDirectory || mkdirs()) }
                val file = java.io.File.createTempFile("live-", ".json", directory)
                try {
                    file.writeText(LiveRecipeJson.encode(recipe))
                    val result = launch(controlIntent(deviceId, scaleId, recipe.controlSettings())
                        .putExtra(LiveBrewControlService.EXTRA_RECIPE_FILE, file.name))
                    if (result != null) file.delete()
                    return result
                } catch (error: Exception) { file.delete(); throw error }
            }
            override fun start(deviceId: Long, scaleId: String, settings: LiveControlSettings): String? {
                return launch(controlIntent(deviceId, scaleId, settings))
            }

            override fun startCommandEncodingProbe(deviceId: Long, path: LiveModeProbePath, sequence: LiveModeProbeSequence): String? = launch(
                Intent(context, LiveBrewControlService::class.java)
                    .putExtra(LiveBrewControlService.EXTRA_DEVICE_ID, deviceId)
                    .putExtra(LiveBrewControlService.EXTRA_COMMAND_PROBE, true)
                    .putExtra(LiveBrewControlService.EXTRA_COMMAND_PROBE_PATH, path.id)
                    .putExtra(LiveBrewControlService.EXTRA_COMMAND_PROBE_SEQUENCE, sequence.id)
                    .putExtra(LiveBrewControlService.EXTRA_DEADLINE, 100_000L),
            )

            private fun launch(intent: Intent): String? {
                if (Build.VERSION.SDK_INT >= 33 &&
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) return "Allow notifications to show the persistent Stop control."
                if (Build.VERSION.SDK_INT >= 31 &&
                    context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                ) return "Allow nearby-device access before starting the test."

                if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
                return null
            }

            private fun controlIntent(deviceId: Long, scaleId: String, settings: LiveControlSettings) = Intent(context, LiveBrewControlService::class.java)
                    .putExtra(LiveBrewControlService.EXTRA_DEVICE_ID, deviceId)
                    .putExtra(LiveBrewControlService.EXTRA_SCALE_ID, scaleId)
                    .putExtra(LiveBrewControlService.EXTRA_BASE_PRESSURE, settings.basePressureBar)
                    .putExtra(LiveBrewControlService.EXTRA_PRESSURE_CEILING, settings.maximumPressureBar)
                    .putExtra(LiveBrewControlService.EXTRA_SLEW, settings.slewBarPerSecond)
                    .putExtra(LiveBrewControlService.EXTRA_KP, settings.proportionalGainBarPerGramPerSecond)
                    .putExtra(LiveBrewControlService.EXTRA_KI, settings.integralGainBarPerGram)
                    .putExtra(LiveBrewControlService.EXTRA_COMMAND_INTERVAL, settings.commandIntervalMillis)
                    .putExtra(LiveBrewControlService.EXTRA_DEADLINE, settings.shotDeadlineMillis)

            override fun requestNotificationPermission() {
                if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            override fun loadSettings(): LiveControlSettings? = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).let { prefs ->
                if (!prefs.contains(KEY_BASE)) return@let null
                LiveControlSettings(
                    basePressureBar = prefs.getFloat(KEY_BASE, 3f),
                    maximumPressureBar = prefs.getFloat(KEY_CEILING, 9f),
                    slewBarPerSecond = prefs.getFloat(KEY_SLEW, 1f),
                    proportionalGainBarPerGramPerSecond = prefs.getFloat(KEY_KP, .5f),
                    integralGainBarPerGram = prefs.getFloat(KEY_KI, .1f),
                    commandIntervalMillis = prefs.getLong(
                        KEY_COMMAND_INTERVAL,
                        LiveControlSettings.DEFAULT_COMMAND_INTERVAL_MILLIS,
                    ),
                    shotDeadlineMillis = prefs.getLong(KEY_DEADLINE, 40_000L),
                )
            }

            override fun saveSettings(settings: LiveControlSettings) {
                context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
                    .putFloat(KEY_BASE, settings.basePressureBar)
                    .putFloat(KEY_CEILING, settings.maximumPressureBar)
                    .putFloat(KEY_SLEW, settings.slewBarPerSecond)
                    .putFloat(KEY_KP, settings.proportionalGainBarPerGramPerSecond)
                    .putFloat(KEY_KI, settings.integralGainBarPerGram)
                    .putLong(KEY_COMMAND_INTERVAL, settings.commandIntervalMillis)
                    .putLong(KEY_DEADLINE, settings.shotDeadlineMillis)
                    .apply()
            }

            override fun exportCapture(state: LiveTestRunState): String = exportLiveTestCapture(context, state)
        }
    }

}
