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
    heightDp = 460,
    fontScale = 1f
)
annotation class GeeFlowScreenPreview

val isPreview: Boolean
    @Composable
    get() {
        return LocalInspectionMode.current
    }
