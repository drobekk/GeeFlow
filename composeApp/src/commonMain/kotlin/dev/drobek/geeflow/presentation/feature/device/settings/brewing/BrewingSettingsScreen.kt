package dev.drobek.geeflow.presentation.feature.device.settings.brewing

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.drobek.geeflow.ui.theme.GeeFlowScreenPreview
import dev.drobek.geeflow.ui.theme.GeeFlowTheme

@Composable
internal fun BrewingSettingsScreen() {
    Content()
}

@Composable
private fun Content() {
    Text("Brewing settings details screen")
}

@Composable
@GeeFlowScreenPreview
private fun PreviewLight() = GeeFlowTheme(false) {
    Content()
}

@Composable
@GeeFlowScreenPreview
private fun PreviewDark() = GeeFlowTheme(true) {
    Content()
}
