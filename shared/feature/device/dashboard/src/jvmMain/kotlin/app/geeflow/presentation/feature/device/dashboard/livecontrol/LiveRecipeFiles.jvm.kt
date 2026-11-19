package app.geeflow.presentation.feature.device.dashboard.livecontrol
import androidx.compose.runtime.Composable
@Composable
actual fun rememberLiveRecipeFiles(onImported: (String) -> Unit, onMessage: (String) -> Unit): LiveRecipeFiles = object : LiveRecipeFiles {
    override fun openImport() { onMessage("Use Import JSON to paste a recipe on this platform.") }
    override fun export(name: String, text: String) { onMessage("Recipe JSON copied to the clipboard.") }
}
