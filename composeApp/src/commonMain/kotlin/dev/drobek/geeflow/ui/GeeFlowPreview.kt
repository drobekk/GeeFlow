package dev.drobek.geeflow.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_NO
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview

@Preview(
    name = "Light",
    group = "Light",
    uiMode = UI_MODE_NIGHT_NO,
    fontScale = 1f,
)
@Preview(
    name = "Dark",
    group = "Dark",
    uiMode = UI_MODE_NIGHT_YES
)
annotation class GeeFlowPreview

val isPreview: Boolean
    @Composable
    get() {
        return LocalInspectionMode.current
    }
