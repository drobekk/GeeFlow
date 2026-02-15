package dev.drobek.geeflow.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview

@Preview(
    name = "Compact",
    fontScale = 1f
)
@Preview(
    name = "Expanded",
    widthDp = 840,
    heightDp = 480,
    fontScale = 1f
)
annotation class GeeFlowPreview

val isPreview: Boolean
    @Composable
    get() {
        return LocalInspectionMode.current
    }
