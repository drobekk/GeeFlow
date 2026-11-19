package app.geeflow.presentation.feature.device.dashboard.livecontrol

import androidx.compose.runtime.Composable

interface LiveRecipeFiles {
    fun openImport()
    fun export(name: String, text: String)
}
@Composable
expect fun rememberLiveRecipeFiles(onImported: (String) -> Unit, onMessage: (String) -> Unit): LiveRecipeFiles
